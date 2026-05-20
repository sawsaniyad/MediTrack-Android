package com.meditrack.app.database

import android.content.ContentValues
import android.database.Cursor
import com.meditrack.app.models.IntakeLog

/**
 * IntakeLogDAO - פעולות CRUD לטבלת יומן נטילה
 * אחראית: סמירה
 */
class IntakeLogDAO(private val dbHelper: DatabaseHelper) {

    fun insert(log: IntakeLog): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_LOG_MED_ID, log.medicationId)
            put(DatabaseHelper.COL_LOG_SCHEDULED, log.scheduledTime)
            put(DatabaseHelper.COL_LOG_TAKEN, log.takenTime)
            put(DatabaseHelper.COL_LOG_STATUS, log.status.name)
        }
        return db.insert(DatabaseHelper.TABLE_INTAKE_LOG, null, values)
    }

    fun updateStatus(logId: Long, status: IntakeLog.Status, takenTime: Long = 0): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_LOG_STATUS, status.name)
            if (takenTime > 0) put(DatabaseHelper.COL_LOG_TAKEN, takenTime)
        }
        return db.update(
            DatabaseHelper.TABLE_INTAKE_LOG, values,
            "${DatabaseHelper.COL_LOG_ID} = ?",
            arrayOf(logId.toString())
        )
    }

    fun getByMedication(medicationId: Long): List<IntakeLog> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_INTAKE_LOG,
            null,
            "${DatabaseHelper.COL_LOG_MED_ID} = ?",
            arrayOf(medicationId.toString()),
            null, null,
            "${DatabaseHelper.COL_LOG_SCHEDULED} DESC"
        )
        return cursor.use { parseCursor(it) }
    }

    fun getByDateRange(from: Long, to: Long): List<IntakeLog> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_INTAKE_LOG,
            null,
            "${DatabaseHelper.COL_LOG_SCHEDULED} BETWEEN ? AND ?",
            arrayOf(from.toString(), to.toString()),
            null, null,
            "${DatabaseHelper.COL_LOG_SCHEDULED} DESC"
        )
        return cursor.use { parseCursor(it) }
    }

    fun getTodayLogs(): List<IntakeLog> {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val endOfDay = cal.timeInMillis
        return getByDateRange(startOfDay, endOfDay)
    }

    private fun parseCursor(cursor: Cursor): List<IntakeLog> {
        val list = mutableListOf<IntakeLog>()
        while (cursor.moveToNext()) list.add(rowToLog(cursor))
        return list
    }

    private fun rowToLog(cursor: Cursor) = IntakeLog(
        id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_ID)),
        medicationId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_MED_ID)),
        scheduledTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_SCHEDULED)),
        takenTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_TAKEN)),
        status = IntakeLog.Status.valueOf(
            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_STATUS))
        )
    )
}
