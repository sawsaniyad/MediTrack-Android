package com.samiraa_raghadm_sawsana.meditrack.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public final class PrefsManager {

    public static final String PREFS_NAME = "MediTrackPrefs";
    public static final String KEY_REMIND_MIN = "remind_minutes";
    public static final String KEY_VIBRATE = "vibrate_enabled";
    public static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_ACTION_WINDOW_PREFIX = "action_window_";

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

    public static void activateDoseActionWindow(Context ctx,
                                                int medicationId,
                                                String scheduledDatetime,
                                                long durationMillis) {
        if (scheduledDatetime == null || scheduledDatetime.trim().isEmpty()) {
            return;
        }
        long expiresAt = System.currentTimeMillis() + Math.max(0L, durationMillis);
        prefs(ctx).edit()
                .putLong(actionWindowKey(medicationId, scheduledDatetime), expiresAt)
                .apply();
    }

    public static boolean isDoseActionWindowActive(Context ctx,
                                                   int medicationId,
                                                   String scheduledDatetime) {
        long expiresAt = getDoseActionWindowExpiry(ctx, medicationId, scheduledDatetime);
        return expiresAt >= 0L && expiresAt > System.currentTimeMillis();
    }

    public static long getDoseActionWindowExpiry(Context ctx,
                                                 int medicationId,
                                                 String scheduledDatetime) {
        if (scheduledDatetime == null || scheduledDatetime.trim().isEmpty()) {
            return -1L;
        }
        String key = actionWindowKey(medicationId, scheduledDatetime);
        return prefs(ctx).getLong(key, -1L);
    }

    public static void clearDoseActionWindow(Context ctx,
                                             int medicationId,
                                             String scheduledDatetime) {
        if (scheduledDatetime == null || scheduledDatetime.trim().isEmpty()) {
            clearDoseActionWindowsForMedication(ctx, medicationId);
            return;
        }
        prefs(ctx).edit()
                .remove(actionWindowKey(medicationId, scheduledDatetime))
                .apply();
    }

    public static void clearDoseActionWindowsForMedication(Context ctx, int medicationId) {
        String prefix = actionWindowMedicationPrefix(medicationId);
        SharedPreferences.Editor editor = prefs(ctx).edit();
        for (String key : prefs(ctx).getAll().keySet()) {
            if (key.startsWith(prefix)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    private static String actionWindowKey(int medicationId, String scheduledDatetime) {
        return actionWindowMedicationPrefix(medicationId)
                + scheduledDatetime.replace(" ", "_").replace(":", "-");
    }

    private static String actionWindowMedicationPrefix(int medicationId) {
        return KEY_ACTION_WINDOW_PREFIX + medicationId + "_";
    }
}
