package com.samiraa_raghadm_sawsana.meditrack.receivers;

import android.Manifest;
import android.content.ContentValues;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsManager;

import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;
import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDAO;
import com.samiraa_raghadm_sawsana.meditrack.helpers.NotificationHelper;
import com.samiraa_raghadm_sawsana.meditrack.helpers.PermissionManager;

import java.util.List;

public class MissedDoseReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int medicationId = intent.getIntExtra("MEDICATION_ID", -1);
        String scheduledDatetime = intent.getStringExtra(AlarmReceiver.EXTRA_SCHEDULED_DATETIME);
        if (medicationId == -1) {
            return;
        }

        AppExecutors.getInstance().diskIO(() -> {
            MedicationDAO dao = new MedicationDAO(DatabaseHelper.getInstance(context));
            List<IntakeLog> logs = dao.getLogsByMedication(medicationId);
            IntakeLog pendingLog = findPendingLog(logs, scheduledDatetime);

            if (pendingLog == null) {
                return;
            }

            Medication med = dao.getMedicationById(medicationId);
            if (med == null) {
                return;
            }

            markAsMissed(context, pendingLog.getId());

            String phone = med.getEmergencyContactPhone();
            String name = med.getEmergencyContactName();

            if (PermissionManager.isGranted(context, Manifest.permission.SEND_SMS)
                    && phone != null && !phone.isEmpty()) {
                try {
                    SmsManager sms;
                    if (Build.VERSION.SDK_INT >= 31) {
                        sms = context.getSystemService(SmsManager.class);
                    } else {
                        sms = SmsManager.getDefault();
                    }
                    if (sms != null) {
                        String contactName = name != null ? name : "";
                        sms.sendTextMessage(phone, null,
                                context.getString(R.string.sms_missed_dose,
                                        contactName, med.getName()),
                                null, null);
                    }
                } catch (Exception e) {
                    android.util.Log.w("MissedDoseReceiver", "SMS failed", e);
                    NotificationHelper.showMissedDoseAlert(context, med.getName());
                }
            } else {
                NotificationHelper.showMissedDoseAlert(context, med.getName());
            }
        });
    }

    private IntakeLog findPendingLog(List<IntakeLog> logs, String scheduledDatetime) {
        IntakeLog newestPendingLog = null;
        for (IntakeLog log : logs) {
            if (log.isTaken()) {
                continue;
            }
            if (scheduledDatetime != null && scheduledDatetime.equals(log.getScheduledDatetime())) {
                return log;
            }
            if (newestPendingLog == null) {
                newestPendingLog = log;
            }
        }
        return newestPendingLog;
    }

    private void markAsMissed(Context context, int logId) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_LOG_STATUS, IntakeLog.STATUS_MISSED);
        DatabaseHelper.getInstance(context)
                .getWritableDatabase()
                .update(DatabaseHelper.TABLE_INTAKE_LOG,
                        values,
                        DatabaseHelper.COL_LOG_ID + " = ?",
                        new String[] { String.valueOf(logId) });
    }
}

