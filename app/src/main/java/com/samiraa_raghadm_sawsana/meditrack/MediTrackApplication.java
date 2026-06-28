package com.samiraa_raghadm_sawsana.meditrack;

import android.app.Application;

import com.samiraa_raghadm_sawsana.meditrack.helpers.NotificationHelper;

/**
 * מחלקת אפליקציה גלובלית - רצה פעם אחת בעליית האפליקציה
 * Application class: runs once at app startup, before any Activity.
 * Used to create the Notification Channel required on Android 8.0+ (API 26+).
 *
 * רשומה ב-AndroidManifest תחת android:name
 * כותבת: סמירה אבו אל-הווא
 */
public class MediTrackApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // ערוץ ההתראות נוצר במקום אחד בלבד — NotificationHelper —
        // כדי שכל ההתראות ישתמשו באותו ערוץ בדיוק.
        // Single source of truth for the notification channel: NotificationHelper.
        // Every notification posts on NotificationHelper.CHANNEL_ID, so the channel
        // MUST be created there (not duplicated here under a different id).
        NotificationHelper.createNotificationChannel(this);
    }
}