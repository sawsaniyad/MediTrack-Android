package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDAO;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AlarmScheduler;
import com.samiraa_raghadm_sawsana.meditrack.models.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;
import com.samiraa_raghadm_sawsana.meditrack.models.Schedule;
import com.samiraa_raghadm_sawsana.meditrack.models.PrefsManager;

import java.util.List;

public class SettingsActivity extends BaseActivity {

    private SeekBar seekBarReminderMinutes;
    private TextView tvReminderMinutesValue;
    private SwitchMaterial switchVibrate;
    private SwitchMaterial switchSound;
    private View rowPermissions;
    private View rowAbout;
    private View rowClearData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setupOverflowMenu(toolbar, R.id.action_settings);
        toolbar.setNavigationOnClickListener(v -> finish());

        seekBarReminderMinutes = findViewById(R.id.seekBarReminderMinutes);
        tvReminderMinutesValue = findViewById(R.id.tvReminderMinutesValue);
        switchVibrate = findViewById(R.id.switchVibrate);
        switchSound = findViewById(R.id.switchSound);
        rowPermissions = findViewById(R.id.rowPermissions);
        rowAbout = findViewById(R.id.rowAbout);
        rowClearData = findViewById(R.id.rowClearData);

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
            }
        });

        switchVibrate.setOnCheckedChangeListener((buttonView, isChecked) ->
                PrefsManager.setVibrateEnabled(SettingsActivity.this, isChecked));

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) ->
                PrefsManager.setSoundEnabled(SettingsActivity.this, isChecked));

        rowPermissions.setOnClickListener(v -> openAppPermissions());
        rowAbout.setOnClickListener(v -> showAboutDialog());
        rowClearData.setOnClickListener(v -> showClearDataDialog());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (handleOverflowMenuItem(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateMinutesLabel(int minutes) {
        tvReminderMinutesValue.setText(getString(R.string.settings_minutes_format, minutes));
    }

    private void openAppPermissions() {
        PermissionManager.openAppSettings(this);
        showToast(getString(R.string.settings_permissions_opened));
    }

    private void showClearDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_data_title)
                .setMessage(R.string.clear_data_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> clearAllData())
                .show();
    }

    private void clearAllData() {
        showLoading();
        AppExecutors.getInstance().diskIO(() -> {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
            MedicationDAO dao = new MedicationDAO(dbHelper);
            List<Schedule> schedules = dao.getAllSchedules();
            List<Medication> medications = dao.getAllMedications();

            for (Schedule schedule : schedules) {
                AlarmScheduler.cancelAlarm(getApplicationContext(), schedule.getId());
            }
            for (Medication medication : medications) {
                AlarmScheduler.cancelExpiryAlarm(getApplicationContext(), medication.getId());
            }

            dbHelper.resetDatabase();
            getSharedPreferences(PrefsManager.PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
            AppExecutors.getInstance().mainThread(() -> {
                hideLoading();
                showToast(getString(R.string.clear_data_success));
                Intent intent = new Intent(this, MedicationListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        });
    }
}
