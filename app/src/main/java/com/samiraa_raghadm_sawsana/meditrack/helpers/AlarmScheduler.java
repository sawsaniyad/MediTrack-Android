package com.samiraa_raghadm_sawsana.meditrack.helpers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDAO;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;
import com.samiraa_raghadm_sawsana.meditrack.models.Schedule;
import com.samiraa_raghadm_sawsana.meditrack.receivers.AlarmReceiver;
import com.samiraa_raghadm_sawsana.meditrack.receivers.ExpiryReceiver;
import com.samiraa_raghadm_sawsana.meditrack.receivers.MissedDoseReceiver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * PendingIntent requestCode ranges:
 * 1-999: intake alarms (schedule IDs)
 * 10001-10999: snooze action (scheduleId + 10000)
 * 20001-20999: snooze re-alarm (scheduleId + 20000)
 * 30001-30999: missed-dose check (scheduleId + 30000)
 * 50001-50999: expiry alarms (medicationId + 50000)
 */
public final class AlarmScheduler {

    private AlarmScheduler() {
    }

    private static boolean canScheduleExact(AlarmManager am) {
        if (Build.VERSION.SDK_INT < 31) {
            return true;
        }
        try {
            return (Boolean) AlarmManager.class.getMethod("canScheduleExactAlarms").invoke(am);
        } catch (Exception e) {
            return true;
        }
    }

    private static void scheduleAllowWhileIdle(AlarmManager alarmManager,
                                               long triggerAtMillis,
                                               PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= 31) {
            if (canScheduleExact(alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            return;
        }

        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void scheduleAlarm(Context context, Schedule schedule, Medication medication) {
        if (schedule == null || medication == null) {
            return;
        }

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }

        String[] parts = schedule.getIntakeTime().split(":");
        if (parts.length < 2) {
            return;
        }

        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int remindMinutes = PrefsManager.getReminderMinutes(context);
        cal.add(Calendar.MINUTE, -remindMinutes);

        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Canonical scheduled datetime = the exact dose time (seconds = 00) of the
        // day the alarm will fire for. Every component (the medication card window,
        // the mark-taken/missed actions, and MissedDoseReceiver) keys off this exact
        // string, so it MUST match selectedDate.atTime(intakeTime) in the adapter.
        Calendar doseCal = (Calendar) cal.clone();
        doseCal.add(Calendar.MINUTE, remindMinutes); // undo the reminder offset -> dose time
        String scheduledDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(doseCal.getTime());

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("MEDICATION_ID", medication.getId());
        intent.putExtra("MEDICATION_NAME", medication.getName());
        intent.putExtra("DOSAGE", medication.getDosage() != null ? medication.getDosage() : "");
        intent.putExtra("SCHEDULE_ID", schedule.getId());
        intent.putExtra(AlarmReceiver.EXTRA_SCHEDULED_DATETIME, scheduledDatetime);

        PendingIntent pi = PendingIntent.getBroadcast(context,
                schedule.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        scheduleAllowWhileIdle(am, cal.getTimeInMillis(), pi);
    }

    public static void scheduleAllAlarms(Context context, MedicationDAO dao) {
        AppExecutors.getInstance().diskIO(() -> {
            List<Schedule> schedules = dao.getAllSchedules();
            for (Schedule schedule : schedules) {
                Medication med = dao.getMedicationById(schedule.getMedicationId());
                if (med != null && med.isActive()) {
                    scheduleAlarm(context.getApplicationContext(), schedule, med);
                }
            }
        });
    }

    public static void cancelAlarm(Context context, int scheduleId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context,
                scheduleId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }

    public static void cancelAlarmsForMedication(Context context, MedicationDAO dao, int medicationId) {
        AppExecutors.getInstance().diskIO(() -> {
            List<Schedule> schedules = dao.getSchedulesForMedication(medicationId);
            for (Schedule schedule : schedules) {
                cancelAlarm(context.getApplicationContext(), schedule.getId());
                cancelMissedDoseCheck(context.getApplicationContext(), schedule.getId());
                cancelSnoozedReminder(context.getApplicationContext(), schedule.getId());
            }
            cancelExpiryAlarm(context.getApplicationContext(), medicationId);
        });
    }

    public static void cancelAllAlarms(Context context, MedicationDAO dao) {
        List<Schedule> schedules = dao.getAllSchedules();
        for (Schedule schedule : schedules) {
            cancelAlarm(context, schedule.getId());
            cancelMissedDoseCheck(context, schedule.getId());
            cancelSnoozedReminder(context, schedule.getId());
        }

        List<Medication> medications = dao.getAllMedications();
        for (Medication medication : medications) {
            cancelExpiryAlarm(context, medication.getId());
        }
    }

    private static void cancelMissedDoseCheck(Context context, int scheduleId) {
        cancelBroadcast(context, MissedDoseReceiver.class, scheduleId + 30000);
    }

    private static void cancelSnoozedReminder(Context context, int scheduleId) {
        cancelBroadcast(context, AlarmReceiver.class, scheduleId + 20000);
    }

    private static void cancelExpiryAlarm(Context context, int medicationId) {
        cancelBroadcast(context, ExpiryReceiver.class, medicationId + 50000);
    }

    private static void cancelBroadcast(Context context,
                                        Class<?> receiverClass,
                                        int requestCode) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, receiverClass);
        PendingIntent pi = PendingIntent.getBroadcast(context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am != null) {
            am.cancel(pi);
        }
        pi.cancel();
    }

    public static void scheduleExpiryAlarm(Context context, Medication medication) {
        if (medication == null
                || medication.getExpiryDate() == null
                || medication.getExpiryDate().isEmpty()) {
            return;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date expiry = sdf.parse(medication.getExpiryDate());
            if (expiry == null) {
                return;
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(expiry);
            cal.add(Calendar.DAY_OF_YEAR, -7);
            cal.set(Calendar.HOUR_OF_DAY, 9);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                return;
            }

            Intent intent = new Intent(context, ExpiryReceiver.class);
            intent.putExtra("MEDICATION_NAME", medication.getName());
            intent.putExtra("EXPIRY_DATE", medication.getExpiryDate());

            PendingIntent pi = PendingIntent.getBroadcast(context,
                    medication.getId() + 50000,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) {
                return;
            }

            scheduleAllowWhileIdle(am, cal.getTimeInMillis(), pi);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
