package com.meditrack.app.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * DatabaseHelper - מנהל יצירת ועדכון מסד הנתונים SQLite
 * אחראית: סמירה
 */
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "meditrack.db"
        const val DATABASE_VERSION = 1

        // טבלת תרופות
        const val TABLE_MEDICATIONS = "medications"
        const val COL_MED_ID = "_id"
        const val COL_MED_NAME = "name"
        const val COL_MED_DOSAGE = "dosage"
        const val COL_MED_NOTES = "notes"
        const val COL_MED_PHOTO = "photo_path"
        const val COL_MED_CONTACT_NAME = "contact_name"
        const val COL_MED_CONTACT_PHONE = "contact_phone"
        const val COL_MED_ACTIVE = "is_active"

        // טבלת לוחות זמנים
        const val TABLE_SCHEDULES = "schedules"
        const val COL_SCH_ID = "_id"
        const val COL_SCH_MED_ID = "medication_id"
        const val COL_SCH_HOUR = "hour"
        const val COL_SCH_MINUTE = "minute"
        const val COL_SCH_DAYS = "days_of_week"

        // טבלת יומן נטילה
        const val TABLE_INTAKE_LOG = "intake_log"
        const val COL_LOG_ID = "_id"
        const val COL_LOG_MED_ID = "medication_id"
        const val COL_LOG_SCHEDULED = "scheduled_time"
        const val COL_LOG_TAKEN = "taken_time"
        const val COL_LOG_STATUS = "status"

        // SQL ליצירת טבלאות
        private const val CREATE_MEDICATIONS = """
            CREATE TABLE $TABLE_MEDICATIONS (
                $COL_MED_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_MED_NAME TEXT NOT NULL,
                $COL_MED_DOSAGE TEXT NOT NULL,
                $COL_MED_NOTES TEXT DEFAULT '',
                $COL_MED_PHOTO TEXT DEFAULT '',
                $COL_MED_CONTACT_NAME TEXT DEFAULT '',
                $COL_MED_CONTACT_PHONE TEXT DEFAULT '',
                $COL_MED_ACTIVE INTEGER DEFAULT 1
            )
        """

        private const val CREATE_SCHEDULES = """
            CREATE TABLE $TABLE_SCHEDULES (
                $COL_SCH_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SCH_MED_ID INTEGER NOT NULL,
                $COL_SCH_HOUR INTEGER NOT NULL,
                $COL_SCH_MINUTE INTEGER NOT NULL,
                $COL_SCH_DAYS TEXT NOT NULL,
                FOREIGN KEY($COL_SCH_MED_ID) REFERENCES $TABLE_MEDICATIONS($COL_MED_ID) ON DELETE CASCADE
            )
        """

        private const val CREATE_INTAKE_LOG = """
            CREATE TABLE $TABLE_INTAKE_LOG (
                $COL_LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LOG_MED_ID INTEGER NOT NULL,
                $COL_LOG_SCHEDULED INTEGER NOT NULL,
                $COL_LOG_TAKEN INTEGER DEFAULT 0,
                $COL_LOG_STATUS TEXT DEFAULT 'PENDING',
                FOREIGN KEY($COL_LOG_MED_ID) REFERENCES $TABLE_MEDICATIONS($COL_MED_ID) ON DELETE CASCADE
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_MEDICATIONS)
        db.execSQL(CREATE_SCHEDULES)
        db.execSQL(CREATE_INTAKE_LOG)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // בגרסה עתידית: ALTER TABLE במקום DROP
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INTAKE_LOG")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SCHEDULES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEDICATIONS")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }
}
