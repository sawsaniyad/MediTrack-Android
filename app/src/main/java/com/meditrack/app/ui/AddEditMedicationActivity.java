package com.meditrack.app.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AlertDialog;

import com.meditrack.app.R;
import com.meditrack.app.data.AppExecutors;
import com.meditrack.app.data.Medication;
import com.meditrack.app.data.Schedule;
import com.meditrack.app.db.DatabaseHelper;
import com.meditrack.app.db.MedicationDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddEditMedicationActivity extends BaseActivity {

    public static final String EXTRA_MEDICATION_ID = "MEDICATION_ID";

    private EditText etMedicationName;
    private EditText etDosage;
    private EditText etInstructions;
    private LinearLayout llTimesContainer;
    private ImageView ivMedicationPhoto;
    private TextView tvEmergencyContact;
    private EditText etExpiryDate;
    private Button btnDelete;

    private MedicationDao dao;
    private int medicationId;
    private boolean editMode;
    private String medicationNameForDelete = "";

    private final CheckBox[] dayCheckboxes = new CheckBox[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_medication);

        dao = new MedicationDao(DatabaseHelper.getInstance(this));
        medicationId = getIntent().getIntExtra(EXTRA_MEDICATION_ID, 0);
        editMode = medicationId > 0;

        etMedicationName = findViewById(R.id.etMedicationName);
        etDosage = findViewById(R.id.etDosage);
        etInstructions = findViewById(R.id.etInstructions);
        llTimesContainer = findViewById(R.id.llTimesContainer);
        ivMedicationPhoto = findViewById(R.id.ivMedicationPhoto);
        tvEmergencyContact = findViewById(R.id.tvEmergencyContact);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        btnDelete = findViewById(R.id.btnDelete);

        dayCheckboxes[0] = findViewById(R.id.cbMon);
        dayCheckboxes[1] = findViewById(R.id.cbTue);
        dayCheckboxes[2] = findViewById(R.id.cbWed);
        dayCheckboxes[3] = findViewById(R.id.cbThu);
        dayCheckboxes[4] = findViewById(R.id.cbFri);
        dayCheckboxes[5] = findViewById(R.id.cbSat);
        dayCheckboxes[6] = findViewById(R.id.cbSun);

        findViewById(R.id.btnAddTime).setOnClickListener(v -> addTimeRow("08:00"));
        findViewById(R.id.btnSave).setOnClickListener(v -> saveMedication());
        findViewById(R.id.btnTakePhoto).setOnClickListener(v ->
                showToast("צילום יתווסף בשלב הבא"));
        findViewById(R.id.btnPickContact).setOnClickListener(v ->
                showToast("בחירת איש קשר תתווסף בשלב הבא"));

        btnDelete.setVisibility(editMode ? View.VISIBLE : View.GONE);
        btnDelete.setOnClickListener(v -> confirmDelete());

        if (!editMode) {
            addTimeRow("08:00");
            dayCheckboxes[0].setChecked(true);
            dayCheckboxes[1].setChecked(true);
            dayCheckboxes[2].setChecked(true);
            dayCheckboxes[3].setChecked(true);
            dayCheckboxes[4].setChecked(true);
        } else {
            loadMedicationForEdit();
        }
    }

    private void loadMedicationForEdit() {
        showLoading();
        AppExecutors.getInstance().diskIO(() -> {
            Medication medication = dao.getMedicationById(medicationId);
            List<Schedule> schedules = dao.getSchedulesForMedication(medicationId);
            AppExecutors.getInstance().mainThread(() -> {
                hideLoading();
                if (medication == null) {
                    showToast("תרופה לא נמצאה");
                    finish();
                    return;
                }
                populateForm(medication, schedules);
            });
        });
    }

    private void populateForm(Medication medication, List<Schedule> schedules) {
        medicationNameForDelete = medication.getName();
        etMedicationName.setText(medication.getName());
        etDosage.setText(medication.getDosage());
        etInstructions.setText(medication.getInstructions());
        etExpiryDate.setText(medication.getExpiryDate() != null ? medication.getExpiryDate() : "");

        if (!TextUtils.isEmpty(medication.getEmergencyContactName())
                || !TextUtils.isEmpty(medication.getEmergencyContactPhone())) {
            tvEmergencyContact.setText(medication.getEmergencyContactName() + " — "
                    + medication.getEmergencyContactPhone());
        }

        llTimesContainer.removeAllViews();
        if (schedules.isEmpty()) {
            addTimeRow("08:00");
        } else {
            for (Schedule schedule : schedules) {
                addTimeRow(schedule.getIntakeTime());
            }
        }

        setDaysFromString(!schedules.isEmpty()
                ? schedules.get(0).getDaysOfWeek() : "1,2,3,4,5,6,7");
    }

    private void setDaysFromString(String daysOfWeek) {
        for (CheckBox checkBox : dayCheckboxes) {
            checkBox.setChecked(false);
        }
        if (TextUtils.isEmpty(daysOfWeek)) {
            return;
        }
        String[] parts = daysOfWeek.split(",");
        for (String part : parts) {
            try {
                int day = Integer.parseInt(part.trim());
                if (day >= 1 && day <= 7) {
                    dayCheckboxes[day - 1].setChecked(true);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void addTimeRow(String time) {
        View row = LayoutInflater.from(this).inflate(R.layout.layout_time_row, llTimesContainer, false);
        TimePicker timePicker = row.findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        String[] parts = time.split(":");
        if (parts.length == 2) {
            try {
                timePicker.setHour(Integer.parseInt(parts[0]));
                timePicker.setMinute(Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        row.findViewById(R.id.btnRemoveTime).setOnClickListener(v -> llTimesContainer.removeView(row));
        llTimesContainer.addView(row);
    }

    private List<String> collectTimes() {
        List<String> times = new ArrayList<>();
        for (int i = 0; i < llTimesContainer.getChildCount(); i++) {
            View row = llTimesContainer.getChildAt(i);
            TimePicker timePicker = row.findViewById(R.id.timePicker);
            times.add(String.format(Locale.getDefault(), "%02d:%02d",
                    timePicker.getHour(), timePicker.getMinute()));
        }
        return times;
    }

    private String collectDaysOfWeek() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < dayCheckboxes.length; i++) {
            if (dayCheckboxes[i].isChecked()) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(i + 1);
            }
        }
        if (builder.length() == 0) {
            return "1,2,3,4,5,6,7";
        }
        return builder.toString();
    }

    private void saveMedication() {
        String name = etMedicationName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            showToast("נא להזין שם תרופה");
            return;
        }

        List<String> times = collectTimes();
        if (times.isEmpty()) {
            showToast("נא להוסיף לפחות שעת נטילה");
            return;
        }

        String daysOfWeek = collectDaysOfWeek();
        Medication medication = new Medication();
        if (editMode) {
            medication.setId(medicationId);
        }
        medication.setName(name);
        medication.setDosage(etDosage.getText().toString().trim());
        medication.setInstructions(etInstructions.getText().toString().trim());
        medication.setExpiryDate(etExpiryDate.getText().toString().trim());
        medication.setActive(true);

        showLoading();
        AppExecutors.getInstance().diskIO(() -> {
            if (editMode) {
                Medication existing = dao.getMedicationById(medicationId);
                if (existing != null) {
                    medication.setImagePath(existing.getImagePath());
                    medication.setEmergencyContactName(existing.getEmergencyContactName());
                    medication.setEmergencyContactPhone(existing.getEmergencyContactPhone());
                }
            }

            long savedId;
            if (editMode) {
                dao.updateMedication(medication);
                savedId = medicationId;
            } else {
                savedId = dao.insertMedication(medication);
            }

            int medId = (int) savedId;
            dao.deleteSchedulesForMedication(medId);
            for (String time : times) {
                Schedule schedule = new Schedule();
                schedule.setMedicationId(medId);
                schedule.setIntakeTime(time);
                schedule.setDaysOfWeek(daysOfWeek);
                dao.insertSchedule(schedule);
            }

            AppExecutors.getInstance().mainThread(() -> {
                hideLoading();
                showToast("תרופה נשמרה בהצלחה");
                finish();
            });
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת תרופה")
                .setMessage("האם אתה בטוח שברצונך למחוק את " + medicationNameForDelete + "?")
                .setPositiveButton("מחק", (dialog, which) -> {
                    showLoading();
                    AppExecutors.getInstance().diskIO(() -> {
                        dao.deleteMedication(medicationId);
                        AppExecutors.getInstance().mainThread(() -> {
                            hideLoading();
                            showToast("תרופה נמחקה");
                            finish();
                        });
                    });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}
