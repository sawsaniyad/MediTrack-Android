package com.samiraa_raghadm_sawsana.meditrack.models;

/**
 * מחלקת מודל לתזמון תרופה
 * Model class representing a Schedule (when a medication should be taken).
 * Linked to a Medication via medicationId (foreign key).
 */
public class Schedule {

    // -------- שדות פרטיים / Private Fields --------
    private int id;
    private int medicationId;   // מזהה התרופה המקושרת
    private String time;        // שעת הנטילה בפורמט HH:mm
    private String days;        // ימי הנטילה (לדוגמה: "ראשון,שני,שלישי")
    private boolean isEnabled;  // האם התזמון פעיל

    // -------- בנאי ריק / Default Constructor --------
    public Schedule() {}

    // -------- בנאי מלא / Full Constructor --------
    public Schedule(int id, int medicationId, String time, String days, boolean isEnabled) {
        this.id = id;
        this.medicationId = medicationId;
        this.time = time;
        this.days = days;
        this.isEnabled = isEnabled;
    }

    // בנאי ללא id
    public Schedule(int medicationId, String time, String days, boolean isEnabled) {
        this.medicationId = medicationId;
        this.time = time;
        this.days = days;
        this.isEnabled = isEnabled;
    }

    // -------- Getters --------
    public int getId() { return id; }
    public int getMedicationId() { return medicationId; }
    public String getTime() { return time; }
    public String getDays() { return days; }
    public boolean isEnabled() { return isEnabled; }

    // -------- Setters --------
    public void setId(int id) { this.id = id; }
    public void setMedicationId(int medicationId) { this.medicationId = medicationId; }
    public void setTime(String time) { this.time = time; }
    public void setDays(String days) { this.days = days; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }

    @Override
    public String toString() {
        return "Schedule{id=" + id + ", medicationId=" + medicationId + ", time='" + time + "', days='" + days + "'}";
    }
}