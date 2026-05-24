package com.samiraa_raghadm_sawsana.meditrack.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DAO לרישום נטילות - כל פעולות CRUD על טבלת IntakeLog
 * Data Access Object for the IntakeLog table.
 * All operations run on a background thread via ExecutorService.
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
            ContentValues values = buildContentValues(log);
            long id = db.insert(DatabaseHelper.TABLE_INTAKE_LOG, null, values);
            if (callback != null) {
                callback.onResult(id);
            }
        });
    }

    // ================================================================
    // UPDATE - עדכון סטטוס נטילה
    // ================================================================

    /**
     * מעדכן סטטוס רשומת נטילה בthread רקע
     * Used when user taps "Taken" or "Snooze" on a notification.
     */
    public void updateStatus(int logId, String newStatus, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_LOG_STATUS,    newStatus);
            values.put(DatabaseHelper.COL_LOG_TIMESTAMP, System.currentTimeMillis());
            int rowsAffected = db.update(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    values,
                    DatabaseHelper.COL_LOG_ID + " = ?",
                    new String[]{String.valueOf(logId)}
            );
            if (callback != null) {
                callback.onResult(rowsAffected > 0);
            }
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
            int rowsDeleted = db.delete(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    DatabaseHelper.COL_LOG_ID + " = ?",
                    new String[]{String.valueOf(logId)}
            );
            if (callback != null) {
                callback.onResult(rowsDeleted > 0);
            }
        });
    }

    // ================================================================
    // GET ALL - כל ההיסטוריה
    // ================================================================

    /**
     * מחזיר את כל רשומות הנטילה, ממוינות לפי תאריך (חדש ראשון)
     * Used by HistoryActivity to show full intake history.
     */
    public void getAll(OnLogListResult callback) {
        executor.execute(() -> {
            List<IntakeLog> list = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    null, null, null, null, null,
                    DatabaseHelper.COL_LOG_DATE + " DESC, " + DatabaseHelper.COL_LOG_TIMESTAMP + " DESC"
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    list.add(cursorToIntakeLog(cursor));
                }
                cursor.close();
            }
            if (callback != null) {
                callback.onResult(list);
            }
        });
    }

    // ================================================================
    // GET BY DATE - סינון לפי תאריך
    // ================================================================

    /**
     * מחזיר רשומות נטילה לפי תאריך מסוים בthread רקע
     * @param date תאריך בפורמט yyyy-MM-dd
     */
    public void getByDate(String date, OnLogListResult callback) {
        executor.execute(() -> {
            List<IntakeLog> list = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    null,
                    DatabaseHelper.COL_LOG_DATE + " = ?",
                    new String[]{date},
                    null, null,
                    DatabaseHelper.COL_LOG_TIMESTAMP + " DESC"
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    list.add(cursorToIntakeLog(cursor));
                }
                cursor.close();
            }
            if (callback != null) {
                callback.onResult(list);
            }
        });
    }

    // ================================================================
    // GET BY MEDICATION NAME - סינון לפי שם תרופה
    // ================================================================

    /**
     * מחזיר רשומות נטילה לפי שם תרופה (חיפוש חלקי) בthread רקע
     */
    public void getByMedicationName(String medicationName, OnLogListResult callback) {
        executor.execute(() -> {
            List<IntakeLog> list = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    null,
                    DatabaseHelper.COL_LOG_MEDICATION_NAME + " LIKE ?",
                    new String[]{"%" + medicationName + "%"},
                    null, null,
                    DatabaseHelper.COL_LOG_DATE + " DESC"
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    list.add(cursorToIntakeLog(cursor));
                }
                cursor.close();
            }
            if (callback != null) {
                callback.onResult(list);
            }
        });
    }

    // ================================================================
    // GET BY DATE AND MEDICATION NAME - סינון משולב
    // ================================================================

    /**
     * מחזיר רשומות לפי תאריך ושם תרופה גם יחד בthread רקע
     */
    public void getByDateAndName(String date, String medicationName, OnLogListResult callback) {
        executor.execute(() -> {
            List<IntakeLog> list = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_INTAKE_LOG,
                    null,
                    DatabaseHelper.COL_LOG_DATE + " = ? AND " +
                            DatabaseHelper.COL_LOG_MEDICATION_NAME + " LIKE ?",
                    new String[]{date, "%" + medicationName + "%"},
                    null, null,
                    DatabaseHelper.COL_LOG_TIMESTAMP + " DESC"
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    list.add(cursorToIntakeLog(cursor));
                }
                cursor.close();
            }
            if (callback != null) {
                callback.onResult(list);
            }
        });
    }

    // ================================================================
    // פונקציות עזר פרטיות / Private Helpers
    // ================================================================

    private ContentValues buildContentValues(IntakeLog log) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_LOG_MEDICATION_ID,   log.getMedicationId());
        values.put(DatabaseHelper.COL_LOG_SCHEDULE_ID,     log.getScheduleId());
        values.put(DatabaseHelper.COL_LOG_MEDICATION_NAME, log.getMedicationName());
        values.put(DatabaseHelper.COL_LOG_SCHEDULED_TIME,  log.getScheduledTime());
        values.put(DatabaseHelper.COL_LOG_DATE,            log.getDate());
        values.put(DatabaseHelper.COL_LOG_STATUS,          log.getStatus());
        values.put(DatabaseHelper.COL_LOG_TIMESTAMP,       log.getTimestamp());
        return values;
    }

    private IntakeLog cursorToIntakeLog(Cursor cursor) {
        IntakeLog log = new IntakeLog();
        log.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_ID)));
        log.setMedicationId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_MEDICATION_ID)));
        log.setScheduleId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_SCHEDULE_ID)));
        log.setMedicationName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_MEDICATION_NAME)));
        log.setScheduledTime(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_SCHEDULED_TIME)));
        log.setDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_DATE)));
        log.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_STATUS)));
        log.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_TIMESTAMP)));
        return log;
    }
}