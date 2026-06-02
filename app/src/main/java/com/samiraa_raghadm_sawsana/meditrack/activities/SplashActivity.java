package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.samiraa_raghadm_sawsana.meditrack.R;

/**
 * מסך פתיחה - SplashActivity
 * Displayed for 2-3 seconds when the app launches.
 * Shows: app logo, app name, student IDs, and submission date.
 * Then navigates automatically to MedicationListActivity.
 *
 * כותבים: סמירה אבו אל-הווא, רע'ד מוחיסן, סוסאן אבו שמא
 * תאריך הגשה: 28.6.2026
 */
public class SplashActivity extends AppCompatActivity {

    // משך הצגת מסך הפתיחה במילישניות (3 שניות)
    private static final int SPLASH_DURATION_MS = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // שימוש ב-Handler לניווט אוטומטי לאחר SPLASH_DURATION_MS
        // Using Handler (not deprecated Timer/AsyncTask) to delay navigation
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // מעבר למסך הראשי
            Intent intent = new Intent(SplashActivity.this, MedicationListActivity.class);
            startActivity(intent);
            // סגירת ה-SplashActivity כדי שלחיצה על Back לא תחזיר אליה
            finish();
        }, SPLASH_DURATION_MS);
    }
}