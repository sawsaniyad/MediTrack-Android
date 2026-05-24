package com.samiraa_raghadm_sawsana.meditrack.models;

/**
 * מחלקת מודל לתרופה
 * Model class representing a Medication entry in the database.
 * Follows OOP encapsulation: all fields are private with getters and setters.
 */
public class Medication {

    // -------- שדות פרטיים / Private Fields --------
    private int id;
    private String name;        // שם התרופה
    private String dosage;      // מינון
    private String notes;       // הערות
    private String photoPath;   // נתיב לתמונת האריזה
    private boolean isActive;   // האם התרופה פעילה

    // -------- בנאי ריק / Default Constructor --------
    public Medication() {}

    // -------- בנאי מלא / Full Constructor --------
    public Medication(int id, String name, String dosage, String notes, String photoPath, boolean isActive) {
        this.id = id;
        this.name = name;
        this.dosage = dosage;
        this.notes = notes;
        this.photoPath = photoPath;
        this.isActive = isActive;
    }

    // בנאי ללא id (לשימוש בעת הוספה חדשה לפני שה-DB מקצה id)
    public Medication(String name, String dosage, String notes, String photoPath, boolean isActive) {
        this.name = name;
        this.dosage = dosage;
        this.notes = notes;
        this.photoPath = photoPath;
        this.isActive = isActive;
    }

    // -------- Getters --------
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDosage() { return dosage; }
    public String getNotes() { return notes; }
    public String getPhotoPath() { return photoPath; }
    public boolean isActive() { return isActive; }

    // -------- Setters --------
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return "Medication{id=" + id + ", name='" + name + "', dosage='" + dosage + "'}";
    }
}