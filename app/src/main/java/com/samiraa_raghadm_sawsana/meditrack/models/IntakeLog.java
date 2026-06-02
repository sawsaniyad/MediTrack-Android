package com.samiraa_raghadm_sawsana.meditrack.models;

/**
 * מחלקת מודל לרישום נטילת תרופה
 * Model class representing a single intake log entry.
 *
 * מיזוג שתי גרסאות:
 * מ-Samira:
 * - status constants (נלקח/הוחמץ/נדחה/ממתין) — נחוצים לתצוגה ולסינון
 * - medicationName — לנוחות תצוגה בלי JOIN
 *
 * מ-Raghad:
 * - wasDelayed — האם נדחה
 * - taken boolean — האם נלקח
 * - actualDatetime — מתי בפועל נלקח
 * - scheduledDatetime — מחרוזת datetime מלאה (יותר גמיש מתאריך בלבד)
 *
 * כותבת: סמירה אבו אל-הווא
 */
public class IntakeLog {

    // -------- סטטוסים אפשריים / Status Constants --------
    public static final String STATUS_TAKEN   = "נלקח";
    public static final String STATUS_MISSED  = "הוחמץ";
    public static final String STATUS_SNOOZED = "נדחה";
    public static final String STATUS_PENDING = "ממתין";

    // -------- שדות פרטיים / Private Fields --------
    private int id;
    private int medicationId;           // מזהה התרופה
    private String medicationName;      // שם התרופה — לנוחות תצוגה ללא JOIN
    private String scheduledDatetime;   // תאריך ושעה מתוזמנים בפורמט yyyy-MM-dd HH:mm
    private boolean taken;              // האם נלקח
    private String actualDatetime;      // תאריך ושעה בפועל של הנטילה
    private boolean wasDelayed;         // האם נדחה
    private String status;              // סטטוס: נלקח / הוחמץ / נדחה / ממתין

    // -------- בנאי ריק / Default Constructor --------
    public IntakeLog() {
        this.status = STATUS_PENDING; // ברירת מחדל — ממתין
    }

    // -------- בנאי מלא / Full Constructor (טעינה מ-DB) --------
    public IntakeLog(int id, int medicationId, String medicationName,
                     String scheduledDatetime, boolean taken,
                     String actualDatetime, boolean wasDelayed, String status) {
        this.id = id;
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.scheduledDatetime = scheduledDatetime;
        this.taken = taken;
        this.actualDatetime = actualDatetime;
        this.wasDelayed = wasDelayed;
        this.status = status;
    }

    // -------- בנאי ללא id / Constructor without id (הוספה חדשה) --------
    public IntakeLog(int medicationId, String medicationName,
                     String scheduledDatetime, String status) {
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.scheduledDatetime = scheduledDatetime;
        this.taken = false;
        this.wasDelayed = false;
        this.status = status;
    }

    // -------- Getters --------
    public int getId() { return id; }
    public int getMedicationId() { return medicationId; }
    public String getMedicationName() { return medicationName; }
    public String getScheduledDatetime() { return scheduledDatetime; }
    public boolean isTaken() { return taken; }
    public String getActualDatetime() { return actualDatetime; }
    public boolean isWasDelayed() { return wasDelayed; }
    public String getStatus() { return status; }

    // -------- Setters --------
    public void setId(int id) { this.id = id; }
    public void setMedicationId(int medicationId) { this.medicationId = medicationId; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    public void setScheduledDatetime(String scheduledDatetime) { this.scheduledDatetime = scheduledDatetime; }
    public void setTaken(boolean taken) { this.taken = taken; }
    public void setActualDatetime(String actualDatetime) { this.actualDatetime = actualDatetime; }
    public void setWasDelayed(boolean wasDelayed) { this.wasDelayed = wasDelayed; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "IntakeLog{id=" + id + ", medicationName='" + medicationName +
                "', scheduledDatetime='" + scheduledDatetime +
                "', status='" + status + "'}";
    }
}
