package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDAO;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AlarmScheduler;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.helpers.PermissionManager;
import com.samiraa_raghadm_sawsana.meditrack.helpers.PrefsManager;

public class SettingsActivity extends BaseActivity {

    private SeekBar seekBarReminderMinutes;
    private TextView tvReminderMinutesValue;
    private SeekBar seekBarSnoozeMinutes;
    private TextView tvSnoozeMinutesValue;
    private SwitchMaterial switchVibrate;
    private SwitchMaterial switchSound;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = getSharedPreferences(PrefsManager.PREFS_NAME, MODE_PRIVATE);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        seekBarReminderMinutes = findViewById(R.id.seekBarReminderMinutes);
        tvReminderMinutesValue = findViewById(R.id.tvReminderMinutesValue);
        seekBarSnoozeMinutes = findViewById(R.id.seekBarSnoozeMinutes);
        tvSnoozeMinutesValue = findViewById(R.id.tvSnoozeMinutesValue);
        switchVibrate = findViewById(R.id.switchVibrate);
        switchSound = findViewById(R.id.switchSound);

        findViewById(R.id.rowPermissions).setOnClickListener(v ->
                PermissionManager.openAppSettings(this));
        findViewById(R.id.rowAbout).setOnClickListener(v -> showAboutDialog());
        findViewById(R.id.rowClearData).setOnClickListener(v -> confirmClearAllData());

        int minutes = PrefsManager.getReminderMinutes(this);
        seekBarReminderMinutes.setProgress(minutes);
        updateMinutesLabel(minutes);

        int snoozeMinutes = PrefsManager.getSnoozeDurationMinutes(this);
        seekBarSnoozeMinutes.setProgress(snoozeMinutes);
        updateSnoozeLabel(snoozeMinutes);

        switchVibrate.setChecked(PrefsManager.isVibrateEnabled(this));
        switchSound.setChecked(PrefsManager.isSoundEnabled(this));

        seekBarReminderMinutes.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateMinutesLabel(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int value = seekBar.getProgress();
                PrefsManager.setReminderMinutes(SettingsActivity.this, value);
                preferences.edit().putInt(PrefsManager.KEY_REMIND_MIN, value).apply();
            }
        });

        seekBarSnoozeMinutes.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = Math.max(1, progress);
                updateSnoozeLabel(value);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int value = Math.max(1, seekBar.getProgress());
                PrefsManager.setSnoozeDurationMinutes(SettingsActivity.this, value);
            }
        });

        switchVibrate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PrefsManager.setVibrateEnabled(SettingsActivity.this, isChecked);
            preferences.edit().putBoolean(PrefsManager.KEY_VIBRATE, isChecked).apply();
        });

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PrefsManager.setSoundEnabled(SettingsActivity.this, isChecked);
            preferences.edit().putBoolean(PrefsManager.KEY_SOUND, isChecked).apply();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateSnoozeLabel(int minutes) {
        tvSnoozeMinutesValue.setText(getString(R.string.settings_minutes_format, minutes));
    }

    private void updateMinutesLabel(int minutes) {
        tvReminderMinutesValue.setText(getString(R.string.settings_minutes_format, minutes));
    }

    private void confirmClearAllData() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_data_title)
                .setMessage(R.string.clear_data_message)
                .setPositiveButton(R.string.label_clear_data, (dialog, which) -> clearAllData())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void clearAllData() {
        showLoading();
        AppExecutors.getInstance().diskIO(() -> {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
            MedicationDAO dao = new MedicationDAO(dbHelper);
            AlarmScheduler.cancelAllAlarms(getApplicationContext(), dao);
            dbHelper.resetDatabase();

            AppExecutors.getInstance().mainThread(() -> {
                hideLoading();
                NotificationManagerCompat.from(this).cancelAll();
                showToast(getString(R.string.clear_data_success));
                finish();
            });
        });
    }
}
