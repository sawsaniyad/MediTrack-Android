package com.samiraa_raghadm_sawsana.meditrack.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDAO;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AlarmScheduler;
import com.samiraa_raghadm_sawsana.meditrack.helpers.NotificationHelper;
import com.samiraa_raghadm_sawsana.meditrack.models.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;
import com.samiraa_raghadm_sawsana.meditrack.models.PrefsManager;
import com.samiraa_raghadm_sawsana.meditrack.models.Schedule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int medicationId = intent.getIntExtra("MEDICATION_ID", -1);
        String medicationName = intent.getStringExtra("MEDICATION_NAME");
        String dosage = intent.getStringExtra("DOSAGE");
        int scheduleId = intent.getIntExtra("SCHEDULE_ID", -1);
        boolean fromSnooze = intent.getBooleanExtra(SnoozeActionReceiver.EXTRA_FROM_SNOOZE, false);

        if (medicationName == null) {
            medicationName = context.getString(R.string.default_medication_name);
        }
        if (dosage == null) {
            dosage = "";
        }

        if (medicationId == -1 || scheduleId == -1) {
            return;
        }

        final String finalMedicationName = medicationName;
        final String finalDosage = dosage;

        if (!fromSnooze) {
            AppExecutors.getInstance().diskIO(() -> insertPendingLog(
                    context, medicationId, finalMedicationName));
        }

        NotificationHelper.showMedicationReminder(
                context, medicationId, finalMedicationName, finalDosage, scheduleId);

        Intent localIntent = new Intent(context.getString(R.string.broadcast_medication_due));
        localIntent.putExtra(context.getString(R.string.extra_medication_id), medicationId);
        localIntent.putExtra(context.getString(R.string.extra_medication_name), finalMedicationName);
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

        scheduleMissedDoseCheck(context, medicationId, scheduleId);

        if (!fromSnooze) {
            AppExecutors.getInstance().diskIO(() -> scheduleNextDailyReminder(
                    context, medicationId, scheduleId));
        }
    }

    private void insertPendingLog(Context context, int medicationId, String medicationName) {
        MedicationDAO dao = new MedicationDAO(DatabaseHelper.getInstance(context));
        IntakeLog log = new IntakeLog();
        log.setMedicationId(medicationId);
        log.setMedicationName(medicationName);
        log.setScheduledDatetime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()).format(new Date()));
        log.setTaken(false);
        log.setStatus(IntakeLog.STATUS_PENDING);
        dao.insertIntakeLog(log);
    }

    private void scheduleNextDailyReminder(Context context, int medicationId, int scheduleId) {
        MedicationDAO dao = new MedicationDAO(DatabaseHelper.getInstance(context));
        Schedule schedule = null;
        for (Schedule item : dao.getAllSchedules()) {
            if (item.getId() == scheduleId) {
                schedule = item;
                break;
            }
        }

        if (schedule == null) {
            return;
        }

        Medication medication = dao.getMedicationById(medicationId);
        if (medication != null && medication.isActive()) {
            AlarmScheduler.scheduleAlarm(context.getApplicationContext(), schedule, medication);
        }
    }

    private void scheduleMissedDoseCheck(Context context, int medicationId, int scheduleId) {
        int checkDelay = PrefsManager.getReminderMinutes(context) * 2;
        long checkAt = System.currentTimeMillis() + checkDelay * 60 * 1000L;

        Intent checkIntent = new Intent(context, MissedDoseReceiver.class);
        checkIntent.putExtra("MEDICATION_ID", medicationId);
        checkIntent.putExtra("SCHEDULE_ID", scheduleId);

        PendingIntent checkPI = PendingIntent.getBroadcast(context,
                scheduleId + 30000,
                checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, checkAt, checkPI);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, checkAt, checkPI);
        }
    }
}
