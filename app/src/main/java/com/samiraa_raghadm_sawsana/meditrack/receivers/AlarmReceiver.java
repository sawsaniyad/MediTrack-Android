package com.samiraa_raghadm_sawsana.meditrack.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;
import com.samiraa_raghadm_sawsana.meditrack.helpers.PrefsManager;
import com.samiraa_raghadm_sawsana.meditrack.models.Schedule;
import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDAO;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AlarmScheduler;
import com.samiraa_raghadm_sawsana.meditrack.helpers.NotificationHelper;

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

        if (medicationName == null) {
            medicationName = context.getString(R.string.default_medication_name);
        }
        if (dosage == null) {
            dosage = "";
        }

        if (medicationId == -1 || scheduleId == -1) {
            return;
        }

        final String finalMedName = medicationName;
        final String finalDosage = dosage;

        // FIXED: moved to diskIO
        AppExecutors.getInstance().diskIO(() -> {
            MedicationDAO dao = new MedicationDAO(DatabaseHelper.getInstance(context));
            IntakeLog log = new IntakeLog();
            log.setMedicationId(medicationId);
            log.setScheduledDatetime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()).format(new Date()));
            log.setTaken(false);
            dao.insertIntakeLog(log);
        });

        NotificationHelper.showMedicationReminder(context,
                medicationId, finalMedName, finalDosage, scheduleId);

        Intent localIntent = new Intent(context.getString(R.string.broadcast_medication_due));
        localIntent.putExtra(context.getString(R.string.extra_medication_id), medicationId);
        localIntent.putExtra(context.getString(R.string.extra_medication_name), finalMedName);
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

        scheduleMissedDoseCheck(context, medicationId, scheduleId);

        // FIXED: moved to diskIO — reschedule for next day
        AppExecutors.getInstance().diskIO(() -> {
            MedicationDAO dao = new MedicationDAO(DatabaseHelper.getInstance(context));
            Schedule schedule = null;
            for (Schedule s : dao.getAllSchedules()) {
                if (s.getId() == scheduleId) {
                    schedule = s;
                    break;
                }
            }
            if (schedule != null) {
                Medication med = dao.getMedicationById(medicationId);
                if (med != null && med.isActive()) {
                    AlarmScheduler.scheduleAlarm(context.getApplicationContext(), schedule, med);
                }
            }
        });
    }

    private static boolean canScheduleExact(android.app.AlarmManager am) {
        if (Build.VERSION.SDK_INT < 31) return true;
        try {
            return (Boolean) android.app.AlarmManager.class.getMethod("canScheduleExactAlarms").invoke(am);
        } catch (Exception e) {
            return true;
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

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 31) {
            if (canScheduleExact(am)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, checkAt, checkPI);
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, checkAt, checkPI);
        }
    }
}
