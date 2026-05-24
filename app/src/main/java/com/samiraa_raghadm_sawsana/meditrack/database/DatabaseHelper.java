package com.samiraa_raghadm_sawsana.meditrack.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * עוזר מסד הנתונים - אחראי על יצירת הטבלאות ועדכון הסכמה
 * DatabaseHelper: manages SQLite database creation and version management.
 * Extends SQLiteOpenHelper as required by the course.
 *
 * טבלאות: Medications, Schedules, IntakeLog
 * Tables: Medications, Schedules, IntakeLog
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // -------- קבועי מסד הנתונים / DB Constants --------
    private static final String DB_NAME    = "meditrack.db";
    private static final int    DB_VERSION = 1;

    // -------- שמות טבלאות / Table Names --------
    public static final String TABLE_MEDICATIONS = "Medications";
    public static final String TABLE_SCHEDULES   = "Schedules";
    public static final String TABLE_INTAKE_LOG  = "IntakeLog";

    // -------- עמודות טבלת Medications --------
    public static final String COL_MED_ID         = "id";
    public static final String COL_MED_NAME       = "name";
    public static final String COL_MED_DOSAGE     = "dosage";
    public static final String COL_MED_NOTES      = "notes";
    public static final String COL_MED_PHOTO_PATH = "photo_path";
    public static final String COL_MED_IS_ACTIVE  = "is_active";

    // -------- עמודות טבלת Schedules --------
    public static final String COL_SCH_ID            = "id";
    public static final String COL_SCH_MEDICATION_ID = "medication_id";
    public static final String COL_SCH_TIME          = "time";
    public static final String COL_SCH_DAYS          = "days";
    public static final String COL_SCH_IS_ENABLED    = "is_enabled";

    // -------- עמודות טבלת IntakeLog --------
    public static final String COL_LOG_ID              = "id";
    public static final String COL_LOG_MEDICATION_ID   = "medication_id";
    public static final String COL_LOG_SCHEDULE_ID     = "schedule_id";
    public static final String COL_LOG_MEDICATION_NAME = "medication_name";
    public static final String COL_LOG_SCHEDULED_TIME  = "scheduled_time";
    public static final String COL_LOG_DATE            = "date";
    public static final String COL_LOG_STATUS          = "status";
    public static final String COL_LOG_TIMESTAMP       = "timestamp";

    // -------- פקודות יצירת טבלאות / CREATE TABLE Statements --------

    private static final String CREATE_TABLE_MEDICATIONS =
            "CREATE TABLE " + TABLE_MEDICATIONS + " (" +
                    COL_MED_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_MED_NAME       + " TEXT NOT NULL, " +
                    COL_MED_DOSAGE     + " TEXT, " +
                    COL_MED_NOTES      + " TEXT, " +
                    COL_MED_PHOTO_PATH + " TEXT, " +
                    COL_MED_IS_ACTIVE  + " INTEGER DEFAULT 1" +
                    ");";

    private static final String CREATE_TABLE_SCHEDULES =
            "CREATE TABLE " + TABLE_SCHEDULES + " (" +
                    COL_SCH_ID            + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_SCH_MEDICATION_ID + " INTEGER NOT NULL, " +
                    COL_SCH_TIME          + " TEXT NOT NULL, " +
                    COL_SCH_DAYS          + " TEXT, " +
                    COL_SCH_IS_ENABLED    + " INTEGER DEFAULT 1, " +
                    "FOREIGN KEY(" + COL_SCH_MEDICATION_ID + ") REFERENCES " +
                    TABLE_MEDICATIONS + "(" + COL_MED_ID + ") ON DELETE CASCADE" +
                    ");";

    private static final String CREATE_TABLE_INTAKE_LOG =
            "CREATE TABLE " + TABLE_INTAKE_LOG + " (" +
                    COL_LOG_ID              + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_LOG_MEDICATION_ID   + " INTEGER NOT NULL, " +
                    COL_LOG_SCHEDULE_ID     + " INTEGER, " +
                    COL_LOG_MEDICATION_NAME + " TEXT, " +
                    COL_LOG_SCHEDULED_TIME  + " TEXT, " +
                    COL_LOG_DATE            + " TEXT NOT NULL, " +
                    COL_LOG_STATUS          + " TEXT NOT NULL, " +
                    COL_LOG_TIMESTAMP       + " INTEGER" +
                    ");";

    // -------- Singleton Instance --------
    private static DatabaseHelper instance;

    /**
     * Singleton pattern - מונע יצירת מספר חיבורים במקביל
     * Returns a single shared instance of DatabaseHelper.
     */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    // בנאי פרטי / Private constructor
    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * נקרא בפעם הראשונה שמסד הנתונים נוצר
     * Called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // הפעלת CASCADE Deletes
        db.execSQL("PRAGMA foreign_keys = ON;");

        // יצירת הטבלאות
        db.execSQL(CREATE_TABLE_MEDICATIONS);
        db.execSQL(CREATE_TABLE_SCHEDULES);
        db.execSQL(CREATE_TABLE_INTAKE_LOG);
    }

    /**
     * נקרא כשגרסת מסד הנתונים עולה
     * Called when DB version is bumped — drop and recreate all tables.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // מחיקת טבלאות קיימות ויצירה מחדש
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INTAKE_LOG);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICATIONS);
        onCreate(db);
    }

    /**
     * מאפשר Foreign Keys בכל פתיחה של מסד הנתונים
     */
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys = ON;");
        }
    }
}