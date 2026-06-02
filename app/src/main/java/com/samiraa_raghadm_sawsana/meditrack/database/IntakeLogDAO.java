package com.samiraa_raghadm_sawsana.meditrack.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DAO לרישום נטילות - כל פעולות CRUD על טבלת intake_log
 * Data Access Object for the IntakeLog table.
 * All operations run on a background thread via ExecutorService.
 *
 * עודכן לשמות שדות חדשים:
 * - scheduledDatetime (במקום date + scheduledTime)
 * - taken, wasDelayed, actualDatetime (מ-Raghad)
 * - status, medicationName (מ-Samira)
 *
 * כותבת: סמירה אבו אל-הווא
 */
public class IntakeLogDAO {

    // -------- Callback Interfaces --------
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
    public IntakeLogDAO(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    // ================================================================
    // INSERT - הוספת רשומת נטילה חדשה
    // ================================================================

    /**
     * מוסיף רשומת נטילה חדשה בthread רקע
     */
    public void insert(IntakeLog log, OnInsertResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            long id = db.insert(DatabaseHelper.TABLE_INTAKE_LOG, null,
                    buildContentValues(log));
            if (callback != null) callback.onResult(id);
        });
    }

    // ================================================================
    // UPDATE - עדכון רשומת נטילה
    // ================================================================

    /**
     * מעדכן רשומת נטילה קיימת בthread רקע
     */
    public void update(IntakeLog log, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int rows = db.update(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    buildContentValues(log),
                    DatabaseHelper.COL_LOG_ID + " = ?",
                    new String[]{String.valueOf(log.getId())}
            );
            if (callback != null) callback.onResult(rows > 0);
        });
    }

    /**
     * מסמן נטילה כ"נלקח" בthread רקע
     * Used when user taps "נלקח" on notification
     */
    public void markAsTaken(int logId, String actualDatetime, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_LOG_TAKEN,           1);
            values.put(DatabaseHelper.COL_LOG_ACTUAL_DATETIME, actualDatetime);
            values.put(DatabaseHelper.COL_LOG_STATUS,          IntakeLog.STATUS_TAKEN);
            int rows = db.update(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    values,
                    DatabaseHelper.COL_LOG_ID + " = ?",
                    new String[]{String.valueOf(logId)}
            );
            if (callback != null) callback.onResult(rows > 0);
        });
    }

    /**
     * מסמן נטילה כ"נדחה" בthread רקע
     * Used when user taps "דחה" on notification
     */
    public void markAsSnoozed(int logId, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_LOG_WAS_DELAYED, 1);
            values.put(DatabaseHelper.COL_LOG_STATUS,      IntakeLog.STATUS_SNOOZED);
            int rows = db.update(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    values,
                    DatabaseHelper.COL_LOG_ID + " = ?",
                    new String[]{String.valueOf(logId)}
            );
            if (callback != null) callback.onResult(rows > 0);
        });
    }

    /**
     * מסמן נטילה כ"הוחמץ" בthread רקע
     * Called by AlarmReceiver when dose time passes without action
     */
    public void markAsMissed(int logId, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_LOG_STATUS, IntakeLog.STATUS_MISSED);
            int rows = db.update(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    values,
                    DatabaseHelper.COL_LOG_ID + " = ?",
                    new String[]{String.valueOf(logId)}
            );
            if (callback != null) callback.onResult(rows > 0);
        });
    }

    // ================================================================
    // DELETE
    // ================================================================

    /**
     * מוחק רשומה לפי id בthread רקע
     */
    public void delete(int logId, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int rows = db.delete(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    DatabaseHelper.COL_LOG_ID + " = ?",
                    new String[]{String.valueOf(logId)}
            );
            if (callback != null) callback.onResult(rows > 0);
        });
    }

    // ================================================================
    // GET ALL - כל ההיסטוריה
    // ================================================================

    /**
     * מחזיר את כל רשומות הנטילה בthread רקע
     * Used by HistoryActivity
     */
    public void getAll(OnLogListResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseHelper.TABLE_INTAKE_LOG,
                        null, null, null, null, null,
                        DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + " DESC"
                );
                if (callback != null) callback.onResult(cursorToLogList(cursor));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    // ================================================================
    // GET TODAY - נטילות של היום
    // ================================================================

    /**
     * מחזיר נטילות של היום בthread רקע
     * Used by MedicationListActivity to show today's status
     */
    public void getTodayLogs(OnLogListResult callback) {
        // LocalDate.now() בטוח ב-API 26+
        String today = LocalDate.now().toString();
        getByDateRange(today, today, callback);
    }

    // ================================================================
    // GET BY DATE RANGE - סינון לפי טווח תאריכים
    // ================================================================

    /**
     * מחזיר נטילות לפי טווח תאריכים בthread רקע
     * Used by HistoryActivity date filter
     * @param startDate תאריך התחלה בפורמט yyyy-MM-dd
     * @param endDate   תאריך סיום בפורמט yyyy-MM-dd
     */
    public void getByDateRange(String startDate, String endDate, OnLogListResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                // substr חותך את ה-datetime לתאריך בלבד (10 תווים ראשונים)
                cursor = db.query(
                        DatabaseHelper.TABLE_INTAKE_LOG,
                        null,
                        "substr(" + DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + ", 1, 10) BETWEEN ? AND ?",
                        new String[]{startDate, endDate},
                        null, null,
                        DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + " ASC"
                );
                if (callback != null) callback.onResult(cursorToLogList(cursor));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    // ================================================================
    // GET BY MEDICATION NAME - סינון לפי שם תרופה
    // ================================================================

    /**
     * מחזיר נטילות לפי שם תרופה (חיפוש חלקי) בthread רקע
     * Used by HistoryActivity name filter
     */
    public void getByMedicationName(String medicationName, OnLogListResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseHelper.TABLE_INTAKE_LOG,
                        null,
                        DatabaseHelper.COL_LOG_MEDICATION_NAME + " LIKE ?",
                        new String[]{"%" + medicationName + "%"},
                        null, null,
                        DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + " DESC"
                );
                if (callback != null) callback.onResult(cursorToLogList(cursor));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    // ================================================================
    // GET BY DATE AND NAME - סינון משולב
    // ================================================================

    /**
     * מחזיר נטילות לפי תאריך ושם תרופה גם יחד בthread רקע
     * Used by HistoryActivity combined filter
     */
    public void getByDateAndName(String date, String medicationName, OnLogListResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseHelper.TABLE_INTAKE_LOG,
                        null,
                        "substr(" + DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + ", 1, 10) = ? AND " +
                        DatabaseHelper.COL_LOG_MEDICATION_NAME + " LIKE ?",
                        new String[]{date, "%" + medicationName + "%"},
                        null, null,
                        DatabaseHelper.COL_LOG_SCHEDULED_DATETIME + " DESC"
                );
                if (callback != null) callback.onResult(cursorToLogList(cursor));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    // ================================================================
    // פונקציות עזר פרטיות / Private Helpers
    // ================================================================

    private ContentValues buildContentValues(IntakeLog log) {
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

    private IntakeLog cursorToLog(Cursor cursor) {
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

    private List<IntakeLog> cursorToLogList(Cursor cursor) {
        List<IntakeLog> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            list.add(cursorToLog(cursor));
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
