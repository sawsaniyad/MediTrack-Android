package com.samiraa_raghadm_sawsana.meditrack.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;

import com.samiraa_raghadm_sawsana.meditrack.helpers.PrefsManager;

public class SnoozeActionReceiver extends BroadcastReceiver {

    private static boolean canScheduleExact(AlarmManager am) {
        if (android.os.Build.VERSION.SDK_INT < 31) return true;
        try {
            return (Boolean) AlarmManager.class.getMethod("canScheduleExactAlarms").invoke(am);
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int medicationId = intent.getIntExtra("MEDICATION_ID", -1);
        int scheduleId = intent.getIntExtra("SCHEDULE_ID", -1);
        String medicationName = intent.getStringExtra("MEDICATION_NAME");
        String dosage = intent.getStringExtra("DOSAGE");
        int notificationId = intent.getIntExtra("NOTIFICATION_ID", -1);

        if (medicationId == -1) {
            return;
        }

        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId);
        }

        int snoozeMinutes = PrefsManager.getReminderMinutes(context);
        long triggerAt = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L;

        Intent snoozeFireIntent = new Intent(context, AlarmReceiver.class);
        snoozeFireIntent.putExtra("MEDICATION_ID", medicationId);
        snoozeFireIntent.putExtra("MEDICATION_NAME", medicationName);
        snoozeFireIntent.putExtra("DOSAGE", dosage);
        snoozeFireIntent.putExtra("SCHEDULE_ID", scheduleId);

        PendingIntent pi = PendingIntent.getBroadcast(context,
                scheduleId + 20000,
                snoozeFireIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 31) {
            if (canScheduleExact(am)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }
}
