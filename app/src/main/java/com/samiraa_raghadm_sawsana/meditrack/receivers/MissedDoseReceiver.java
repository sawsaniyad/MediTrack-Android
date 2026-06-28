package com.samiraa_raghadm_sawsana.meditrack.receivers;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
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
import com.samiraa_raghadm_sawsana.meditrack.helpers.PrefsManager;

import java.util.List;

public class MissedDoseReceiver extends BroadcastReceiver {

    static final String EXTRA_REPEAT_COUNT = "REPEAT_COUNT";

    @Override
    public void onReceive(Context context, Intent intent) {
        int medicationId = intent.getIntExtra("MEDICATION_ID", -1);
        int scheduleId   = intent.getIntExtra("SCHEDULE_ID", -1);
        String scheduledDatetime = intent.getStringExtra(AlarmReceiver.EXTRA_SCHEDULED_DATETIME);
        String medicationName    = intent.getStringExtra("MEDICATION_NAME");
        String dosage            = intent.getStringExtra("DOSAGE");
        int repeatCount          = intent.getIntExtra(EXTRA_REPEAT_COUNT, 0);

        if (medicationId == -1) return;

        AppExecutors.getInstance().diskIO(() -> {
            MedicationDAO dao = new MedicationDAO(DatabaseHelper.getInstance(context));
            List<IntakeLog> logs = dao.getLogsByMedication(medicationId);
            IntakeLog pendingLog = findPendingLog(logs, scheduledDatetime);

            if (pendingLog == null) {
                // Dose was already taken — stop the repeat cycle.
                return;
            }

            // On the first check: mark the dose as missed and alert the emergency contact.
            if (repeatCount == 0) {
                markAsMissed(context, pendingLog.getId());
                PrefsManager.clearDoseActionWindow(context, medicationId, scheduledDatetime);
                sendMissedDoseAlert(context, dao, medicationId, medicationName);
            }

            // Resolve name from DB when not carried in the intent.
            String name = (medicationName != null && !medicationName.isEmpty())
                    ? medicationName : resolveNameFromDb(dao, medicationId);

            // Extend the action window so the "take" button stays active in the UI.
            int snoozeDuration = PrefsManager.getSnoozeDurationMinutes(context);
            PrefsManager.activateDoseActionWindow(
                    context, medicationId, scheduledDatetime, snoozeDuration * 60 * 1000L);

            // Re-show the full reminder notification (re-alerts with sound/vibration).
            NotificationHelper.showMedicationReminder(
                    context, medicationId, name,
                    dosage != null ? dosage : "", scheduleId, scheduledDatetime);

            // Schedule the next repeat check using the same request code (scheduleId + 30000)
            // so TakenActionReceiver.cancelMissedDoseCheck() can stop the loop.
            scheduleRepeatCheck(context, medicationId, scheduleId, scheduledDatetime,
                    name, dosage, repeatCount + 1, snoozeDuration);
        });
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void sendMissedDoseAlert(Context context, MedicationDAO dao,
                                     int medicationId, String intentName) {
        Medication med = dao.getMedicationById(medicationId);
        if (med == null) return;

        String name = med.getName() != null ? med.getName()
                : (intentName != null ? intentName : "");
        String phone       = med.getEmergencyContactPhone();
        String contactName = med.getEmergencyContactName();

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
                    sms.sendTextMessage(phone, null,
                            context.getString(R.string.sms_missed_dose,
                                    contactName != null ? contactName : "", name),
                            null, null);
                    return;
                }
            } catch (Exception e) {
                android.util.Log.w("MissedDoseReceiver", "SMS failed", e);
            }
        }
        NotificationHelper.showMissedDoseAlert(context, name);
    }

    private void scheduleRepeatCheck(Context context,
                                     int medicationId, int scheduleId,
                                     String scheduledDatetime,
                                     String medicationName, String dosage,
                                     int nextRepeatCount, int snoozeDurationMinutes) {
        long checkAt = System.currentTimeMillis() + snoozeDurationMinutes * 60 * 1000L;

        Intent checkIntent = new Intent(context, MissedDoseReceiver.class);
        checkIntent.putExtra("MEDICATION_ID", medicationId);
        checkIntent.putExtra("SCHEDULE_ID", scheduleId);
        checkIntent.putExtra(AlarmReceiver.EXTRA_SCHEDULED_DATETIME, scheduledDatetime);
        checkIntent.putExtra("MEDICATION_NAME", medicationName);
        checkIntent.putExtra("DOSAGE", dosage);
        checkIntent.putExtra(EXTRA_REPEAT_COUNT, nextRepeatCount);

        PendingIntent checkPI = PendingIntent.getBroadcast(context,
                scheduleId + 30000,
                checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        if (canScheduleExact(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, checkAt, checkPI);
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, checkAt, checkPI);
        }
    }

    private String resolveNameFromDb(MedicationDAO dao, int medicationId) {
        Medication med = dao.getMedicationById(medicationId);
        return (med != null && med.getName() != null) ? med.getName() : "";
    }

    private boolean canScheduleExact(AlarmManager alarmManager) {
        try {
            return (Boolean) AlarmManager.class
                    .getMethod("canScheduleExactAlarms")
                    .invoke(alarmManager);
        } catch (Exception ignored) {
            return true;
        }
    }

    private IntakeLog findPendingLog(List<IntakeLog> logs, String scheduledDatetime) {
        IntakeLog newestNonTaken = null;
        // Iterate newest-first so the fallback is the most recent non-taken dose.
        for (int i = logs.size() - 1; i >= 0; i--) {
            IntakeLog log = logs.get(i);
            if (log.isTaken()) continue;
            if (scheduledDatetime != null && scheduledDatetime.equals(log.getScheduledDatetime())) {
                return log;
            }
            if (newestNonTaken == null) newestNonTaken = log;
        }
        return newestNonTaken;
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
