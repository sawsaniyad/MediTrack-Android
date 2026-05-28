package com.meditrack.app.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.meditrack.app.data.IntakeLog;
import com.meditrack.app.data.Medication;
import com.meditrack.app.data.Schedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MedicationDao {

    private final DatabaseHelper dbHelper;

    public MedicationDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insertMedication(Medication medication) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(DatabaseHelper.TABLE_MEDICATIONS, null, medicationToContentValues(medication));
    }

    public int updateMedication(Medication medication) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.update(
                DatabaseHelper.TABLE_MEDICATIONS,
                medicationToContentValues(medication),
                "id = ?",
                new String[]{String.valueOf(medication.getId())}
        );
    }

    public int deleteMedication(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                DatabaseHelper.TABLE_MEDICATIONS,
                "id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public Medication getMedicationById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_MEDICATIONS,
                    null,
                    "id = ?",
                    new String[]{String.valueOf(id)},
                    null,
                    null,
                    null
            );
            if (cursor.moveToFirst()) {
                return cursorToMedication(cursor);
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<Medication> getAllMedications() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_MEDICATIONS,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "name ASC"
            );
            return cursorToMedicationList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<Medication> getActiveMedications() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_MEDICATIONS,
                    null,
                    "is_active = ?",
                    new String[]{"1"},
                    null,
                    null,
                    "name ASC"
            );
            return cursorToMedicationList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public long insertSchedule(Schedule schedule) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(DatabaseHelper.TABLE_SCHEDULES, null, scheduleToContentValues(schedule));
    }

    public List<Schedule> getSchedulesForMedication(int medicationId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_SCHEDULES,
                    null,
                    "medication_id = ?",
                    new String[]{String.valueOf(medicationId)},
                    null,
                    null,
                    "intake_time ASC"
            );
            return cursorToScheduleList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public int deleteSchedulesForMedication(int medicationId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                DatabaseHelper.TABLE_SCHEDULES,
                "medication_id = ?",
                new String[]{String.valueOf(medicationId)}
        );
    }

    public List<Schedule> getAllSchedules() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_SCHEDULES,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "intake_time ASC"
            );
            return cursorToScheduleList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public long insertIntakeLog(IntakeLog log) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(DatabaseHelper.TABLE_INTAKE_LOG, null, intakeLogToContentValues(log));
    }

    public int updateIntakeLog(IntakeLog log) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.update(
                DatabaseHelper.TABLE_INTAKE_LOG,
                intakeLogToContentValues(log),
                "id = ?",
                new String[]{String.valueOf(log.getId())}
        );
    }

    public List<IntakeLog> getTodayLogs() {
        String today = LocalDate.now().toString();
        return getLogsByDateRange(today, today);
    }

    public List<IntakeLog> getLogsByMedication(int medicationId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    null,
                    "medication_id = ?",
                    new String[]{String.valueOf(medicationId)},
                    null,
                    null,
                    "scheduled_datetime DESC"
            );
            return cursorToIntakeLogList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<IntakeLog> getLogsByDateRange(String startDate, String endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    null,
                    "substr(scheduled_datetime, 1, 10) BETWEEN ? AND ?",
                    new String[]{startDate, endDate},
                    null,
                    null,
                    "scheduled_datetime ASC"
            );
            return cursorToIntakeLogList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public int markAsTaken(int logId, String actualDatetime) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("taken", 1);
        values.put("actual_datetime", actualDatetime);
        return db.update(
                DatabaseHelper.TABLE_INTAKE_LOG,
                values,
                "id = ?",
                new String[]{String.valueOf(logId)}
        );
    }

    private ContentValues medicationToContentValues(Medication medication) {
        ContentValues values = new ContentValues();
        if (medication.getId() != 0) {
            values.put("id", medication.getId());
        }
        values.put("name", medication.getName());
        values.put("dosage", medication.getDosage());
        values.put("instructions", medication.getInstructions());
        values.put("image_path", medication.getImagePath());
        values.put("expiry_date", medication.getExpiryDate());
        values.put("emergency_contact_name", medication.getEmergencyContactName());
        values.put("emergency_contact_phone", medication.getEmergencyContactPhone());
        values.put("is_active", medication.isActive() ? 1 : 0);
        return values;
    }

    private ContentValues scheduleToContentValues(Schedule schedule) {
        ContentValues values = new ContentValues();
        if (schedule.getId() != 0) {
            values.put("id", schedule.getId());
        }
        values.put("medication_id", schedule.getMedicationId());
        values.put("intake_time", schedule.getIntakeTime());
        values.put("days_of_week", schedule.getDaysOfWeek());
        return values;
    }

    private ContentValues intakeLogToContentValues(IntakeLog log) {
        ContentValues values = new ContentValues();
        if (log.getId() != 0) {
            values.put("id", log.getId());
        }
        values.put("medication_id", log.getMedicationId());
        values.put("scheduled_datetime", log.getScheduledDatetime());
        values.put("taken", log.isTaken() ? 1 : 0);
        values.put("actual_datetime", log.getActualDatetime());
        values.put("was_delayed", log.isWasDelayed() ? 1 : 0);
        return values;
    }

    private Medication cursorToMedication(Cursor cursor) {
        Medication medication = new Medication();
        medication.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        medication.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        medication.setDosage(getStringOrEmpty(cursor, "dosage"));
        medication.setInstructions(getStringOrEmpty(cursor, "instructions"));
        medication.setImagePath(getStringOrNull(cursor, "image_path"));
        medication.setExpiryDate(getStringOrNull(cursor, "expiry_date"));
        medication.setEmergencyContactName(getStringOrNull(cursor, "emergency_contact_name"));
        medication.setEmergencyContactPhone(getStringOrNull(cursor, "emergency_contact_phone"));
        medication.setActive(cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1);
        return medication;
    }

    private List<Medication> cursorToMedicationList(Cursor cursor) {
        List<Medication> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            list.add(cursorToMedication(cursor));
        }
        return list;
    }

    private Schedule cursorToSchedule(Cursor cursor) {
        Schedule schedule = new Schedule();
        schedule.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        schedule.setMedicationId(cursor.getInt(cursor.getColumnIndexOrThrow("medication_id")));
        schedule.setIntakeTime(cursor.getString(cursor.getColumnIndexOrThrow("intake_time")));
        schedule.setDaysOfWeek(cursor.getString(cursor.getColumnIndexOrThrow("days_of_week")));
        return schedule;
    }

    private List<Schedule> cursorToScheduleList(Cursor cursor) {
        List<Schedule> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            list.add(cursorToSchedule(cursor));
        }
        return list;
    }

    private IntakeLog cursorToIntakeLog(Cursor cursor) {
        IntakeLog log = new IntakeLog();
        log.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        log.setMedicationId(cursor.getInt(cursor.getColumnIndexOrThrow("medication_id")));
        log.setScheduledDatetime(cursor.getString(cursor.getColumnIndexOrThrow("scheduled_datetime")));
        log.setTaken(cursor.getInt(cursor.getColumnIndexOrThrow("taken")) == 1);
        log.setActualDatetime(getStringOrNull(cursor, "actual_datetime"));
        log.setWasDelayed(cursor.getInt(cursor.getColumnIndexOrThrow("was_delayed")) == 1);
        return log;
    }

    private List<IntakeLog> cursorToIntakeLogList(Cursor cursor) {
        List<IntakeLog> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            list.add(cursorToIntakeLog(cursor));
        }
        return list;
    }

    private String getStringOrEmpty(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        String value = cursor.getString(index);
        return value != null ? value : "";
    }

    private String getStringOrNull(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        if (cursor.isNull(index)) {
            return null;
        }
        return cursor.getString(index);
    }
}
