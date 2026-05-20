package com.meditrack.app.database

import android.content.ContentValues
import android.database.Cursor
import com.meditrack.app.models.Medication

/**
 * MedicationDAO - פעולות CRUD לטבלת תרופות
 * אחראית: סמירה
 */
class MedicationDAO(private val dbHelper: DatabaseHelper) {

    fun insert(med: Medication): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_MED_NAME, med.name)
            put(DatabaseHelper.COL_MED_DOSAGE, med.dosage)
            put(DatabaseHelper.COL_MED_NOTES, med.notes)
            put(DatabaseHelper.COL_MED_PHOTO, med.photoPath)
            put(DatabaseHelper.COL_MED_CONTACT_NAME, med.contactName)
            put(DatabaseHelper.COL_MED_CONTACT_PHONE, med.contactPhone)
            put(DatabaseHelper.COL_MED_ACTIVE, if (med.isActive) 1 else 0)
        }
        return db.insert(DatabaseHelper.TABLE_MEDICATIONS, null, values)
    }

    fun update(med: Medication): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_MED_NAME, med.name)
            put(DatabaseHelper.COL_MED_DOSAGE, med.dosage)
            put(DatabaseHelper.COL_MED_NOTES, med.notes)
            put(DatabaseHelper.COL_MED_PHOTO, med.photoPath)
            put(DatabaseHelper.COL_MED_CONTACT_NAME, med.contactName)
            put(DatabaseHelper.COL_MED_CONTACT_PHONE, med.contactPhone)
            put(DatabaseHelper.COL_MED_ACTIVE, if (med.isActive) 1 else 0)
        }
        return db.update(
            DatabaseHelper.TABLE_MEDICATIONS,
            values,
            "${DatabaseHelper.COL_MED_ID} = ?",
            arrayOf(med.id.toString())
        )
    }

    fun delete(id: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(
            DatabaseHelper.TABLE_MEDICATIONS,
            "${DatabaseHelper.COL_MED_ID} = ?",
            arrayOf(id.toString())
        )
    }

    fun getAll(): List<Medication> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_MEDICATIONS,
            null, null, null, null, null,
            "${DatabaseHelper.COL_MED_NAME} ASC"
        )
        return cursor.use { parseCursor(it) }
    }

    fun getActive(): List<Medication> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_MEDICATIONS,
            null,
            "${DatabaseHelper.COL_MED_ACTIVE} = 1",
            null, null, null,
            "${DatabaseHelper.COL_MED_NAME} ASC"
        )
        return cursor.use { parseCursor(it) }
    }

    fun getById(id: Long): Medication? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_MEDICATIONS,
            null,
            "${DatabaseHelper.COL_MED_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use { if (it.moveToFirst()) rowToMedication(it) else null }
    }

    private fun parseCursor(cursor: Cursor): List<Medication> {
        val list = mutableListOf<Medication>()
        while (cursor.moveToNext()) list.add(rowToMedication(cursor))
        return list
    }

    private fun rowToMedication(cursor: Cursor) = Medication(
        id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_ID)),
        name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_NAME)),
        dosage = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_DOSAGE)),
        notes = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_NOTES)),
        photoPath = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_PHOTO)),
        contactName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_CONTACT_NAME)),
        contactPhone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_CONTACT_PHONE)),
        isActive = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_ACTIVE)) == 1
    )
}
