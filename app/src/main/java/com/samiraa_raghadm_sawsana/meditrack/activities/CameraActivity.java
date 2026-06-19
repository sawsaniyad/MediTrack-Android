package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.os.Bundle;

import com.samiraa_raghadm_sawsana.meditrack.R;

// TODO Sawsan: replace this stub with the real Camera2 implementation.
// CameraX was removed (not compatible with compileSdk 30).
// EXTRA_IMAGE_PATH is kept so AddEditMedicationActivity still compiles.
public class CameraActivity extends BaseActivity {

    public static final String EXTRA_IMAGE_PATH = "IMAGE_PATH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
    }
}