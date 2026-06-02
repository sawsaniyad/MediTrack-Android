package com.samiraa_raghadm_sawsana.meditrack.data;

public class IntakeLog {

    private int id;
    private int medicationId;
    private String scheduledDatetime;
    private boolean taken;
    private String actualDatetime;
    private boolean wasDelayed;

    public IntakeLog() {
    }

    public IntakeLog(int id, int medicationId, String scheduledDatetime, boolean taken,
                     String actualDatetime, boolean wasDelayed) {
        this.id = id;
        this.medicationId = medicationId;
        this.scheduledDatetime = scheduledDatetime;
        this.taken = taken;
        this.actualDatetime = actualDatetime;
        this.wasDelayed = wasDelayed;
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

    public String getScheduledDatetime() {
        return scheduledDatetime;
    }

    public void setScheduledDatetime(String scheduledDatetime) {
        this.scheduledDatetime = scheduledDatetime;
    }

    public boolean isTaken() {
        return taken;
    }

    public void setTaken(boolean taken) {
        this.taken = taken;
    }

    public String getActualDatetime() {
        return actualDatetime;
    }

    public void setActualDatetime(String actualDatetime) {
        this.actualDatetime = actualDatetime;
    }

    public boolean isWasDelayed() {
        return wasDelayed;
    }

    public void setWasDelayed(boolean wasDelayed) {
        this.wasDelayed = wasDelayed;
    }
}
