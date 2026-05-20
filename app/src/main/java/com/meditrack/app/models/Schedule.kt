package com.meditrack.app.models

/**
 * מודל לוח זמנים - מתי לקחת תרופה מסוימת
 */
data class Schedule(
    val id: Long = 0,
    val medicationId: Long,   // FK → Medication
    val hour: Int,            // שעה (0-23)
    val minute: Int,          // דקות (0-59)
    val daysOfWeek: String    // "1,2,3,4,5,6,7" = ימים (1=ראשון ... 7=שבת)
)
