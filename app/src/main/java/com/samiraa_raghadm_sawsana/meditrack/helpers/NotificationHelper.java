package com.samiraa_raghadm_sawsana.meditrack.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.models.PrefsManager;
import com.samiraa_raghadm_sawsana.meditrack.receivers.SnoozeActionReceiver;
import com.samiraa_raghadm_sawsana.meditrack.receivers.TakenActionReceiver;
import com.samiraa_raghadm_sawsana.meditrack.activities.MedicationListActivity;

public final class NotificationHelper {

    public static final String CHANNEL_ID = "medication_reminders";
    public static final int NOTIFICATION_ID_BASE = 1000;
    public static final int MISSED_DOSE_NOTIFICATION_ID = 9000;

    private NotificationHelper() {
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.notification_channel_name));
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.enableLights(true);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_NOTIFICATION), audioAttributes);
            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showMedicationReminder(Context context,
                                              int medicationId,
                                              String medicationName,
                                              String dosage,
                                              int scheduleId) {
        createNotificationChannel(context);

        if (medicationName == null) {
            medicationName = context.getString(R.string.default_medication_name);
        }
        if (dosage == null) {
            dosage = "";
        }

        Intent takenIntent = new Intent(context, TakenActionReceiver.class);
        takenIntent.putExtra("MEDICATION_ID", medicationId);
        takenIntent.putExtra("MEDICATION_NAME", medicationName);
        takenIntent.putExtra("SCHEDULE_ID", scheduleId);
        takenIntent.putExtra("NOTIFICATION_ID", NOTIFICATION_ID_BASE + scheduleId);
        PendingIntent takenPI = PendingIntent.getBroadcast(context,
                scheduleId,
                takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent snoozeIntent = new Intent(context, SnoozeActionReceiver.class);
        snoozeIntent.putExtra("MEDICATION_ID", medicationId);
        snoozeIntent.putExtra("SCHEDULE_ID", scheduleId);
        snoozeIntent.putExtra("MEDICATION_NAME", medicationName);
        snoozeIntent.putExtra("DOSAGE", dosage);
        snoozeIntent.putExtra("NOTIFICATION_ID", NOTIFICATION_ID_BASE + scheduleId);
        PendingIntent snoozePI = PendingIntent.getBroadcast(context,
                scheduleId + 10000,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent mainIntent = new Intent(context, MedicationListActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent mainPI = PendingIntent.getActivity(context, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        boolean vibrate = PrefsManager.isVibrateEnabled(context);
        boolean sound = PrefsManager.isSoundEnabled(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(R.string.notif_reminder_title, medicationName))
                .setContentText(context.getString(R.string.notif_reminder_text, medicationName, dosage))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(mainPI)
                .setAutoCancel(false)
                .addAction(android.R.drawable.checkbox_on_background,
                        context.getString(R.string.notif_taken_action), takenPI)
                .addAction(android.R.drawable.ic_menu_recent_history,
                        context.getString(R.string.notif_snooze_action), snoozePI);

        if (!vibrate) {
            builder.setVibrate(new long[]{0});
        }
        if (!sound) {
            builder.setSound(null);
        }

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            nm.notify(NOTIFICATION_ID_BASE + scheduleId, builder.build());
        }
    }

    public static void showMissedDoseAlert(Context context, String medicationName) {
        createNotificationChannel(context);

        if (medicationName == null) {
            medicationName = context.getString(R.string.default_medication_name);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(context.getString(R.string.status_missed))
                .setContentText(context.getString(R.string.msg_missed_dose_notification, medicationName))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            nm.notify(MISSED_DOSE_NOTIFICATION_ID + medicationName.hashCode(), builder.build());
        }
    }
}
