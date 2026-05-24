package com.samiraa_raghadm_sawsana.meditrack.models;

/**
 * מחלקת מודל לרישום נטילת תרופה
 * Model class representing a single intake log entry.
 * Records whether a medication was taken, snoozed, or missed at a given time.
 */
public class IntakeLog {

    // סטטוסים אפשריים לנטילה / Possible intake statuses
    public static final String STATUS_TAKEN   = "נלקח";
    public static final String STATUS_MISSED  = "הוחמץ";
    public static final String STATUS_SNOOZED = "נדחה";
    public static final String STATUS_PENDING = "ממתין";

    // -------- שדות פרטיים / Private Fields --------
    private int id;
    private int medicationId;       // מזהה התרופה
    private int scheduleId;         // מזהה התזמון
    private String medicationName;  // שם התרופה (לנוחות תצוגה)
    private String scheduledTime;   // שעה מתוזמנת בפורמט HH:mm
    private String date;            // תאריך בפורמט yyyy-MM-dd
    private String status;          // סטטוס הנטילה
    private long timestamp;         // חותמת זמן בפועל של הנטילה

    // -------- בנאי ריק / Default Constructor --------
    public IntakeLog() {}

    // -------- בנאי מלא / Full Constructor --------
    public IntakeLog(int id, int medicationId, int scheduleId, String medicationName,
                     String scheduledTime, String date, String status, long timestamp) {
        this.id = id;
        this.medicationId = medicationId;
        this.scheduleId = scheduleId;
        this.medicationName = medicationName;
        this.scheduledTime = scheduledTime;
        this.date = date;
        this.status = status;
        this.timestamp = timestamp;
    }

    // בנאי ללא id
    public IntakeLog(int medicationId, int scheduleId, String medicationName,
                     String scheduledTime, String date, String status, long timestamp) {
        this.medicationId = medicationId;
        this.scheduleId = scheduleId;
        this.medicationName = medicationName;
        this.scheduledTime = scheduledTime;
        this.date = date;
        this.status = status;
        this.timestamp = timestamp;
    }

    // -------- Getters --------
    public int getId() { return id; }
    public int getMedicationId() { return medicationId; }
    public int getScheduleId() { return scheduleId; }
    public String getMedicationName() { return medicationName; }
    public String getScheduledTime() { return scheduledTime; }
    public String getDate() { return date; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }

    // -------- Setters --------
    public void setId(int id) { this.id = id; }
    public void setMedicationId(int medicationId) { this.medicationId = medicationId; }
    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }
    public void setDate(String date) { this.date = date; }
    public void setStatus(String status) { this.status = status; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "IntakeLog{id=" + id + ", medicationName='" + medicationName +
                "', date='" + date + "', status='" + status + "'}";
    }
}