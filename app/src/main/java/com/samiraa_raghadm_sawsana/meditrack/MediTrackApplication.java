package com.samiraa_raghadm_sawsana.meditrack;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

/**
 * מחלקת אפליקציה גלובלית - רצה פעם אחת בעליית האפליקציה
 * Application class: runs once at app startup, before any Activity.
 * Used to create the Notification Channel required on Android 8.0+ (API 26+).
 *
 * רשומה ב-AndroidManifest תחת android:name
 * כותבת: סמירה אבו אל-הווא
 */
public class MediTrackApplication extends Application {

    // מזהה ערוץ ההתראות - בשימוש ע"י NotificationHelper של רגד
    // Public so Raghad's NotificationHelper references the SAME channel.
    public static final String CHANNEL_ID = "meditrack_reminders";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    /**
     * יוצר את ערוץ ההתראות פעם אחת
     * Creates the notification channel once at startup.
     * No SDK version check needed: minSdk is 26, so NotificationChannel always
     * exists.
     */
    private void createNotificationChannel() {
        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_HIGH; // heads-up + sound for reminders

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        channel.enableVibration(true);
        channel.enableLights(true);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}