package com.samiraa_raghadm_sawsana.meditrack.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDAO;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.helpers.PrefsManager;
import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;

import java.util.List;

public class SnoozeActionReceiver extends BroadcastReceiver {

    public static final String EXTRA_FROM_SNOOZE = "FROM_SNOOZE";

    @Override
    public void onReceive(Context context, Intent intent) {
        int medicationId = intent.getIntExtra("MEDICATION_ID", -1);
        int scheduleId = intent.getIntExtra("SCHEDULE_ID", -1);
        String medicationName = intent.getStringExtra("MEDICATION_NAME");
        String dosage = intent.getStringExtra("DOSAGE");
        int notificationId = intent.getIntExtra("NOTIFICATION_ID", -1);
        String scheduledDatetime = intent.getStringExtra(AlarmReceiver.EXTRA_SCHEDULED_DATETIME);

        if (medicationId == -1) {
            return;
        }

        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId);
        }

        cancelMissedDoseCheck(context, medicationId, scheduleId);
        PrefsManager.clearDoseActionWindow(context, medicationId, scheduledDatetime);

        AppExecutors.getInstance().diskIO(() -> {
            MedicationDAO dao = new MedicationDAO(DatabaseHelper.getInstance(context));
            IntakeLog pendingLog = findPendingLog(dao.getLogsByMedication(medicationId), scheduledDatetime);
            if (pendingLog != null) {
                markAsSnoozed(context, pendingLog.getId());
            }
        });

        int snoozeMinutes = PrefsManager.getSnoozeDurationMinutes(context);
        long triggerAt = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L;

        Intent snoozeFireIntent = new Intent(context, AlarmReceiver.class);
        snoozeFireIntent.putExtra("MEDICATION_ID", medicationId);
        snoozeFireIntent.putExtra("MEDICATION_NAME", medicationName);
        snoozeFireIntent.putExtra("DOSAGE", dosage);
        snoozeFireIntent.putExtra("SCHEDULE_ID", scheduleId);
        snoozeFireIntent.putExtra(EXTRA_FROM_SNOOZE, true);
        snoozeFireIntent.putExtra(AlarmReceiver.EXTRA_SCHEDULED_DATETIME, scheduledDatetime);

        PendingIntent pi = PendingIntent.getBroadcast(context,
                scheduleId + 20000,
                snoozeFireIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }

        if (canScheduleExact(am)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
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

    private void cancelMissedDoseCheck(Context context, int medicationId, int scheduleId) {
        if (scheduleId == -1) {
            return;
        }

        Intent checkIntent = new Intent(context, MissedDoseReceiver.class);
        checkIntent.putExtra("MEDICATION_ID", medicationId);
        checkIntent.putExtra("SCHEDULE_ID", scheduleId);

        PendingIntent checkPI = PendingIntent.getBroadcast(context,
                scheduleId + 30000,
                checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(checkPI);
        }
        checkPI.cancel();
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

    private void markAsSnoozed(Context context, int logId) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_LOG_WAS_DELAYED, 1);
        values.put(DatabaseHelper.COL_LOG_STATUS, IntakeLog.STATUS_SNOOZED);
        DatabaseHelper.getInstance(context)
                .getWritableDatabase()
                .update(DatabaseHelper.TABLE_INTAKE_LOG,
                        values,
                        DatabaseHelper.COL_LOG_ID + " = ?",
                        new String[] { String.valueOf(logId) });
    }
}

