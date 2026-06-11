package com.samiraa_raghadm_sawsana.meditrack.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public final class PrefsManager {

    public static final String PREFS_NAME = "MediTrackPrefs";
    public static final String KEY_REMIND_MIN = "remind_minutes";
    public static final String KEY_VIBRATE = "vibrate_enabled";
    public static final String KEY_SOUND = "sound_enabled";

    private static final int DEFAULT_REMIND_MIN = 15;

    private PrefsManager() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int getReminderMinutes(Context ctx) {
        return prefs(ctx).getInt(KEY_REMIND_MIN, DEFAULT_REMIND_MIN);
    }

    public static boolean isVibrateEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_VIBRATE, true);
    }

    public static boolean isSoundEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_SOUND, true);
    }

    public static void setReminderMinutes(Context ctx, int minutes) {
        prefs(ctx).edit().putInt(KEY_REMIND_MIN, minutes).apply();
    }

    public static void setVibrateEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_VIBRATE, enabled).apply();
    }

    public static void setSoundEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_SOUND, enabled).apply();
    }
}
