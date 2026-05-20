package com.meditrack.app.models

/**
 * מודל יומן נטילה - כל פעם שמשתמש לקח תרופה
 */
data class IntakeLog(
    val id: Long = 0,
    val medicationId: Long,
    val medicationName: String = "", // cached for display
    val scheduledTime: Long,  // timestamp מתוכנן
    val takenTime: Long = 0,  // timestamp בפועל (0 = לא נלקח)
    val status: Status = Status.PENDING
) {
    enum class Status { PENDING, TAKEN, SKIPPED, SNOOZED }
}
