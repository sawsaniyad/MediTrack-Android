package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.samiraa_raghadm_sawsana.meditrack.R;

public class SplashActivity extends BaseActivity {

    private static final long SPLASH_DELAY_MS = 2500L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MedicationListActivity.class));
            finish();
        }, SPLASH_DELAY_MS);
    }
}
