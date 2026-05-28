package com.meditrack.app.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.meditrack.app.BuildConfig;
import com.meditrack.app.R;
import com.meditrack.app.data.AppExecutors;
import com.meditrack.app.data.IntakeLog;
import com.meditrack.app.data.Medication;
import com.meditrack.app.data.Schedule;
import com.meditrack.app.db.DatabaseHelper;
import com.meditrack.app.db.MedicationDao;
import com.meditrack.app.services.AlarmScheduler;
import com.meditrack.app.services.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MedicationListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private View emptyStateContainer;
    private MedicationAdapter adapter;
    private MedicationDao dao;
    private ActivityResultLauncher<String> notifPermLauncher;

    private final BroadcastReceiver medicationDueReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int medicationId = intent.getIntExtra(getString(R.string.extra_medication_id), -1);
            String medName = intent.getStringExtra(getString(R.string.extra_medication_name));
            if (medName == null) {
                medName = getString(R.string.default_medication_name);
            }
            showMedicationDueSnackbar(medName, medicationId);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_list);

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
        }

        dao = new MedicationDao(DatabaseHelper.getInstance(this));

        NotificationHelper.createNotificationChannel(this);

        notifPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (!granted) {
                        showToast(getString(R.string.msg_notifications_disabled));
                    } else {
                        scheduleAlarms();
                    }
                });

        requestNotificationPermission();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerViewMedications);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MedicationAdapter(medication -> {
            Intent intent = new Intent(MedicationListActivity.this, AddEditMedicationActivity.class);
            intent.putExtra(AddEditMedicationActivity.EXTRA_MEDICATION_ID, medication.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddMedication);
        fab.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditMedicationActivity.class)));

        loadMedications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                medicationDueReceiver,
                new IntentFilter(getString(R.string.broadcast_medication_due)));
        loadMedications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(medicationDueReceiver);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionManager.isGranted(this, Manifest.permission.POST_NOTIFICATIONS)) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.perm_notif_title)
                            .setMessage(R.string.perm_notif_message)
                            .setPositiveButton(R.string.perm_notif_allow, (d, w) ->
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                            .setNegativeButton(R.string.perm_notif_later, null)
                            .show();
                } else {
                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            } else {
                scheduleAlarms();
            }
        } else {
            scheduleAlarms();
        }
    }

    private void scheduleAlarms() {
        AlarmScheduler.scheduleAllAlarms(getApplicationContext(), dao);
    }

    private void loadMedications() {
        // FIXED: moved to diskIO
        AppExecutors.getInstance().diskIO(() -> {
            List<Medication> list = dao.getActiveMedications();
            List<IntakeLog> todayLogs = dao.getTodayLogs();
            Map<Integer, IntakeLog> todayLogsMap = new HashMap<>();
            for (IntakeLog log : todayLogs) {
                todayLogsMap.put(log.getMedicationId(), log);
            }
            Map<Integer, List<Schedule>> schedulesMap = new HashMap<>();
            for (Medication medication : list) {
                schedulesMap.put(medication.getId(),
                        dao.getSchedulesForMedication(medication.getId()));
            }
            AppExecutors.getInstance().mainThread(() -> {
                if (list.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyStateContainer.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateContainer.setVisibility(View.GONE);
                    adapter.updateList(list, todayLogs, schedulesMap, todayLogsMap);
                }
            });
        });
    }

    private void showMedicationDueSnackbar(String medName, int medicationId) {
        Snackbar.make(recyclerView,
                        getString(R.string.msg_medication_due, medName),
                        Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_action_taken, v -> markMedicationTaken(medicationId))
                .show();
    }

    private void markMedicationTaken(int medicationId) {
        String actualTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()).format(new Date());
        AppExecutors.getInstance().diskIO(() -> {
            List<IntakeLog> logs = dao.getLogsByMedication(medicationId);
            for (int i = logs.size() - 1; i >= 0; i--) {
                IntakeLog log = logs.get(i);
                if (!log.isTaken()) {
                    dao.markAsTaken(log.getId(), actualTime);
                    break;
                }
            }
            AppExecutors.getInstance().mainThread(this::loadMedications);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_medication_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.action_history) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_exit) {
            finish();
            System.exit(0);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.about_close, null)
                .show();
    }
}
