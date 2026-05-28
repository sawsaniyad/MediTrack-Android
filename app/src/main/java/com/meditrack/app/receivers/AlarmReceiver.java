package com.meditrack.app.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.meditrack.app.data.AppExecutors;
import com.meditrack.app.data.IntakeLog;
import com.meditrack.app.data.Medication;
import com.meditrack.app.data.PrefsManager;
import com.meditrack.app.data.Schedule;
import com.meditrack.app.db.DatabaseHelper;
import com.meditrack.app.db.MedicationDao;
import com.meditrack.app.services.AlarmScheduler;
import com.meditrack.app.services.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int medicationId = intent.getIntExtra("MEDICATION_ID", -1);
        String medicationName = intent.getStringExtra("MEDICATION_NAME");
        String dosage = intent.getStringExtra("DOSAGE");
        int scheduleId = intent.getIntExtra("SCHEDULE_ID", -1);

        if (medicationId == -1 || scheduleId == -1) {
            return;
        }

        AppExecutors.getInstance().diskIO(() -> {
            MedicationDao dao = new MedicationDao(DatabaseHelper.getInstance(context));
            IntakeLog log = new IntakeLog();
            log.setMedicationId(medicationId);
            log.setScheduledDatetime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()).format(new Date()));
            log.setTaken(false);
            dao.insertIntakeLog(log);
        });

        NotificationHelper.showMedicationReminder(context,
                medicationId,
                medicationName != null ? medicationName : "",
                dosage != null ? dosage : "",
                scheduleId);

        scheduleMissedDoseCheck(context, medicationId, scheduleId);

        AppExecutors.getInstance().diskIO(() -> {
            MedicationDao dao = new MedicationDao(DatabaseHelper.getInstance(context));
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, checkAt, checkPI);
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, checkAt, checkPI);
        }
    }
}
