package com.samiraa_raghadm_sawsana.meditrack.receivers;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;

import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.data.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.data.IntakeLog;
import com.samiraa_raghadm_sawsana.meditrack.data.Medication;
import com.samiraa_raghadm_sawsana.meditrack.db.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.db.MedicationDao;
import com.samiraa_raghadm_sawsana.meditrack.services.NotificationHelper;
import com.samiraa_raghadm_sawsana.meditrack.ui.PermissionManager;

import java.util.List;

public class MissedDoseReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int medicationId = intent.getIntExtra("MEDICATION_ID", -1);
        if (medicationId == -1) {
            return;
        }

        // FIXED: moved to diskIO
        AppExecutors.getInstance().diskIO(() -> {
            MedicationDao dao = new MedicationDao(DatabaseHelper.getInstance(context));
            List<IntakeLog> logs = dao.getLogsByMedication(medicationId);
            boolean stillUntaken = false;
            for (int i = logs.size() - 1; i >= 0; i--) {
                IntakeLog log = logs.get(i);
                if (!log.isTaken()) {
                    stillUntaken = true;
                    break;
                } else {
                    break;
                }
            }

            if (!stillUntaken) {
                return;
            }

            Medication med = dao.getMedicationById(medicationId);
            if (med == null) {
                return;
            }

            String phone = med.getEmergencyContactPhone();
            String name = med.getEmergencyContactName();

            if (PermissionManager.isGranted(context, Manifest.permission.SEND_SMS)
                    && phone != null && !phone.isEmpty()) {
                try {
                    SmsManager sms = SmsManager.getDefault();
                    String contactName = name != null ? name : "";
                    sms.sendTextMessage(phone, null,
                            context.getString(R.string.sms_missed_dose,
                                    contactName, med.getName()),
                            null, null);
                } catch (Exception e) {
                    android.util.Log.w("MissedDoseReceiver", "SMS failed", e);
                    NotificationHelper.showMissedDoseAlert(context, med.getName());
                }
            } else {
                NotificationHelper.showMissedDoseAlert(context, med.getName());
            }
        });
    }
}
