package com.samiraa_raghadm_sawsana.meditrack.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;
import com.samiraa_raghadm_sawsana.meditrack.models.Schedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DAO מאוחד לתרופות, תזמונים ורישום נטילות
 * Merged DAO combining the best of both versions:
 *
 * מ-Samira:
 * - ExecutorService לפעולות רקע (חובה בקורס!)
 * - Callback interfaces להחזרת תוצאות ל-UI thread
 * - הפרדה נכונה בין DB thread ל-UI thread
 *
 * מ-Raghad:
 * - try/finally לסגירת Cursor בטוחה
 * - getActiveMedications() — רשימת תרופות פעילות בלבד
 * - getTodayLogs() — לוג של היום
 * - getLogsByDateRange() — סינון לפי טווח תאריכים
 * - markAsTaken() — סימון נטילה
 * - helper methods: getStringOrNull, getStringOrEmpty
 *
 * כותבת: סמירה אבו אל-הווא
 */
public class MedicationDAO {

    // ================================================================
    // Callback Interfaces — להחזרת תוצאות מה-background thread ל-UI
    // ================================================================

    public interface OnMedicationResult {
        void onResult(Medication medication);
    }

    public interface OnMedicationListResult {
        void onResult(List<Medication> medications);
    }

    public interface OnScheduleListResult {
        void onResult(List<Schedule> schedules);
    }

    public interface OnLogListResult {
        void onResult(List<IntakeLog> logs);
    }

    public interface OnInsertResult {
        void onResult(long insertedId);
    }

    public interface OnOperationResult {
        void onResult(boolean success);
    }

    // -------- שדות / Fields --------
    private final DatabaseHelper dbHelper;
    private final ExecutorService executor;

    // -------- בנאי / Constructor --------
    public MedicationDAO(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        // thread יחיד לפעולות DB סדרתיות — מונע בעיות race condition
        this.executor = Executors.newSingleThreadExecutor();
    }

    public MedicationDAO(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
        this.executor = Executors.newSingleThreadExecutor();
    }

    // ================================================================
    // MEDICATIONS — פעולות על טבלת תרופות
    // ================================================================

    /**
     * הוספת תרופה חדשה בthread רקע
     */
    public void insertMedication(Medication medication, OnInsertResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            long id = db.insert(DatabaseHelper.TABLE_MEDICATIONS, null,
                    medicationToContentValues(medication));
            if (callback != null) callback.onResult(id);
        });
    }

    public long insertMedication(Medication medication) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(DatabaseHelper.TABLE_MEDICATIONS, null,
                medicationToContentValues(medication));
    }

    /**
     * עדכון תרופה קיימת בthread רקע
     */
    public void updateMedication(Medication medication, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int rows = db.update(
                    DatabaseHelper.TABLE_MEDICATIONS,
                    medicationToContentValues(medication),
                    DatabaseHelper.COL_MED_ID + " = ?",
                    new String[]{String.valueOf(medication.getId())}
            );
            if (callback != null) callback.onResult(rows > 0);
        });
    }

    public int updateMedication(Medication medication) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.update(
                DatabaseHelper.TABLE_MEDICATIONS,
                medicationToContentValues(medication),
                DatabaseHelper.COL_MED_ID + " = ?",
                new String[]{String.valueOf(medication.getId())}
        );
    }

    /**
     * מחיקת תרופה בthread רקע
     * CASCADE ימחק גם את ה-schedules וה-intake_log המקושרים
     */
    public void deleteMedication(int medicationId, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int rows = db.delete(
                    DatabaseHelper.TABLE_MEDICATIONS,
                    DatabaseHelper.COL_MED_ID + " = ?",
                    new String[]{String.valueOf(medicationId)}
            );
            if (callback != null) callback.onResult(rows > 0);
        });
    }

    public int deleteMedication(int medicationId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                DatabaseHelper.TABLE_MEDICATIONS,
                DatabaseHelper.COL_MED_ID + " = ?",
                new String[]{String.valueOf(medicationId)}
        );
    }

    /**
     * קבלת תרופה לפי id בthread רקע
     */
    public void getMedicationById(int medicationId, OnMedicationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseHelper.TABLE_MEDICATIONS, null,
                        DatabaseHelper.COL_MED_ID + " = ?",
                        new String[]{String.valueOf(medicationId)},
                        null, null, null
                );
                Medication medication = null;
                if (cursor.moveToFirst()) {
                    medication = cursorToMedication(cursor);
                }
                if (callback != null) callback.onResult(medication);
            } finally {
                // try/finally מבטיח סגירת cursor גם אם יש שגיאה
                if (cursor != null) cursor.close();
            }
        });
    }

    public Medication getMedicationById(int medicationId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_MEDICATIONS, null,
                    DatabaseHelper.COL_MED_ID + " = ?",
                    new String[]{String.valueOf(medicationId)},
                    null, null, null
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

    /**
     * קבלת כל התרופות בthread רקע
     */
    public void getAllMedications(OnMedicationListResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseHelper.TABLE_MEDICATIONS, null,
                        null, null, null, null,
                        DatabaseHelper.COL_MED_NAME + " ASC"
                );
                if (callback != null) callback.onResult(cursorToMedicationList(cursor));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public List<Medication> getAllMedications() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_MEDICATIONS, null,
                    null, null, null, null,
                    DatabaseHelper.COL_MED_NAME + " ASC"
            );
            return cursorToMedicationList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * קבלת תרופות פעילות בלבד בthread רקע
     * Used by MedicationListActivity to show only active medications
     */
    public void getActiveMedications(OnMedicationListResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseHelper.TABLE_MEDICATIONS, null,
                        DatabaseHelper.COL_MED_IS_ACTIVE + " = ?",
                        new String[]{"1"},
                        null, null,
                        DatabaseHelper.COL_MED_NAME + " ASC"
                );
                if (callback != null) callback.onResult(cursorToMedicationList(cursor));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public List<Medication> getActiveMedications() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_MEDICATIONS, null,
                    DatabaseHelper.COL_MED_IS_ACTIVE + " = ?",
                    new String[]{"1"},
                    null, null,
                    DatabaseHelper.COL_MED_NAME + " ASC"
            );
            return cursorToMedicationList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // ================================================================
    // SCHEDULES — פעולות על טבלת תזמונים
    // ================================================================

    /**
     * הוספת תזמון חדש בthread רקע
     */
    public void insertSchedule(Schedule schedule, OnInsertResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            long id = db.insert(DatabaseHelper.TABLE_SCHEDULES, null,
                    scheduleToContentValues(schedule));
            if (callback != null) callback.onResult(id);
        });
    }

    public long insertSchedule(Schedule schedule) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(DatabaseHelper.TABLE_SCHEDULES, null,
                scheduleToContentValues(schedule));
    }

    /**
     * קבלת כל התזמונים של תרופה מסוימת בthread רקע
     * Used by BootReceiver to reschedule alarms after reboot
     */
    public void getSchedulesForMedication(int medicationId, OnScheduleListResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseHelper.TABLE_SCHEDULES, null,
                        DatabaseHelper.COL_SCH_MEDICATION_ID + " = ?",
                        new String[]{String.valueOf(medicationId)},
                        null, null,
                        DatabaseHelper.COL_SCH_INTAKE_TIME + " ASC"
                );
                if (callback != null) callback.onResult(cursorToScheduleList(cursor));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public List<Schedule> getSchedulesForMedication(int medicationId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_SCHEDULES, null,
                    DatabaseHelper.COL_SCH_MEDICATION_ID + " = ?",
                    new String[]{String.valueOf(medicationId)},
                    null, null,
                    DatabaseHelper.COL_SCH_INTAKE_TIME + " ASC"
            );
            return cursorToScheduleList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * קבלת כל התזמונים בthread רקע
     * Used by BootReceiver to reschedule ALL alarms after reboot
     */
    public void getAllSchedules(OnScheduleListResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseHelper.TABLE_SCHEDULES, null,
                        null, null, null, null,
                        DatabaseHelper.COL_SCH_INTAKE_TIME + " ASC"
                );
                if (callback != null) callback.onResult(cursorToScheduleList(cursor));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public List<Schedule> getAllSchedules() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_SCHEDULES, null,
                    null, null, null, null,
                    DatabaseHelper.COL_SCH_INTAKE_TIME + " ASC"
            );
            return cursorToScheduleList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * מחיקת כל התזמונים של תרופה בthread רקע
     */
    public void deleteSchedulesForMedication(int medicationId, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int rows = db.delete(
                    DatabaseHelper.TABLE_SCHEDULES,
                    DatabaseHelper.COL_SCH_MEDICATION_ID + " = ?",
                    new String[]{String.valueOf(medicationId)}
            );
            if (callback != null) callback.onResult(rows > 0);
        });
    }

    public int deleteSchedulesForMedication(int medicationId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                DatabaseHelper.TABLE_SCHEDULES,
                DatabaseHelper.COL_SCH_MEDICATION_ID + " = ?",
                new String[]{String.valueOf(medicationId)}
        );
    }

    // ================================================================
    // INTAKE LOG — פעולות על טבלת רישום נטילות
    // ================================================================

    /**
     * הוספת רשומת נטילה חדשה בthread רקע
     */
    public void insertIntakeLog(IntakeLog log, OnInsertResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            long id = db.insert(DatabaseHelper.TABLE_INTAKE_LOG, null,
                    intakeLogToContentValues(log));
            if (callback != null) callback.onResult(id);
        });
    }

    public long insertIntakeLog(IntakeLog log) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(DatabaseHelper.TABLE_INTAKE_LOG, null,
                intakeLogToContentValues(log));
    }

    /**
     * עדכון רשומת נטילה בthread רקע
     */
    public void updateIntakeLog(IntakeLog log, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int rows = db.update(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    intakeLogToContentValues(log),
                    DatabaseHelper.COL_LOG_ID + " = ?",
                    new String[]{String.valueOf(log.getId())}
            );
            if (callback != null) callback.onResult(rows > 0);
        });
    }

    public int updateIntakeLog(IntakeLog log) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.update(
                DatabaseHelper.TABLE_INTAKE_LOG,
                intakeLogToContentValues(log),
                DatabaseHelper.COL_LOG_ID + " = ?",
                new String[]{String.valueOf(log.getId())}
        );
    }

    /**
     * סימון נטילה כ"נלקח" בthread רקע
     * Used when user taps "נלקח" on notification
     */
    public void markAsTaken(int logId, String actualDatetime, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_LOG_TAKEN,           1);
            values.put(DatabaseHelper.COL_LOG_ACTUAL_DATETIME, actualDatetime);
            values.put(DatabaseHelper.COL_LOG_STATUS,          "נלקח");
            int rows = db.update(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    values,
                    DatabaseHelper.COL_LOG_ID + " = ?",
                    new String[]{String.valueOf(logId)}
            );
            if (callback != null) callback.onResult(rows > 0);
        });
    }

    public int markAsTaken(int logId, String actualDatetime) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_LOG_TAKEN, 1);
        values.put(DatabaseHelper.COL_LOG_ACTUAL_DATETIME, actualDatetime);
        values.put(DatabaseHelper.COL_LOG_STATUS, "נלקח");
        return db.update(
                DatabaseHelper.TABLE_INTAKE_LOG,
                values,
                DatabaseHelper.COL_LOG_ID + " = ?",
                new String[]{String.valueOf(logId)}
        );
    }

    /**
     * קבלת לוג של היום בthread רקע
     * Used by MedicationListActivity to show today's intake status
     */
    public void getTodayLogs(OnLogListResult callback) {
        // LocalDate.now() בטוח לשימוש ב-API 26+
        String today = LocalDate.now().toString();
        getLogsByDateRange(today, today, callback);
    }

    public List<IntakeLog> getTodayLogs() {
        String today = LocalDate.now().toString();
        return getLogsByDateRange(today, today);
    }

    /**
     * קבלת לוג לפי טווח תאריכים בthread רקע
     * Used by HistoryActivity filter
     */
    public void getLogsByDateRange(String startDate, String endDate, OnLogListResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                // substr חותך את ה-datetime לתאריך בלבד לצורך השוואה
                cursor = db.query(
                        DatabaseHelper.TABLE_INTAKE_LOG, null,
                        "substr(" + DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + ", 1, 10) BETWEEN ? AND ?",
                        new String[]{startDate, endDate},
                        null, null,
                        DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + " ASC"
                );
                if (callback != null) callback.onResult(cursorToIntakeLogList(cursor));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public List<IntakeLog> getLogsByDateRange(String startDate, String endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_INTAKE_LOG, null,
                    "substr(" + DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + ", 1, 10) BETWEEN ? AND ?",
                    new String[]{startDate, endDate},
                    null, null,
                    DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + " ASC"
            );
            return cursorToIntakeLogList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * קבלת כל הלוג בthread רקע
     * Used by HistoryActivity to show full history
     */
    public void getAllLogs(OnLogListResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseHelper.TABLE_INTAKE_LOG, null,
                        null, null, null, null,
                        DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + " DESC"
                );
                if (callback != null) callback.onResult(cursorToIntakeLogList(cursor));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public List<IntakeLog> getAllLogs() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_INTAKE_LOG, null,
                    null, null, null, null,
                    DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + " DESC"
            );
            return cursorToIntakeLogList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * קבלת לוג לפי שם תרופה בthread רקע
     * Used by HistoryActivity name filter
     */
    public void getLogsByMedicationName(String name, OnLogListResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseHelper.TABLE_INTAKE_LOG, null,
                        DatabaseHelper.COL_LOG_MEDICATION_NAME + " LIKE ?",
                        new String[]{"%" + name + "%"},
                        null, null,
                        DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + " DESC"
                );
                if (callback != null) callback.onResult(cursorToIntakeLogList(cursor));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public List<IntakeLog> getLogsByMedication(int medicationId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_INTAKE_LOG, null,
                    DatabaseHelper.COL_LOG_MEDICATION_ID + " = ?",
                    new String[]{String.valueOf(medicationId)},
                    null, null,
                    DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + " DESC"
            );
            return cursorToIntakeLogList(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // ================================================================
    // פונקציות עזר פרטיות / Private Helper Methods
    // ================================================================

    private ContentValues medicationToContentValues(Medication medication) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_MED_NAME,                    medication.getName());
        values.put(DatabaseHelper.COL_MED_DOSAGE,                  medication.getDosage());
        values.put(DatabaseHelper.COL_MED_INSTRUCTIONS,            medication.getInstructions());
        values.put(DatabaseHelper.COL_MED_IMAGE_PATH,              medication.getImagePath());
        values.put(DatabaseHelper.COL_MED_EXPIRY_DATE,             medication.getExpiryDate());
        values.put(DatabaseHelper.COL_MED_EMERGENCY_CONTACT_NAME,  medication.getEmergencyContactName());
        values.put(DatabaseHelper.COL_MED_EMERGENCY_CONTACT_PHONE, medication.getEmergencyContactPhone());
        values.put(DatabaseHelper.COL_MED_IS_ACTIVE,               medication.isActive() ? 1 : 0);
        return values;
    }

    private ContentValues scheduleToContentValues(Schedule schedule) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SCH_MEDICATION_ID, schedule.getMedicationId());
        values.put(DatabaseHelper.COL_SCH_INTAKE_TIME,   schedule.getIntakeTime());
        values.put(DatabaseHelper.COL_SCH_DAYS_OF_WEEK,  schedule.getDaysOfWeek());
        values.put(DatabaseHelper.COL_SCH_IS_ENABLED,    schedule.isEnabled() ? 1 : 0);
        return values;
    }

    private ContentValues intakeLogToContentValues(IntakeLog log) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_LOG_MEDICATION_ID,      log.getMedicationId());
        values.put(DatabaseHelper.COL_LOG_MEDICATION_NAME,    log.getMedicationName());
        values.put(DatabaseHelper.COL_LOG_SCHEDULED_DATETIME, log.getScheduledDatetime());
        values.put(DatabaseHelper.COL_LOG_TAKEN,              log.isTaken() ? 1 : 0);
        values.put(DatabaseHelper.COL_LOG_ACTUAL_DATETIME,    log.getActualDatetime());
        values.put(DatabaseHelper.COL_LOG_WAS_DELAYED,        log.isWasDelayed() ? 1 : 0);
        values.put(DatabaseHelper.COL_LOG_STATUS,             log.getStatus());
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
        log.setScheduledDatetime(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_SCHEDULED_DATETIME)));
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

    /** מחזיר ערך String או מחרוזת ריקה אם null */
    private String getStringOrEmpty(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        String value = cursor.getString(index);
        return value != null ? value : "";
    }

    /** מחזיר ערך String או null */
    private String getStringOrNull(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getString(index);
    }
}
