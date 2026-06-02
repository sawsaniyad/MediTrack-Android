package com.samiraa_raghadm_sawsana.meditrack.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * עוזר מסד הנתונים - אחראי על יצירת הטבלאות ועדכון הסכמה
 * DatabaseHelper: manages SQLite database creation and version management.
 * Extends SQLiteOpenHelper as required by the course.
 *
 * טבלאות: medications, schedules, intake_log
 *
 * כותבת: סמירה אבו אל-הווא
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // -------- קבועי מסד הנתונים / DB Constants --------
    private static final String DB_NAME    = "meditrack.db";
    private static final int    DB_VERSION = 1;

    // -------- שמות טבלאות / Table Names --------
    public static final String TABLE_MEDICATIONS = "medications";
    public static final String TABLE_SCHEDULES   = "schedules";
    public static final String TABLE_INTAKE_LOG  = "intake_log";

    // -------- עמודות טבלת medications --------
    public static final String COL_MED_ID                    = "id";
    public static final String COL_MED_NAME                  = "name";
    public static final String COL_MED_DOSAGE                = "dosage";
    public static final String COL_MED_INSTRUCTIONS          = "instructions";
    public static final String COL_MED_IMAGE_PATH            = "image_path";
    public static final String COL_MED_EXPIRY_DATE           = "expiry_date";
    public static final String COL_MED_EMERGENCY_CONTACT_NAME  = "emergency_contact_name";
    public static final String COL_MED_EMERGENCY_CONTACT_PHONE = "emergency_contact_phone";
    public static final String COL_MED_IS_ACTIVE             = "is_active";

    // -------- עמודות טבלת schedules --------
    public static final String COL_SCH_ID            = "id";
    public static final String COL_SCH_MEDICATION_ID = "medication_id";
    public static final String COL_SCH_INTAKE_TIME   = "intake_time";
    public static final String COL_SCH_DAYS_OF_WEEK  = "days_of_week";
    public static final String COL_SCH_IS_ENABLED    = "is_enabled";

    // -------- עמודות טבלת intake_log --------
    public static final String COL_LOG_ID                 = "id";
    public static final String COL_LOG_MEDICATION_ID      = "medication_id";
    public static final String COL_LOG_MEDICATION_NAME    = "medication_name";
    public static final String COL_LOG_SCHEDULED_DATETIME = "scheduled_datetime";
    public static final String COL_LOG_TAKEN              = "taken";
    public static final String COL_LOG_ACTUAL_DATETIME    = "actual_datetime";
    public static final String COL_LOG_WAS_DELAYED        = "was_delayed";
    public static final String COL_LOG_STATUS             = "status";

    // -------- פקודות יצירת טבלאות / CREATE TABLE Statements --------

    private static final String CREATE_TABLE_MEDICATIONS =
            "CREATE TABLE " + TABLE_MEDICATIONS + " (" +
                    COL_MED_ID                      + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_MED_NAME                    + " TEXT NOT NULL, " +
                    COL_MED_DOSAGE                  + " TEXT, " +
                    COL_MED_INSTRUCTIONS            + " TEXT, " +
                    COL_MED_IMAGE_PATH              + " TEXT, " +
                    COL_MED_EXPIRY_DATE             + " TEXT, " +
                    COL_MED_EMERGENCY_CONTACT_NAME  + " TEXT, " +
                    COL_MED_EMERGENCY_CONTACT_PHONE + " TEXT, " +
                    COL_MED_IS_ACTIVE               + " INTEGER DEFAULT 1" +
                    ")";

    private static final String CREATE_TABLE_SCHEDULES =
            "CREATE TABLE " + TABLE_SCHEDULES + " (" +
                    COL_SCH_ID            + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_SCH_MEDICATION_ID + " INTEGER NOT NULL, " +
                    COL_SCH_INTAKE_TIME   + " TEXT NOT NULL, " +
                    COL_SCH_DAYS_OF_WEEK  + " TEXT NOT NULL, " +
                    COL_SCH_IS_ENABLED    + " INTEGER DEFAULT 1, " +
                    "FOREIGN KEY(" + COL_SCH_MEDICATION_ID + ") REFERENCES " +
                    TABLE_MEDICATIONS + "(" + COL_MED_ID + ") ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_TABLE_INTAKE_LOG =
            "CREATE TABLE " + TABLE_INTAKE_LOG + " (" +
                    COL_LOG_ID                 + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_LOG_MEDICATION_ID      + " INTEGER NOT NULL, " +
                    COL_LOG_MEDICATION_NAME    + " TEXT, " +
                    COL_LOG_SCHEDULED_DATETIME + " TEXT NOT NULL, " +
                    COL_LOG_TAKEN              + " INTEGER DEFAULT 0, " +
                    COL_LOG_ACTUAL_DATETIME    + " TEXT, " +
                    COL_LOG_WAS_DELAYED        + " INTEGER DEFAULT 0, " +
                    COL_LOG_STATUS             + " TEXT DEFAULT 'ממתין', " +
                    "FOREIGN KEY(" + COL_LOG_MEDICATION_ID + ") REFERENCES " +
                    TABLE_MEDICATIONS + "(" + COL_MED_ID + ") ON DELETE CASCADE" +
                    ")";

    // -------- Singleton Instance --------
    // double-checked locking — thread safe
    private static volatile DatabaseHelper instance;

    /**
     * Singleton — מונע יצירת מספר חיבורים במקביל
     * Returns single shared instance of DatabaseHelper.
     */
    public static DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // בנאי פרטי / Private constructor
    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * מפעיל Foreign Keys אוטומטית בכל פתיחה
     * Using onConfigure (Raghad's approach) — cleaner than PRAGMA in onCreate
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * נקרא בפעם הראשונה שמסד הנתונים נוצר
     * Called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MEDICATIONS);
        db.execSQL(CREATE_TABLE_SCHEDULES);
        db.execSQL(CREATE_TABLE_INTAKE_LOG);
    }

    /**
     * נקרא כשגרסת מסד הנתונים עולה
     * Called when DB version is bumped — drops and recreates all tables.
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
     * מנקה את כל הנתונים ללא מחיקת הסכמה
     * Debug helper — clears all data without dropping schema.
     * שימושי לבדיקות בזמן פיתוח
     */
    public void resetDatabase() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_INTAKE_LOG);
        db.execSQL("DELETE FROM " + TABLE_SCHEDULES);
        db.execSQL("DELETE FROM " + TABLE_MEDICATIONS);
    }
}
