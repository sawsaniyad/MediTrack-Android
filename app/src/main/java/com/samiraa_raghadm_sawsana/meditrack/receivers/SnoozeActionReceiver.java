package com.samiraa_raghadm_sawsana.meditrack.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;

import com.samiraa_raghadm_sawsana.meditrack.models.PrefsManager;

public class SnoozeActionReceiver extends BroadcastReceiver {

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }
}
