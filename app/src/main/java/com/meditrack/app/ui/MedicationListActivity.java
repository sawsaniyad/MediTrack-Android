package com.meditrack.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.meditrack.app.R;
import com.meditrack.app.data.AppExecutors;
import com.meditrack.app.data.IntakeLog;
import com.meditrack.app.data.Medication;
import com.meditrack.app.data.Schedule;
import com.meditrack.app.db.DatabaseHelper;
import com.meditrack.app.db.MedicationDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MedicationListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private View tvEmptyState;
    private MedicationAdapter adapter;
    private MedicationDao dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_list);

        dao = new MedicationDao(DatabaseHelper.getInstance(this));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerViewMedications);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MedicationAdapter(null, dao, medication -> {
            Intent intent = new Intent(MedicationListActivity.this, AddEditMedicationActivity.class);
            intent.putExtra("MEDICATION_ID", medication.getId());
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
        loadMedications();
    }

    private void loadMedications() {
        AppExecutors.getInstance().diskIO(() -> {
            List<Medication> list = dao.getActiveMedications();
            List<IntakeLog> todayLogs = dao.getTodayLogs();
            Map<Integer, List<Schedule>> schedulesMap = new HashMap<>();
            for (Medication medication : list) {
                schedulesMap.put(medication.getId(),
                        dao.getSchedulesForMedication(medication.getId()));
            }
            AppExecutors.getInstance().mainThread(() -> {
                if (list.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmptyState.setVisibility(View.GONE);
                    adapter.updateList(list, todayLogs, schedulesMap);
                }
            });
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
                .setTitle("MediTrack")
                .setMessage("פותח על ידי:\nסמירה אבו אלהוא — 324909803\nרגד מחיסן — 212541304\nסאוסן אבו שמעה — 213588270")
                .setPositiveButton("סגור", null)
                .show();
    }
}
