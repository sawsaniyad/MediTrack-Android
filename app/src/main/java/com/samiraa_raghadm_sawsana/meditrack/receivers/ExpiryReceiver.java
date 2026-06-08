package com.samiraa_raghadm_sawsana.meditrack.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.helpers.NotificationHelper;

public class ExpiryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationHelper.createNotificationChannel(context);

        String medName = intent.getStringExtra("MEDICATION_NAME");
        String expiryDate = intent.getStringExtra("EXPIRY_DATE");

        if (medName == null) {
            medName = "";
        }
        if (expiryDate == null) {
            expiryDate = "";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(context.getString(R.string.expiry_notif_title))
                .setContentText(context.getString(R.string.expiry_notif_text, medName, expiryDate))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            nm.notify(NotificationHelper.NOTIFICATION_ID_BASE + medName.hashCode(),
                    builder.build());
        }
    }
}
