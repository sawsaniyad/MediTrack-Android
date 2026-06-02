package com.samiraa_raghadm_sawsana.meditrack.models;

/**
 * מחלקת מודל לתרופה
 * Model class representing a Medication entry in the database.
 * Follows OOP encapsulation: all fields are private with getters and setters.
 *
 * שדות מיוחדים:
 * - expiryDate: תאריך פקיעת התרופה (מ-Raghad)
 * - emergencyContactName/Phone: איש קשר לחירום (מ-Raghad, נדרש בקורס)
 * - instructions: הוראות נטילה (מ-Raghad, יותר ברור מ-notes)
 *
 * כותבת: סמירה אבו אל-הווא
 */
public class Medication {

    // -------- שדות פרטיים / Private Fields --------
    private int id;
    private String name; // שם התרופה
    private String dosage; // מינון
    private String instructions; // הוראות נטילה
    private String imagePath; // נתיב לתמונת האריזה
    private String expiryDate; // תאריך פקיעה
    private String emergencyContactName; // שם איש קשר לחירום
    private String emergencyContactPhone; // טלפון איש קשר לחירום
    private boolean isActive; // האם התרופה פעילה

    // -------- בנאי ריק / Default Constructor --------
    // isActive = true כברירת מחדל — תרופה חדשה תמיד פעילה
    public Medication() {
        this.isActive = true;
    }

    // -------- בנאי מלא / Full Constructor (טעינה מ-DB) --------
    public Medication(int id, String name, String dosage, String instructions,
            String imagePath, String expiryDate,
            String emergencyContactName, String emergencyContactPhone,
            boolean isActive) {
        this.id = id;
        this.name = name;
        this.dosage = dosage;
        this.instructions = instructions;
        this.imagePath = imagePath;
        this.expiryDate = expiryDate;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.isActive = isActive;
    }

    // -------- בנאי ללא id / Constructor without id (הוספה חדשה) --------
    public Medication(String name, String dosage, String instructions,
            String imagePath, String expiryDate,
            String emergencyContactName, String emergencyContactPhone) {
        this.name = name;
        this.dosage = dosage;
        this.instructions = instructions;
        this.imagePath = imagePath;
        this.expiryDate = expiryDate;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.isActive = true;
    }

    // -------- Getters --------
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDosage() {
        return dosage;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public boolean isActive() {
        return isActive;
    }

    // -------- Setters --------
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return "Medication{id=" + id + ", name='" + name + "', dosage='" + dosage + "'}";
    }
}
