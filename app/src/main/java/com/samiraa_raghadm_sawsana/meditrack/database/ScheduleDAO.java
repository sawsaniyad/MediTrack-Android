package com.samiraa_raghadm_sawsana.meditrack.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.samiraa_raghadm_sawsana.meditrack.models.Schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DAO לתזמונים - כל פעולות CRUD על טבלת schedules
 * Data Access Object for the Schedules table.
 * All operations run on a background thread via ExecutorService.
 *
 * עודכן לשמות שדות חדשים:
 * - intakeTime (במקום time)
 * - daysOfWeek (במקום days)
 * - isEnabled (נשמר)
 *
 * כותבת: סמירה אבו אל-הווא
 */
public class ScheduleDAO {

    // -------- Callback Interfaces --------
    public interface OnScheduleResult {
        void onResult(Schedule schedule);
    }

    public interface OnScheduleListResult {
        void onResult(List<Schedule> schedules);
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
    public ScheduleDAO(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    // ================================================================
    // INSERT - הוספת תזמון חדש
    // ================================================================

    /**
     * מוסיף תזמון חדש בthread רקע
     */
    public void insert(Schedule schedule, OnInsertResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = buildContentValues(schedule);
            long id = db.insert(DatabaseHelper.TABLE_SCHEDULES, null, values);
            if (callback != null) callback.onResult(id);
        });
    }

    // ================================================================
    // UPDATE - עדכון תזמון קיים
    // ================================================================

    /**
     * מעדכן תזמון קיים בthread רקע
     */
    public void update(Schedule schedule, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int rows = db.update(
                    DatabaseHelper.TABLE_SCHEDULES,
                    buildContentValues(schedule),
                    DatabaseHelper.COL_SCH_ID + " = ?",
                    new String[]{String.valueOf(schedule.getId())}
            );
            if (callback != null) callback.onResult(rows > 0);
        });
    }

    // ================================================================
    // DELETE - מחיקת תזמון
    // ================================================================

    /**
     * מוחק תזמון לפי id בthread רקע
     */
    public void delete(int scheduleId, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int rows = db.delete(
                    DatabaseHelper.TABLE_SCHEDULES,
                    DatabaseHelper.COL_SCH_ID + " = ?",
                    new String[]{String.valueOf(scheduleId)}
            );
            if (callback != null) callback.onResult(rows > 0);
        });
    }

    /**
     * מוחק את כל התזמונים של תרופה מסוימת בthread רקע
     * נקרא לפני מחיקת תרופה (CASCADE יטפל אוטומטית אם מוגדר)
     */
    public void deleteByMedicationId(int medicationId, OnOperationResult callback) {
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

    // ================================================================
    // GET ALL - קבלת כל התזמונים
    // ================================================================

    /**
     * מחזיר את כל התזמונים בthread רקע
     * Used by BootReceiver to reschedule ALL alarms after reboot
     */
    public void getAll(OnScheduleListResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseHelper.TABLE_SCHEDULES,
                        null, null, null, null, null,
                        DatabaseHelper.COL_SCH_INTAKE_TIME + " ASC"
                );
                if (callback != null) callback.onResult(cursorToScheduleList(cursor));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    // ================================================================
    // GET BY MEDICATION ID - תזמונים של תרופה מסוימת
    // ================================================================

    /**
     * מחזיר את כל התזמונים של תרופה מסוימת בthread רקע
     * Used by BootReceiver and AddEditMedicationActivity
     */
    public void getByMedicationId(int medicationId, OnScheduleListResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseHelper.TABLE_SCHEDULES,
                        null,
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

    // ================================================================
    // GET ENABLED BY MEDICATION ID - תזמונים פעילים בלבד
    // ================================================================

    /**
     * מחזיר רק תזמונים פעילים של תרופה מסוימת בthread רקע
     * Used by AlarmScheduler to only schedule enabled alarms
     */
    public void getEnabledByMedicationId(int medicationId, OnScheduleListResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseHelper.TABLE_SCHEDULES,
                        null,
                        DatabaseHelper.COL_SCH_MEDICATION_ID + " = ? AND " +
                        DatabaseHelper.COL_SCH_IS_ENABLED + " = 1",
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

    // ================================================================
    // GET BY ID
    // ================================================================

    /**
     * מחזיר תזמון בודד לפי id בthread רקע
     */
    public void getById(int scheduleId, OnScheduleResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseHelper.TABLE_SCHEDULES,
                        null,
                        DatabaseHelper.COL_SCH_ID + " = ?",
                        new String[]{String.valueOf(scheduleId)},
                        null, null, null
                );
                Schedule schedule = null;
                if (cursor.moveToFirst()) {
                    schedule = cursorToSchedule(cursor);
                }
                if (callback != null) callback.onResult(schedule);
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    // ================================================================
    // פונקציות עזר פרטיות / Private Helpers
    // ================================================================

    private ContentValues buildContentValues(Schedule schedule) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SCH_MEDICATION_ID, schedule.getMedicationId());
        values.put(DatabaseHelper.COL_SCH_INTAKE_TIME,   schedule.getIntakeTime());
        values.put(DatabaseHelper.COL_SCH_DAYS_OF_WEEK,  schedule.getDaysOfWeek());
        values.put(DatabaseHelper.COL_SCH_IS_ENABLED,    schedule.isEnabled() ? 1 : 0);
        return values;
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
}
