package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.helpers.PrefsManager;
import com.samiraa_raghadm_sawsana.meditrack.receivers.AlarmReceiver;
import com.samiraa_raghadm_sawsana.meditrack.receivers.SnoozeActionReceiver;

public class SnoozePickerActivity extends AppCompatActivity {

    private static final int[] OPTIONS_MINUTES = {5, 10, 15, 30, 60};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent src = getIntent();
        int medicationId = src.getIntExtra("MEDICATION_ID", -1);
        String medicationName = src.getStringExtra("MEDICATION_NAME");
        String dosage = src.getStringExtra("DOSAGE");
        int scheduleId = src.getIntExtra("SCHEDULE_ID", -1);
        String scheduledDatetime = src.getStringExtra(AlarmReceiver.EXTRA_SCHEDULED_DATETIME);

        if (medicationId == -1) {
            finish();
            return;
        }

        String[] labels = new String[OPTIONS_MINUTES.length];
        for (int i = 0; i < OPTIONS_MINUTES.length; i++) {
            labels[i] = getString(R.string.snooze_option_minutes, OPTIONS_MINUTES[i]);
        }

        int defaultMinutes = PrefsManager.getSnoozeDurationMinutes(this);

        new AlertDialog.Builder(this)
                .setTitle(R.string.snooze_picker_title)
                .setItems(labels, (dialog, which) -> {
                    scheduleReAlarm(medicationId, medicationName, dosage,
                            scheduleId, scheduledDatetime, OPTIONS_MINUTES[which]);
                    finish();
                })
                .setNegativeButton(R.string.snooze_picker_cancel, (dialog, which) -> {
                    scheduleReAlarm(medicationId, medicationName, dosage,
                            scheduleId, scheduledDatetime, defaultMinutes);
                    finish();
                })
                .setOnCancelListener(dialog -> {
                    scheduleReAlarm(medicationId, medicationName, dosage,
                            scheduleId, scheduledDatetime, defaultMinutes);
                    finish();
                })
                .show();
    }

    private void scheduleReAlarm(int medicationId, String medicationName, String dosage,
                                  int scheduleId, String scheduledDatetime, int snoozeMinutes) {
        long triggerAt = System.currentTimeMillis() + (long) snoozeMinutes * 60 * 1000L;

        Intent reAlarmIntent = new Intent(this, AlarmReceiver.class);
        reAlarmIntent.putExtra("MEDICATION_ID", medicationId);
        reAlarmIntent.putExtra("MEDICATION_NAME", medicationName);
        reAlarmIntent.putExtra("DOSAGE", dosage != null ? dosage : "");
        reAlarmIntent.putExtra("SCHEDULE_ID", scheduleId);
        reAlarmIntent.putExtra(SnoozeActionReceiver.EXTRA_FROM_SNOOZE, true);
        reAlarmIntent.putExtra(AlarmReceiver.EXTRA_SCHEDULED_DATETIME, scheduledDatetime);

        PendingIntent pi = PendingIntent.getBroadcast(this,
                scheduleId + 20000,
                reAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;

        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }
}
