package com.samiraa_raghadm_sawsana.meditrack.models;

/**
 * מחלקת מודל לתזמון תרופה
 * Model class representing a Schedule (when a medication should be taken).
 * Linked to a Medication via medicationId (foreign key).
 *
 * מיזוג שתי גרסאות:
 * - intakeTime, daysOfWeek: שמות שדות ברורים יותר (מ-Raghad)
 * - isEnabled: האם התזמון פעיל (מ-Samira) — נחוץ לביטול תזכורת מבלי למחוק
 * - בנאי ללא id (מ-Samira) — לשימוש בעת הוספה חדשה
 *
 * כותבת: סמירה אבו אל-הווא
 */
public class Schedule {

    // -------- שדות פרטיים / Private Fields --------
    private int id;
    private int medicationId; // מזהה התרופה המקושרת
    private String intakeTime; // שעת הנטילה בפורמט HH:mm
    private String daysOfWeek; // ימי הנטילה (לדוגמה: "ראשון,שני,שלישי")
    private boolean isEnabled; // האם התזמון פעיל — לביטול תזכורת ללא מחיקה

    // -------- בנאי ריק / Default Constructor --------
    public Schedule() {
        this.isEnabled = true; // תזמון חדש תמיד פעיל כברירת מחדל
    }

    // -------- בנאי מלא / Full Constructor (טעינה מ-DB) --------
    public Schedule(int id, int medicationId, String intakeTime,
            String daysOfWeek, boolean isEnabled) {
        this.id = id;
        this.medicationId = medicationId;
        this.intakeTime = intakeTime;
        this.daysOfWeek = daysOfWeek;
        this.isEnabled = isEnabled;
    }

    // -------- בנאי ללא id / Constructor without id (הוספה חדשה) --------
    public Schedule(int medicationId, String intakeTime,
            String daysOfWeek, boolean isEnabled) {
        this.medicationId = medicationId;
        this.intakeTime = intakeTime;
        this.daysOfWeek = daysOfWeek;
        this.isEnabled = isEnabled;
    }

    // -------- Getters --------
    public int getId() {
        return id;
    }

    public int getMedicationId() {
        return medicationId;
    }

    public String getIntakeTime() {
        return intakeTime;
    }

    public String getDaysOfWeek() {
        return daysOfWeek;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    // -------- Setters --------
    public void setId(int id) {
        this.id = id;
    }

    public void setMedicationId(int medicationId) {
        this.medicationId = medicationId;
    }

    public void setIntakeTime(String intakeTime) {
        this.intakeTime = intakeTime;
    }

    public void setDaysOfWeek(String daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @Override
    public String toString() {
        return "Schedule{id=" + id + ", medicationId=" + medicationId +
                ", intakeTime='" + intakeTime + "', daysOfWeek='" + daysOfWeek + "'}";
    }
}
