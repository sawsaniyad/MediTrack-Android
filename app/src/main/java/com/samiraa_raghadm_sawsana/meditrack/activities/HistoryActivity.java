package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.adapters.HistoryAdapter;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.models.MedicationSpinnerItem;
import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;
import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryActivity extends BaseActivity {

    private MedicationDao dao;
    private HistoryAdapter adapter;
    private TextView tvSelectedDate;
    private TextView tvHistoryEmpty;
    private Spinner spinnerMedication;
    private String selectedDate;
    private int selectedMedId = -1;
    private final List<MedicationSpinnerItem> spinnerItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dao = new MedicationDao(DatabaseHelper.getInstance(this));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty);
        spinnerMedication = findViewById(R.id.spinnerMedication);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        recyclerView.setAdapter(adapter);

        tvSelectedDate.setText(R.string.filter_all);

        Button btnFilterDate = findViewById(R.id.btnFilterDate);
        Button btnClearFilter = findViewById(R.id.btnClearFilter);

        btnFilterDate.setOnClickListener(v -> showDatePicker());
        btnClearFilter.setOnClickListener(v -> clearFilters());

        spinnerMedication.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMedId = spinnerItems.get(position).getId();
                loadHistory(selectedDate, selectedMedId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        populateSpinner();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateSpinner() {
        AppExecutors.getInstance().diskIO(() -> {
            List<Medication> medications = dao.getAllMedications();
            AppExecutors.getInstance().mainThread(() -> {
                spinnerItems.clear();
                spinnerItems.add(new MedicationSpinnerItem(-1, getString(R.string.filter_all)));
                for (Medication medication : medications) {
                    spinnerItems.add(new MedicationSpinnerItem(
                            medication.getId(), medication.getName()));
                }
                ArrayAdapter<MedicationSpinnerItem> spinnerAdapter =
                        new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerItems);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerMedication.setAdapter(spinnerAdapter);
                loadHistory(selectedDate, selectedMedId);
            });
        });
    }

    private void showDatePicker() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    year, month + 1, dayOfMonth);
            tvSelectedDate.setText(selectedDate);
            loadHistory(selectedDate, selectedMedId);
        }, calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private void clearFilters() {
        selectedDate = null;
        selectedMedId = -1;
        tvSelectedDate.setText(R.string.filter_all);
        spinnerMedication.setSelection(0);
        loadHistory(null, -1);
    }

    private void loadHistory(String dateFilter, int medicationIdFilter) {
        AppExecutors.getInstance().diskIO(() -> {
            List<IntakeLog> logs;
            if (dateFilter != null && medicationIdFilter > 0) {
                logs = filterByDate(dao.getLogsByMedication(medicationIdFilter), dateFilter);
            } else if (dateFilter != null) {
                logs = dao.getLogsByDateRange(dateFilter, dateFilter);
            } else if (medicationIdFilter > 0) {
                logs = dao.getLogsByMedication(medicationIdFilter);
            } else {
                logs = dao.getTodayLogs();
            }

            Map<Integer, String> names = buildMedicationNameMap();
            AppExecutors.getInstance().mainThread(() -> {
                adapter.updateList(logs, names);
                tvHistoryEmpty.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private List<IntakeLog> filterByDate(List<IntakeLog> logs, String dateFilter) {
        List<IntakeLog> filtered = new ArrayList<>();
        for (IntakeLog log : logs) {
            if (log.getScheduledDatetime() != null
                    && log.getScheduledDatetime().startsWith(dateFilter)) {
                filtered.add(log);
            }
        }
        return filtered;
    }

    private Map<Integer, String> buildMedicationNameMap() {
        Map<Integer, String> map = new HashMap<>();
        for (Medication medication : dao.getAllMedications()) {
            map.put(medication.getId(), medication.getName());
        }
        return map;
    }
}
