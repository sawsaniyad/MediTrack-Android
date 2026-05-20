package com.meditrack.app.database

import android.content.ContentValues
import android.database.Cursor
import com.meditrack.app.models.Schedule

/**
 * ScheduleDAO - פעולות CRUD לטבלת לוחות זמנים
 * אחראית: סמירה
 */
class ScheduleDAO(private val dbHelper: DatabaseHelper) {

    fun insert(schedule: Schedule): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_SCH_MED_ID, schedule.medicationId)
            put(DatabaseHelper.COL_SCH_HOUR, schedule.hour)
            put(DatabaseHelper.COL_SCH_MINUTE, schedule.minute)
            put(DatabaseHelper.COL_SCH_DAYS, schedule.daysOfWeek)
        }
        return db.insert(DatabaseHelper.TABLE_SCHEDULES, null, values)
    }

    fun update(schedule: Schedule): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_SCH_HOUR, schedule.hour)
            put(DatabaseHelper.COL_SCH_MINUTE, schedule.minute)
            put(DatabaseHelper.COL_SCH_DAYS, schedule.daysOfWeek)
        }
        return db.update(
            DatabaseHelper.TABLE_SCHEDULES, values,
            "${DatabaseHelper.COL_SCH_ID} = ?",
            arrayOf(schedule.id.toString())
        )
    }

    fun deleteByMedication(medicationId: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(
            DatabaseHelper.TABLE_SCHEDULES,
            "${DatabaseHelper.COL_SCH_MED_ID} = ?",
            arrayOf(medicationId.toString())
        )
    }

    fun getByMedication(medicationId: Long): List<Schedule> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_SCHEDULES,
            null,
            "${DatabaseHelper.COL_SCH_MED_ID} = ?",
            arrayOf(medicationId.toString()),
            null, null, null
        )
        return cursor.use { parseCursor(it) }
    }

    fun getAll(): List<Schedule> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_SCHEDULES, null, null, null, null, null, null)
        return cursor.use { parseCursor(it) }
    }

    private fun parseCursor(cursor: Cursor): List<Schedule> {
        val list = mutableListOf<Schedule>()
        while (cursor.moveToNext()) list.add(rowToSchedule(cursor))
        return list
    }

    private fun rowToSchedule(cursor: Cursor) = Schedule(
        id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_ID)),
        medicationId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_MED_ID)),
        hour = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_HOUR)),
        minute = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_MINUTE)),
        daysOfWeek = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCH_DAYS))
    )
}
