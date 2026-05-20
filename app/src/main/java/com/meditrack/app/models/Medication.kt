package com.meditrack.app.models

/**
 * מודל תרופה - מייצג תרופה אחת במסד הנתונים
 */
data class Medication(
    val id: Long = 0,
    val name: String,           // שם התרופה
    val dosage: String,         // מינון (למשל "500mg")
    val notes: String = "",     // הערות נוספות
    val photoPath: String = "", // נתיב לתמונת האריזה
    val contactName: String = "",  // איש קשר לחירום
    val contactPhone: String = "", // טלפון איש קשר
    val isActive: Boolean = true
)
