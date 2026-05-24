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
 * DAO לתזמונים - כל פעולות CRUD על טבלת Schedules
 * Data Access Object for the Schedules table.
 * All operations run on a background thread via ExecutorService.
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
    // INSERT
    // ================================================================

    /**
     * מוסיף תזמון חדש בthread רקע
     */
    public void insert(Schedule schedule, OnInsertResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = buildContentValues(schedule);
            long id = db.insert(DatabaseHelper.TABLE_SCHEDULES, null, values);
            if (callback != null) {
                callback.onResult(id);
            }
        });
    }

    // ================================================================
    // UPDATE
    // ================================================================

    /**
     * מעדכן תזמון קיים בthread רקע
     */
    public void update(Schedule schedule, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = buildContentValues(schedule);
            int rowsAffected = db.update(
                    DatabaseHelper.TABLE_SCHEDULES,
                    values,
                    DatabaseHelper.COL_SCH_ID + " = ?",
                    new String[]{String.valueOf(schedule.getId())}
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
     * מוחק תזמון לפי id בthread רקע
     */
    public void delete(int scheduleId, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int rowsDeleted = db.delete(
                    DatabaseHelper.TABLE_SCHEDULES,
                    DatabaseHelper.COL_SCH_ID + " = ?",
                    new String[]{String.valueOf(scheduleId)}
            );
            if (callback != null) {
                callback.onResult(rowsDeleted > 0);
            }
        });
    }

    /**
     * מוחק את כל התזמונים של תרופה מסוימת בthread רקע
     */
    public void deleteByMedicationId(int medicationId, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int rowsDeleted = db.delete(
                    DatabaseHelper.TABLE_SCHEDULES,
                    DatabaseHelper.COL_SCH_MEDICATION_ID + " = ?",
                    new String[]{String.valueOf(medicationId)}
            );
            if (callback != null) {
                callback.onResult(rowsDeleted > 0);
            }
        });
    }

    // ================================================================
    // GET ALL
    // ================================================================

    /**
     * מחזיר את כל התזמונים בthread רקע
     */
    public void getAll(OnScheduleListResult callback) {
        executor.execute(() -> {
            List<Schedule> list = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_SCHEDULES,
                    null, null, null, null, null, null
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    list.add(cursorToSchedule(cursor));
                }
                cursor.close();
            }
            if (callback != null) {
                callback.onResult(list);
            }
        });
    }

    // ================================================================
    // GET BY MEDICATION ID - תזמונים של תרופה מסוימת
    // ================================================================

    /**
     * מחזיר את כל התזמונים של תרופה מסוימת בthread רקע
     * Used by BootReceiver to reschedule alarms after reboot.
     */
    public void getByMedicationId(int medicationId, OnScheduleListResult callback) {
        executor.execute(() -> {
            List<Schedule> list = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_SCHEDULES,
                    null,
                    DatabaseHelper.COL_SCH_MEDICATION_ID + " = ?",
                    new String[]{String.valueOf(medicationId)},
                    null, null, null
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    list.add(cursorToSchedule(cursor));
                }
                cursor.close();
            }
            if (callback != null) {
                callback.onResult(list);
            }
        });
    }

    // ================================================================
    // GET BY ID
    // ================================================================

    public void getById(int scheduleId, OnScheduleResult callback) {
        executor.execute(() -> {
            Schedule schedule = null;
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_SCHEDULES,
                    null,
                    DatabaseHelper.COL_SCH_ID + " = ?",
                    new String[]{String.valueOf(scheduleId)},
                    null, null, null
            );
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    schedule = cursorToSchedule(cursor);
                }
                cursor.close();
            }
            if (callback != null) {
                callback.onResult(schedule);
            }
        });
    }

    // ================================================================
    // פונקציות עזר פרטיות / Private Helpers
    // ================================================================

    private ContentValues buildContentValues(Schedule schedule) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SCH_MEDICATION_ID, schedule.getMedicationId());
        values.put(DatabaseHelper.COL_SCH_TIME,          schedule.getTime());
        values.put(DatabaseHelper.COL_SCH_DAYS,          schedule.getDays());
        values.put(DatabaseHelper.COL_SCH_IS_ENABLED,    schedule.isEnabled() ? 1 : 0);
        return values;
    }

    private Schedule cursorToSchedule(Cursor cursor) {
        Schedule schedule = new Schedule();
        schedule.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_ID)));
        schedule.setMedicationId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_MEDICATION_ID)));
        schedule.setTime(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_TIME)));
        schedule.setDays(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_DAYS)));
        schedule.setEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_IS_ENABLED)) == 1);
        return schedule;
    }
}