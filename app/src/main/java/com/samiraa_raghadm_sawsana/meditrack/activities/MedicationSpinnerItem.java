package com.samiraa_raghadm_sawsana.meditrack.activities;

public class MedicationSpinnerItem {

    private final int id;
    private final String name;

    public MedicationSpinnerItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
