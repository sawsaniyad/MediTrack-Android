package com.samiraa_raghadm_sawsana.meditrack.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.samiraa_raghadm_sawsana.meditrack.models.Medication;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DAO לתרופות - כל פעולות CRUD על טבלת Medications
 * Data Access Object for Medications table.
 * All DB operations run on a background thread via ExecutorService.
 * Results are returned to the UI thread via a callback interface.
 */
public class MedicationDAO {

    // -------- Callback Interface --------

    /** ממשק להחזרת תוצאה בודדת */
    public interface OnMedicationResult {
        void onResult(Medication medication);
    }

    /** ממשק להחזרת רשימה */
    public interface OnMedicationListResult {
        void onResult(List<Medication> medications);
    }

    /** ממשק להחזרת מזהה (insert) */
    public interface OnInsertResult {
        void onResult(long insertedId);
    }

    /** ממשק להחזרת הצלחה/כישלון */
    public interface OnOperationResult {
        void onResult(boolean success);
    }

    // -------- שדות / Fields --------
    private final DatabaseHelper dbHelper;
    private final ExecutorService executor;

    // -------- בנאי / Constructor --------
    public MedicationDAO(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        // thread pool עם thread יחיד לפעולות DB סדרתיות
        this.executor = Executors.newSingleThreadExecutor();
    }

    // ================================================================
    // INSERT - הוספת תרופה חדשה
    // ================================================================

    /**
     * מוסיף תרופה חדשה למסד הנתונים בthread רקע
     * Inserts a new medication on a background thread.
     * @param medication התרופה להוספה
     * @param callback   מוחזר עם ה-id שנוצר, או -1 אם נכשל
     */
    public void insert(Medication medication, OnInsertResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = buildContentValues(medication);
            long id = db.insert(DatabaseHelper.TABLE_MEDICATIONS, null, values);
            if (callback != null) {
                // החזרת תוצאה ל-UI thread תתבצע מה-Activity
                callback.onResult(id);
            }
        });
    }

    // ================================================================
    // UPDATE - עדכון תרופה קיימת
    // ================================================================

    /**
     * מעדכן תרופה קיימת בthread רקע
     */
    public void update(Medication medication, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = buildContentValues(medication);
            int rowsAffected = db.update(
                    DatabaseHelper.TABLE_MEDICATIONS,
                    values,
                    DatabaseHelper.COL_MED_ID + " = ?",
                    new String[]{String.valueOf(medication.getId())}
            );
            if (callback != null) {
                callback.onResult(rowsAffected > 0);
            }
        });
    }

    // ================================================================
    // DELETE - מחיקת תרופה
    // ================================================================

    /**
     * מוחק תרופה לפי id בthread רקע
     */
    public void delete(int medicationId, OnOperationResult callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int rowsDeleted = db.delete(
                    DatabaseHelper.TABLE_MEDICATIONS,
                    DatabaseHelper.COL_MED_ID + " = ?",
                    new String[]{String.valueOf(medicationId)}
            );
            if (callback != null) {
                callback.onResult(rowsDeleted > 0);
            }
        });
    }

    // ================================================================
    // GET ALL - קבלת כל התרופות
    // ================================================================

    /**
     * מחזיר את כל התרופות מהמסד בthread רקע
     */
    public void getAll(OnMedicationListResult callback) {
        executor.execute(() -> {
            List<Medication> list = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_MEDICATIONS,
                    null, null, null, null, null,
                    DatabaseHelper.COL_MED_NAME + " ASC"
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    list.add(cursorToMedication(cursor));
                }
                cursor.close();
            }
            if (callback != null) {
                callback.onResult(list);
            }
        });
    }

    // ================================================================
    // GET BY ID - קבלת תרופה לפי מזהה
    // ================================================================

    /**
     * מחזיר תרופה בודדת לפי id בthread רקע
     */
    public void getById(int medicationId, OnMedicationResult callback) {
        executor.execute(() -> {
            Medication medication = null;
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_MEDICATIONS,
                    null,
                    DatabaseHelper.COL_MED_ID + " = ?",
                    new String[]{String.valueOf(medicationId)},
                    null, null, null
            );
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    medication = cursorToMedication(cursor);
                }
                cursor.close();
            }
            if (callback != null) {
                callback.onResult(medication);
            }
        });
    }

    // ================================================================
    // פונקציות עזר פרטיות / Private Helper Methods
    // ================================================================

    /** ממיר אובייקט Medication ל-ContentValues לשמירה ב-DB */
    private ContentValues buildContentValues(Medication medication) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_MED_NAME,       medication.getName());
        values.put(DatabaseHelper.COL_MED_DOSAGE,     medication.getDosage());
        values.put(DatabaseHelper.COL_MED_NOTES,      medication.getNotes());
        values.put(DatabaseHelper.COL_MED_PHOTO_PATH, medication.getPhotoPath());
        values.put(DatabaseHelper.COL_MED_IS_ACTIVE,  medication.isActive() ? 1 : 0);
        return values;
    }

    /** ממיר Cursor לאובייקט Medication */
    private Medication cursorToMedication(Cursor cursor) {
        Medication med = new Medication();
        med.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_ID)));
        med.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_NAME)));
        med.setDosage(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_DOSAGE)));
        med.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_NOTES)));
        med.setPhotoPath(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_PHOTO_PATH)));
        med.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_IS_ACTIVE)) == 1);
        return med;
    }
}