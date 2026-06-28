package com.samiraa_raghadm_sawsana.meditrack.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;
import com.samiraa_raghadm_sawsana.meditrack.models.Schedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * שכבת גישה לנתונים (DAO) - כל פעולות ה-CRUD מול מסד הנתונים
 * Data Access Object: the single layer that talks to SQLite.
 *
 * אחריות / Responsibilities:
 * - CRUD על שלוש הטבלאות: medications, schedules, intake_log
 * - המרה דו-כיוונית בין Cursor למודלים (cursorTo... / ...ToContentValues)
 * - אף Cursor אינו דולף החוצה — כל שאילתה ממופה למודל ונסגרת ב-finally
 *
 * הערות / Notes:
 * - getActiveMedications מחזיר רק תרופות פעילות (is_active = 1)
 * - deleteMedication הוא מחיקה רכה (soft delete) — השורה נשמרת כדי שההיסטוריה תישרד
 * - רשימות נטילה מוחזרות לפי scheduled_datetime בסדר עולה (ASC)
 *
 * כל הקריאות אמורות לרוץ ב-background thread (AppExecutors.diskIO).
 *
 * כותבת: סמירה אבו אל-הווא
 */
public class MedicationDAO {

    private final DatabaseHelper dbHelper;

    public MedicationDAO(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // ================================================================
    // MEDICATIONS
    // ================================================================

    public List<Medication> getActiveMedications() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.TABLE_MEDICATIONS, null,
                    DatabaseHelper.COL_MED_IS_ACTIVE + " = ?", new String[] { "1" },
                    null, null, DatabaseHelper.COL_MED_NAME + " ASC");
            return cursorToMedicationList(cursor);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public List<Medication> getAllMedications() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.TABLE_MEDICATIONS, null,
                    null, null, null, null, DatabaseHelper.COL_MED_NAME + " ASC");
            return cursorToMedicationList(cursor);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public Medication getMedicationById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.TABLE_MEDICATIONS, null,
                    DatabaseHelper.COL_MED_ID + " = ?", new String[] { String.valueOf(id) },
                    null, null, null);
            return cursor.moveToFirst() ? cursorToMedication(cursor) : null;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public long insertMedication(Medication medication) {
        return dbHelper.getWritableDatabase().insert(
                DatabaseHelper.TABLE_MEDICATIONS, null, medicationToContentValues(medication));
    }

    public void updateMedication(Medication medication) {
        dbHelper.getWritableDatabase().update(DatabaseHelper.TABLE_MEDICATIONS,
                medicationToContentValues(medication),
                DatabaseHelper.COL_MED_ID + " = ?",
                new String[] { String.valueOf(medication.getId()) });
    }

    public void deleteMedication(int medicationId) {
        // Soft delete: keep the row so intake history survives.
        // Also disable its schedules so no future alarms fire for a "deleted" med.
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues medValues = new ContentValues();
            medValues.put(DatabaseHelper.COL_MED_IS_ACTIVE, 0);
            db.update(DatabaseHelper.TABLE_MEDICATIONS, medValues,
                    DatabaseHelper.COL_MED_ID + " = ?",
                    new String[] { String.valueOf(medicationId) });

            ContentValues schValues = new ContentValues();
            schValues.put(DatabaseHelper.COL_SCH_IS_ENABLED, 0);
            db.update(DatabaseHelper.TABLE_SCHEDULES, schValues,
                    DatabaseHelper.COL_SCH_MEDICATION_ID + " = ?",
                    new String[] { String.valueOf(medicationId) });

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ================================================================
    // SCHEDULES
    // ================================================================

    public List<Schedule> getSchedulesForMedication(int medicationId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.TABLE_SCHEDULES, null,
                    DatabaseHelper.COL_SCH_MEDICATION_ID + " = ?",
                    new String[] { String.valueOf(medicationId) },
                    null, null, DatabaseHelper.COL_SCH_INTAKE_TIME + " ASC");
            return cursorToScheduleList(cursor);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public List<Schedule> getAllSchedules() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.TABLE_SCHEDULES, null,
                    null, null, null, null, DatabaseHelper.COL_SCH_INTAKE_TIME + " ASC");
            return cursorToScheduleList(cursor);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public void insertSchedule(Schedule schedule) {
        dbHelper.getWritableDatabase().insert(
                DatabaseHelper.TABLE_SCHEDULES, null, scheduleToContentValues(schedule));
    }

    public void deleteSchedulesForMedication(int medicationId) {
        dbHelper.getWritableDatabase().delete(DatabaseHelper.TABLE_SCHEDULES,
                DatabaseHelper.COL_SCH_MEDICATION_ID + " = ?",
                new String[] { String.valueOf(medicationId) });
    }

    // ================================================================
    // INTAKE LOG
    // ================================================================

    public List<IntakeLog> getTodayLogs() {
        String today = LocalDate.now().toString();
        return getLogsByDateRange(today, today);
    }

    public List<IntakeLog> getLogsByDateRange(String startDate, String endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.TABLE_INTAKE_LOG, null,
                    "substr(" + DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + ", 1, 10) BETWEEN ? AND ?",
                    new String[] { startDate, endDate },
                    null, null, DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + " ASC");
            return cursorToIntakeLogList(cursor);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public List<IntakeLog> getLogsByMedication(int medicationId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.TABLE_INTAKE_LOG, null,
                    DatabaseHelper.COL_LOG_MEDICATION_ID + " = ?",
                    new String[] { String.valueOf(medicationId) },
                    null, null, DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + " ASC");
            return cursorToIntakeLogList(cursor);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public List<IntakeLog> getAllLogs() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.TABLE_INTAKE_LOG, null,
                    null, null, null, null,
                    DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + " ASC");
            return cursorToIntakeLogList(cursor);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public void insertIntakeLog(IntakeLog log) {
        dbHelper.getWritableDatabase().insert(
                DatabaseHelper.TABLE_INTAKE_LOG, null, intakeLogToContentValues(log));
    }

    public void markAsTaken(int logId, String actualDatetime, boolean wasDelayed) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_LOG_TAKEN, 1);
        values.put(DatabaseHelper.COL_LOG_ACTUAL_DATETIME, actualDatetime);
        values.put(DatabaseHelper.COL_LOG_STATUS, IntakeLog.STATUS_TAKEN);
        if (wasDelayed) {
            values.put(DatabaseHelper.COL_LOG_WAS_DELAYED, 1);
        }
        dbHelper.getWritableDatabase().update(DatabaseHelper.TABLE_INTAKE_LOG, values,
                DatabaseHelper.COL_LOG_ID + " = ?", new String[] { String.valueOf(logId) });
    }

    // ================================================================
    // Private helpers
    // ================================================================

    private ContentValues medicationToContentValues(Medication medication) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_MED_NAME, medication.getName());
        values.put(DatabaseHelper.COL_MED_DOSAGE, medication.getDosage());
        values.put(DatabaseHelper.COL_MED_INSTRUCTIONS, medication.getInstructions());
        values.put(DatabaseHelper.COL_MED_IMAGE_PATH, medication.getImagePath());
        values.put(DatabaseHelper.COL_MED_EXPIRY_DATE, medication.getExpiryDate());
        values.put(DatabaseHelper.COL_MED_EMERGENCY_CONTACT_NAME, medication.getEmergencyContactName());
        values.put(DatabaseHelper.COL_MED_EMERGENCY_CONTACT_PHONE, medication.getEmergencyContactPhone());
        values.put(DatabaseHelper.COL_MED_IS_ACTIVE, medication.isActive() ? 1 : 0);
        return values;
    }

    private ContentValues scheduleToContentValues(Schedule schedule) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SCH_MEDICATION_ID, schedule.getMedicationId());
        values.put(DatabaseHelper.COL_SCH_INTAKE_TIME, schedule.getIntakeTime());
        values.put(DatabaseHelper.COL_SCH_DAYS_OF_WEEK, schedule.getDaysOfWeek());
        values.put(DatabaseHelper.COL_SCH_IS_ENABLED, schedule.isEnabled() ? 1 : 0);
        return values;
    }

    private ContentValues intakeLogToContentValues(IntakeLog log) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_LOG_MEDICATION_ID, log.getMedicationId());
        values.put(DatabaseHelper.COL_LOG_MEDICATION_NAME, log.getMedicationName());
        values.put(DatabaseHelper.COL_LOG_SCHEDULED_DATETIME, log.getScheduledDatetime());
        values.put(DatabaseHelper.COL_LOG_TAKEN, log.isTaken() ? 1 : 0);
        values.put(DatabaseHelper.COL_LOG_ACTUAL_DATETIME, log.getActualDatetime());
        values.put(DatabaseHelper.COL_LOG_WAS_DELAYED, log.isWasDelayed() ? 1 : 0);
        values.put(DatabaseHelper.COL_LOG_STATUS, log.getStatus());
        return values;
    }

    private Medication cursorToMedication(Cursor cursor) {
        Medication medication = new Medication();
        medication.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_ID)));
        medication.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_NAME)));
        medication.setDosage(getStringOrEmpty(cursor, DatabaseHelper.COL_MED_DOSAGE));
        medication.setInstructions(getStringOrEmpty(cursor, DatabaseHelper.COL_MED_INSTRUCTIONS));
        medication.setImagePath(getStringOrNull(cursor, DatabaseHelper.COL_MED_IMAGE_PATH));
        medication.setExpiryDate(getStringOrNull(cursor, DatabaseHelper.COL_MED_EXPIRY_DATE));
        medication.setEmergencyContactName(getStringOrNull(cursor, DatabaseHelper.COL_MED_EMERGENCY_CONTACT_NAME));
        medication.setEmergencyContactPhone(getStringOrNull(cursor, DatabaseHelper.COL_MED_EMERGENCY_CONTACT_PHONE));
        medication.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_IS_ACTIVE)) == 1);
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
        schedule.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_ID)));
        schedule.setMedicationId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_MEDICATION_ID)));
        schedule.setIntakeTime(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_INTAKE_TIME)));
        schedule.setDaysOfWeek(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_DAYS_OF_WEEK)));
        schedule.setEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_IS_ENABLED)) == 1);
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
        log.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_ID)));
        log.setMedicationId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_MEDICATION_ID)));
        log.setMedicationName(getStringOrNull(cursor, DatabaseHelper.COL_LOG_MEDICATION_NAME));
        log.setScheduledDatetime(
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_SCHEDULED_DATETIME)));
        log.setTaken(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_TAKEN)) == 1);
        log.setActualDatetime(getStringOrNull(cursor, DatabaseHelper.COL_LOG_ACTUAL_DATETIME));
        log.setWasDelayed(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_WAS_DELAYED)) == 1);
        log.setStatus(getStringOrEmpty(cursor, DatabaseHelper.COL_LOG_STATUS));
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
        return cursor.isNull(index) ? null : cursor.getString(index);
    }
}
