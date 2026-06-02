package com.samiraa_raghadm_sawsana.meditrack.data;

public class Schedule {

    private int id;
    private int medicationId;
    private String intakeTime;
    private String daysOfWeek;

    public Schedule() {
    }

    public Schedule(int id, int medicationId, String intakeTime, String daysOfWeek) {
        this.id = id;
        this.medicationId = medicationId;
        this.intakeTime = intakeTime;
        this.daysOfWeek = daysOfWeek;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(int medicationId) {
        this.medicationId = medicationId;
    }

    public String getIntakeTime() {
        return intakeTime;
    }

    public void setIntakeTime(String intakeTime) {
        this.intakeTime = intakeTime;
    }

    public String getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(String daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }
}
