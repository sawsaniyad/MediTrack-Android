package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.helpers.PrefsManager;

public class SettingsActivity extends BaseActivity {

    private SeekBar seekBarReminderMinutes;
    private TextView tvReminderMinutesValue;
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
        switchVibrate = findViewById(R.id.switchVibrate);
        switchSound = findViewById(R.id.switchSound);

        int minutes = PrefsManager.getReminderMinutes(this);
        seekBarReminderMinutes.setProgress(minutes);
        updateMinutesLabel(minutes);
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

    private void updateMinutesLabel(int minutes) {
        tvReminderMinutesValue.setText(getString(R.string.settings_minutes_format, minutes));
    }
}
