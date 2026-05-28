package com.meditrack.app.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "meditrack.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_MEDICATIONS = "medications";
    public static final String TABLE_SCHEDULES = "schedules";
    public static final String TABLE_INTAKE_LOG = "intake_log";

    private static volatile DatabaseHelper instance;

    private static final String CREATE_TABLE_MEDICATIONS =
            "CREATE TABLE medications (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "dosage TEXT, " +
                    "instructions TEXT, " +
                    "image_path TEXT, " +
                    "expiry_date TEXT, " +
                    "emergency_contact_name TEXT, " +
                    "emergency_contact_phone TEXT, " +
                    "is_active INTEGER DEFAULT 1" +
                    ")";

    private static final String CREATE_TABLE_SCHEDULES =
            "CREATE TABLE schedules (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "medication_id INTEGER NOT NULL, " +
                    "intake_time TEXT NOT NULL, " +
                    "days_of_week TEXT NOT NULL, " +
                    "FOREIGN KEY(medication_id) REFERENCES medications(id) ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_TABLE_INTAKE_LOG =
            "CREATE TABLE intake_log (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "medication_id INTEGER NOT NULL, " +
                    "scheduled_datetime TEXT NOT NULL, " +
                    "taken INTEGER DEFAULT 0, " +
                    "actual_datetime TEXT, " +
                    "was_delayed INTEGER DEFAULT 0, " +
                    "FOREIGN KEY(medication_id) REFERENCES medications(id) ON DELETE CASCADE" +
                    ")";

    private DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    public static DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MEDICATIONS);
        db.execSQL(CREATE_TABLE_SCHEDULES);
        db.execSQL(CREATE_TABLE_INTAKE_LOG);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INTAKE_LOG);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICATIONS);
        onCreate(db);
    }

    /** Debug helper — clears all data without dropping schema. */
    public void resetDatabase() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_INTAKE_LOG);
        db.execSQL("DELETE FROM " + TABLE_SCHEDULES);
        db.execSQL("DELETE FROM " + TABLE_MEDICATIONS);
    }
}
