package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.widget.NumberPicker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.helpers.PrefsManager;
import com.samiraa_raghadm_sawsana.meditrack.receivers.AlarmReceiver;
import com.samiraa_raghadm_sawsana.meditrack.receivers.SnoozeActionReceiver;

public class SnoozePickerActivity extends AppCompatActivity {

    private static final int[] PRESET_MINUTES = {5, 10, 15, 30, 60};
    private static final int CUSTOM_MIN = 1;
    private static final int CUSTOM_MAX = 120;

    private int medicationId;
    private String medicationName;
    private String dosage;
    private int scheduleId;
    private String scheduledDatetime;
    private int defaultMinutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent src = getIntent();
        medicationId   = src.getIntExtra("MEDICATION_ID", -1);
        medicationName = src.getStringExtra("MEDICATION_NAME");
        dosage         = src.getStringExtra("DOSAGE");
        scheduleId     = src.getIntExtra("SCHEDULE_ID", -1);
        scheduledDatetime = src.getStringExtra(AlarmReceiver.EXTRA_SCHEDULED_DATETIME);
        defaultMinutes = PrefsManager.getSnoozeDurationMinutes(this);

        if (medicationId == -1) {
            finish();
            return;
        }

        showPickerDialog();
    }

    private void showPickerDialog() {
        // Build labels: presets + custom entry
        String[] labels = new String[PRESET_MINUTES.length + 1];
        for (int i = 0; i < PRESET_MINUTES.length; i++) {
            labels[i] = getString(R.string.snooze_option_minutes, PRESET_MINUTES[i]);
        }
        labels[PRESET_MINUTES.length] = getString(R.string.snooze_option_custom);

        new AlertDialog.Builder(this)
                .setTitle(R.string.snooze_picker_title)
                .setItems(labels, (dialog, which) -> {
                    if (which < PRESET_MINUTES.length) {
                        scheduleReAlarm(PRESET_MINUTES[which]);
                        finish();
                    } else {
                        showCustomPicker();
                    }
                })
                .setNegativeButton(R.string.snooze_picker_cancel, (dialog, which) -> {
                    scheduleReAlarm(defaultMinutes);
                    finish();
                })
                .setOnCancelListener(dialog -> {
                    scheduleReAlarm(defaultMinutes);
                    finish();
                })
                .show();
    }

    private void showCustomPicker() {
        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(CUSTOM_MIN);
        picker.setMaxValue(CUSTOM_MAX);
        picker.setValue(defaultMinutes);
        picker.setWrapSelectorWheel(false);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        picker.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle(R.string.snooze_custom_title)
                .setView(picker)
                .setPositiveButton(R.string.snooze_custom_confirm, (dialog, which) -> {
                    scheduleReAlarm(picker.getValue());
                    finish();
                })
                .setNegativeButton(R.string.snooze_picker_cancel, (dialog, which) -> {
                    // Go back to the preset list
                    showPickerDialog();
                })
                .setOnCancelListener(dialog -> showPickerDialog())
                .show();
    }

    private void scheduleReAlarm(int snoozeMinutes) {
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
