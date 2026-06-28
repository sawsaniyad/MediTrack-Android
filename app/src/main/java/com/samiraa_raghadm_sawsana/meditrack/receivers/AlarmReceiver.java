package com.samiraa_raghadm_sawsana.meditrack.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDAO;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AlarmScheduler;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.helpers.NotificationHelper;
import com.samiraa_raghadm_sawsana.meditrack.helpers.PrefsManager;
import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;
import com.samiraa_raghadm_sawsana.meditrack.models.Schedule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String EXTRA_SCHEDULED_DATETIME = "SCHEDULED_DATETIME";
    public static final String EXTRA_IS_EXACT_TIME = "IS_EXACT_TIME";

    @Override
    public void onReceive(Context context, Intent intent) {
        int medicationId = intent.getIntExtra("MEDICATION_ID", -1);
        String medicationName = intent.getStringExtra("MEDICATION_NAME");
        String dosage = intent.getStringExtra("DOSAGE");
        int scheduleId = intent.getIntExtra("SCHEDULE_ID", -1);
        boolean fromSnooze = intent.getBooleanExtra(SnoozeActionReceiver.EXTRA_FROM_SNOOZE, false);
        boolean isExactTime = intent.getBooleanExtra(EXTRA_IS_EXACT_TIME, false);
        String scheduledDatetime = intent.getStringExtra(EXTRA_SCHEDULED_DATETIME);

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
        final String finalScheduledDatetime = scheduledDatetime != null
                ? scheduledDatetime
                : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());

        int remindMinutes = PrefsManager.getReminderMinutes(context);

        // Snooze re-alarms and zero-advance-reminder go straight to "הגיע הזמן".
        boolean showExactTime = isExactTime || fromSnooze || remindMinutes == 0;

        if (!showExactTime) {
            // Advance notification: simple text, no action buttons.
            NotificationHelper.showAdvanceReminder(
                    context, finalMedicationName, scheduleId, remindMinutes);
            scheduleExactTimeAlarm(context, medicationId, finalMedicationName, finalDosage,
                    scheduleId, finalScheduledDatetime);
            return;
        }

        // Exact-time: cancel advance notification, show full "הגיע הזמן" notification.
        NotificationHelper.cancelAdvanceNotification(context, scheduleId);

        final int windowMinutes = PrefsManager.getActionWindowMinutes(context);

        if (!fromSnooze) {
            AppExecutors.getInstance().diskIO(() -> insertPendingLog(
                    context, medicationId, finalMedicationName, finalScheduledDatetime));
        }

        PrefsManager.activateDoseActionWindow(
                context, medicationId, finalScheduledDatetime, windowMinutes * 60 * 1000L);

        NotificationHelper.showMedicationReminder(
                context, medicationId, finalMedicationName, finalDosage, scheduleId,
                finalScheduledDatetime);

        Intent localIntent = new Intent(context.getString(R.string.broadcast_medication_due));
        localIntent.putExtra(context.getString(R.string.extra_medication_id), medicationId);
        localIntent.putExtra(context.getString(R.string.extra_medication_name), finalMedicationName);
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

        scheduleMissedDoseCheck(context, medicationId, scheduleId, finalScheduledDatetime,
                finalMedicationName, finalDosage);

        if (!fromSnooze) {
            AppExecutors.getInstance().diskIO(() -> scheduleNextDailyReminder(
                    context, medicationId, scheduleId));
        }
    }

    private void insertPendingLog(Context context,
                                  int medicationId,
                                  String medicationName,
                                  String scheduledDatetime) {
        MedicationDAO dao = new MedicationDAO(DatabaseHelper.getInstance(context));
        IntakeLog log = new IntakeLog();
        log.setMedicationId(medicationId);
        log.setMedicationName(medicationName);
        log.setScheduledDatetime(scheduledDatetime);
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

    private void scheduleExactTimeAlarm(Context context,
                                        int medicationId,
                                        String medicationName,
                                        String dosage,
                                        int scheduleId,
                                        String scheduledDatetime) {
        long triggerAt;
        try {
            Date scheduled = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .parse(scheduledDatetime);
            triggerAt = scheduled != null ? scheduled.getTime() : System.currentTimeMillis();
        } catch (ParseException e) {
            triggerAt = System.currentTimeMillis();
        }

        Intent exactIntent = new Intent(context, AlarmReceiver.class);
        exactIntent.putExtra("MEDICATION_ID", medicationId);
        exactIntent.putExtra("MEDICATION_NAME", medicationName);
        exactIntent.putExtra("DOSAGE", dosage);
        exactIntent.putExtra("SCHEDULE_ID", scheduleId);
        exactIntent.putExtra(EXTRA_SCHEDULED_DATETIME, scheduledDatetime);
        exactIntent.putExtra(EXTRA_IS_EXACT_TIME, true);

        if (triggerAt <= System.currentTimeMillis()) {
            context.sendBroadcast(exactIntent);
            return;
        }

        PendingIntent pi = PendingIntent.getBroadcast(context,
                scheduleId + 45000,
                exactIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }

        if (canScheduleExact(am)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    private void scheduleMissedDoseCheck(Context context,
                                         int medicationId,
                                         int scheduleId,
                                         String scheduledDatetime,
                                         String medicationName,
                                         String dosage) {
        int snoozeDuration = PrefsManager.getSnoozeDurationMinutes(context);
        long checkAt = System.currentTimeMillis() + snoozeDuration * 60 * 1000L;

        Intent checkIntent = new Intent(context, MissedDoseReceiver.class);
        checkIntent.putExtra("MEDICATION_ID", medicationId);
        checkIntent.putExtra("SCHEDULE_ID", scheduleId);
        checkIntent.putExtra(EXTRA_SCHEDULED_DATETIME, scheduledDatetime);
        checkIntent.putExtra("MEDICATION_NAME", medicationName);
        checkIntent.putExtra("DOSAGE", dosage);
        checkIntent.putExtra(MissedDoseReceiver.EXTRA_REPEAT_COUNT, 0);

        PendingIntent checkPI = PendingIntent.getBroadcast(context,
                scheduleId + 30000,
                checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        if (canScheduleExact(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, checkAt, checkPI);
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, checkAt, checkPI);
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
}
