package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.appbar.MaterialToolbar;
import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.helpers.PermissionManager;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;
import com.samiraa_raghadm_sawsana.meditrack.models.Schedule;
import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDAO;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AlarmScheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AddEditMedicationActivity extends BaseActivity {

    public static final String EXTRA_MEDICATION_ID = "MEDICATION_ID";
    private static final int REQUEST_READ_CONTACTS = 102;

    private EditText etMedicationName;
    private EditText etDosage;
    private EditText etInstructions;
    private LinearLayout llTimesContainer;
    private ImageView ivMedicationPhoto;
    private TextView tvEmergencyContact;
    private EditText etExpiryDate;
    private Button btnDelete;

    private MedicationDAO dao;
    private int medicationId;
    private boolean editMode;
    private String medicationNameForDelete = "";
    private String capturedImagePath = null;
    private String emergencyContactName = null;
    private String emergencyContactPhone = null;

    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> contactPickerLauncher;

    private final CheckBox[] dayCheckboxes = new CheckBox[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_medication);

        dao = new MedicationDAO(DatabaseHelper.getInstance(this));
        medicationId = getIntent().getIntExtra(EXTRA_MEDICATION_ID, 0);
        editMode = medicationId > 0;

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(editMode
                    ? R.string.edit_medication
                    : R.string.add_medication);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        registerLaunchers();

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
        findViewById(R.id.btnTakePhoto).setOnClickListener(v -> requestCameraPermission());
        findViewById(R.id.btnPickContact).setOnClickListener(v -> pickContact());

        btnDelete.setVisibility(editMode ? View.VISIBLE : View.GONE);
        btnDelete.setOnClickListener(v -> confirmDelete());

        if (!editMode) {
            addTimeRow("08:00");
            dayCheckboxes[0].setChecked(true);
            dayCheckboxes[1].setChecked(true);
            dayCheckboxes[2].setChecked(true);
            dayCheckboxes[3].setChecked(true);
            dayCheckboxes[4].setChecked(true);
            dayCheckboxes[5].setChecked(true);
            dayCheckboxes[6].setChecked(true);
        } else {
            loadMedicationForEdit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void registerLaunchers() {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        openCameraPreview();
                    } else if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.error_camera_denied_title)
                                .setMessage(R.string.error_camera_denied_message)
                                .setPositiveButton(R.string.open_settings,
                                        (d, w) -> PermissionManager.openAppSettings(this))
                                .setNegativeButton(R.string.error_camera_continue_without, null)
                                .show();
                    } else {
                        showToast(getString(R.string.error_camera_permission));
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        capturedImagePath = result.getData().getStringExtra(CameraActivity.EXTRA_IMAGE_PATH);
                        loadImageIntoPreview(capturedImagePath);
                    }
                });

        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri contactUri = result.getData().getData();
                        if (contactUri != null) {
                            resolveContact(contactUri);
                        }
                    }
                });
    }

    private static boolean canScheduleExact(AlarmManager am) {
        if (Build.VERSION.SDK_INT < 31)
            return true;
        try {
            return (Boolean) AlarmManager.class.getMethod("canScheduleExactAlarms").invoke(am);
        } catch (Exception e) {
            return true;
        }
    }

    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !canScheduleExact(am)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.perm_exact_alarm_title)
                        .setMessage(R.string.perm_exact_alarm_message)
                        .setPositiveButton(R.string.perm_exact_alarm_open, (d, w) -> {
                            Intent intent = new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM");
                            startActivity(intent);
                        })
                        .setNegativeButton(R.string.perm_exact_alarm_skip, null)
                        .show();
            }
        }
    }

    private void requestCameraPermission() {
        if (PermissionManager.isGranted(this, Manifest.permission.CAMERA)) {
            openCameraPreview();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.perm_camera_rationale_title)
                    .setMessage(R.string.perm_camera_rationale_message)
                    .setPositiveButton(R.string.perm_notif_allow,
                            (d, w) -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCameraPreview() {
        Intent intent = new Intent(this, CameraActivity.class);
        if (editMode) {
            intent.putExtra(CameraActivity.EXTRA_MEDICATION_ID, medicationId);
        }
        cameraLauncher.launch(intent);
    }

    private void loadImageIntoPreview(String path) {
        if (path != null && !path.isEmpty() && new File(path).exists()) {
            AppExecutors.getInstance().diskIO(() -> {
                Bitmap bmp = BitmapFactory.decodeFile(path);
                AppExecutors.getInstance().mainThread(() -> {
                    if (bmp != null) {
                        ivMedicationPhoto.setImageBitmap(bmp);
                    } else {
                        showToast(getString(R.string.error_load_image));
                        ivMedicationPhoto.setImageResource(R.drawable.ic_medication_placeholder);
                    }
                });
            });
        } else {
            ivMedicationPhoto.setImageResource(R.drawable.ic_medication_placeholder);
        }
    }

    private void pickContact() {
        if (PermissionManager.isGranted(this, Manifest.permission.READ_CONTACTS)) {
            contactPickerLauncher.launch(new Intent(Intent.ACTION_PICK,
                    ContactsContract.Contacts.CONTENT_URI));
        } else {
            requestPermissions(new String[] { Manifest.permission.READ_CONTACTS }, REQUEST_READ_CONTACTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                contactPickerLauncher.launch(new Intent(Intent.ACTION_PICK,
                        ContactsContract.Contacts.CONTENT_URI));
            } else if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.perm_contacts_denied)
                        .setPositiveButton(R.string.open_settings, (d, w) -> PermissionManager.openAppSettings(this))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        }
    }

    private void resolveContact(Uri contactUri) {
        AppExecutors.getInstance().diskIO(() -> {
            String name = "";
            String phone = "";
            Cursor nameCursor = null;
            Cursor phoneCursor = null;
            try {
                nameCursor = getContentResolver().query(contactUri,
                        new String[] {
                                ContactsContract.Contacts.DISPLAY_NAME,
                                ContactsContract.Contacts._ID
                        }, null, null, null);
                if (nameCursor != null && nameCursor.moveToFirst()) {
                    name = nameCursor.getString(nameCursor.getColumnIndexOrThrow(
                            ContactsContract.Contacts.DISPLAY_NAME));
                    String contactId = nameCursor.getString(nameCursor.getColumnIndexOrThrow(
                            ContactsContract.Contacts._ID));
                    phoneCursor = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER },
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                            new String[] { contactId }, null);
                    if (phoneCursor != null && phoneCursor.moveToFirst()) {
                        phone = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                    }
                }
            } catch (SecurityException e) {
                AppExecutors.getInstance().mainThread(
                        () -> showToast(getString(R.string.perm_contacts_denied)));
            } finally {
                if (nameCursor != null) {
                    nameCursor.close();
                }
                if (phoneCursor != null) {
                    phoneCursor.close();
                }
            }
            final String finalName = name;
            final String finalPhone = phone;
            AppExecutors.getInstance().mainThread(() -> {
                emergencyContactName = finalName;
                emergencyContactPhone = finalPhone;
                tvEmergencyContact.setText(finalName + " — " + finalPhone);
            });
        });
    }

    private void loadMedicationForEdit() {
        showLoading();
        AppExecutors.getInstance().diskIO(() -> {
            Medication medication = dao.getMedicationById(medicationId);
            List<Schedule> schedules = dao.getSchedulesForMedication(medicationId);
            AppExecutors.getInstance().mainThread(() -> {
                hideLoading();
                if (medication == null) {
                    showToast(getString(R.string.msg_medication_not_found));
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

        emergencyContactName = medication.getEmergencyContactName();
        emergencyContactPhone = medication.getEmergencyContactPhone();
        if (!TextUtils.isEmpty(emergencyContactName) || !TextUtils.isEmpty(emergencyContactPhone)) {
            tvEmergencyContact.setText(emergencyContactName + " — " + emergencyContactPhone);
        }

        String imagePath = medication.getImagePath();
        if (imagePath != null && !imagePath.isEmpty() && new File(imagePath).exists()) {
            loadImageIntoPreview(imagePath);
        } else {
            ivMedicationPhoto.setImageResource(R.drawable.ic_medication_placeholder);
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
                ? schedules.get(0).getDaysOfWeek()
                : "1,2,3,4,5,6,7");
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
        Set<String> unique = new LinkedHashSet<>(times);
        return new ArrayList<>(unique);
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
            showToast(getString(R.string.msg_validation_name));
            return;
        }

        List<String> times = collectTimes();
        if (times.isEmpty()) {
            showToast(getString(R.string.msg_validation_time));
            return;
        }

        checkExactAlarmPermission();

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
        medication.setEmergencyContactName(emergencyContactName);
        medication.setEmergencyContactPhone(emergencyContactPhone);

        showLoading();
        AppExecutors.getInstance().diskIO(() -> {
            Medication existing = null;
            if (editMode) {
                existing = dao.getMedicationById(medicationId);
            }

            if (capturedImagePath != null) {
                medication.setImagePath(capturedImagePath);
            } else if (existing != null) {
                medication.setImagePath(existing.getImagePath());
            }

            long savedId;
            if (editMode) {
                dao.updateMedication(medication);
                savedId = medicationId;
            } else {
                savedId = dao.insertMedication(medication);
            }

            int medId = (int) savedId;
            medication.setId(medId);

            dao.deleteSchedulesForMedication(medId);
            for (String time : times) {
                Schedule schedule = new Schedule();
                schedule.setMedicationId(medId);
                schedule.setIntakeTime(time);
                schedule.setDaysOfWeek(daysOfWeek);
                dao.insertSchedule(schedule);
            }

            Medication savedMedication = dao.getMedicationById(medId);
            android.content.Context appContext = getApplicationContext();
            AlarmScheduler.scheduleAllAlarms(appContext, dao);
            if (savedMedication != null) {
                AlarmScheduler.scheduleExpiryAlarm(appContext, savedMedication);
            }

            AppExecutors.getInstance().mainThread(() -> {
                hideLoading();
                showToast(getString(R.string.msg_saved_success));
                finish();
            });
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_medication_title)
                .setMessage(getString(R.string.delete_medication_message, medicationNameForDelete))
                .setPositiveButton(R.string.delete_confirm, (dialog, which) -> {
                    showLoading();
                    AppExecutors.getInstance().diskIO(() -> {
                        AlarmScheduler.cancelAlarmsForMedication(
                                getApplicationContext(), dao, medicationId);
                        dao.deleteMedication(medicationId);
                        AppExecutors.getInstance().mainThread(() -> {
                            hideLoading();
                            showToast(getString(R.string.msg_deleted_success));
                            finish();
                        });
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
