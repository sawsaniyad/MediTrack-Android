package com.samiraa_raghadm_sawsana.meditrack.helpers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.samiraa_raghadm_sawsana.meditrack.models.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;
import com.samiraa_raghadm_sawsana.meditrack.models.PrefsManager;
import com.samiraa_raghadm_sawsana.meditrack.models.Schedule;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDao;
import com.samiraa_raghadm_sawsana.meditrack.receivers.AlarmReceiver;
import com.samiraa_raghadm_sawsana.meditrack.receivers.ExpiryReceiver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * PendingIntent requestCode ranges:
 * 1–999:       intake alarms (schedule IDs)
 * 10001–10999: snooze action (scheduleId + 10000)
 * 20001–20999: snooze re-alarm (scheduleId + 20000)
 * 30001–30999: missed-dose check (scheduleId + 30000)
 * 50001–50999: expiry alarms (medicationId + 50000)
 */
public final class AlarmScheduler {

    private AlarmScheduler() {
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

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("MEDICATION_ID", medication.getId());
        intent.putExtra("MEDICATION_NAME", medication.getName());
        intent.putExtra("DOSAGE", medication.getDosage() != null ? medication.getDosage() : "");
        intent.putExtra("SCHEDULE_ID", schedule.getId());

        PendingIntent pi = PendingIntent.getBroadcast(context,
                schedule.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // setExactAndAllowWhileIdle ensures delivery in Doze
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }
    }

    public static void scheduleAllAlarms(Context context, MedicationDao dao) {
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

    public static void cancelAlarmsForMedication(Context context, MedicationDao dao, int medicationId) {
        AppExecutors.getInstance().diskIO(() -> {
            List<Schedule> schedules = dao.getSchedulesForMedication(medicationId);
            for (Schedule schedule : schedules) {
                cancelAlarm(context.getApplicationContext(), schedule.getId());
            }
        });
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            cal.getTimeInMillis(), pi);
                }
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        cal.getTimeInMillis(), pi);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
