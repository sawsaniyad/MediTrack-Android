package com.samiraa_raghadm_sawsana.meditrack.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import com.samiraa_raghadm_sawsana.meditrack.helpers.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;
import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDAO;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TakenActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int medicationId = intent.getIntExtra("MEDICATION_ID", -1);
        String medicationName = intent.getStringExtra("MEDICATION_NAME");
        int scheduleId = intent.getIntExtra("SCHEDULE_ID", -1);
        int notificationId = intent.getIntExtra("NOTIFICATION_ID", -1);
        String scheduledDatetime = intent.getStringExtra(AlarmReceiver.EXTRA_SCHEDULED_DATETIME);

        if (medicationId == -1) {
            return;
        }

        String actualTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()).format(new Date());

        AppExecutors.getInstance().diskIO(() -> {
            MedicationDAO dao = new MedicationDAO(DatabaseHelper.getInstance(context));
            List<IntakeLog> logs = dao.getLogsByMedication(medicationId);
            IntakeLog pendingLog = findPendingLog(logs, scheduledDatetime);

            if (pendingLog != null) {
                dao.markAsTaken(pendingLog.getId(), actualTime);
            } else {
                Medication medication = dao.getMedicationById(medicationId);
                IntakeLog takenLog = new IntakeLog();
                takenLog.setMedicationId(medicationId);
                takenLog.setMedicationName(resolveMedicationName(medicationName, medication));
                takenLog.setScheduledDatetime(scheduledDatetime != null ? scheduledDatetime : actualTime);
                takenLog.setTaken(true);
                takenLog.setActualDatetime(actualTime);
                takenLog.setStatus(IntakeLog.STATUS_TAKEN);
                dao.insertIntakeLog(takenLog);
            }

            cancelMissedDoseCheck(context, medicationId, scheduleId);
            cancelSnoozedReminder(context, medicationId, scheduleId);
        });

        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId);
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

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(checkPI);
        }
        checkPI.cancel();
    }

    private void cancelSnoozedReminder(Context context, int medicationId, int scheduleId) {
        if (scheduleId == -1) {
            return;
        }

        Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
        snoozeIntent.putExtra("MEDICATION_ID", medicationId);
        snoozeIntent.putExtra("SCHEDULE_ID", scheduleId);

        PendingIntent snoozePI = PendingIntent.getBroadcast(context,
                scheduleId + 20000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(snoozePI);
        }
        snoozePI.cancel();
    }

    private String resolveMedicationName(String medicationName, Medication medication) {
        if (medicationName != null && !medicationName.trim().isEmpty()) {
            return medicationName;
        }
        if (medication != null && medication.getName() != null
                && !medication.getName().trim().isEmpty()) {
            return medication.getName();
        }
        return null;
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
                // MedicationDAO returns logs ordered by scheduled_datetime DESC.
                newestPendingLog = log;
            }
        }
        return newestPendingLog;
    }
}
