package com.samiraa_raghadm_sawsana.meditrack.ui;

import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    private FrameLayout loadingOverlay;

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected void showLoading() {
        if (loadingOverlay != null) {
            return;
        }

        ViewGroup root = findViewById(android.R.id.content);
        loadingOverlay = new FrameLayout(this);
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        loadingOverlay.setLayoutParams(overlayParams);
        loadingOverlay.setBackgroundColor(Color.parseColor("#80000000"));
        loadingOverlay.setClickable(true);
        loadingOverlay.setFocusable(true);

        ProgressBar progressBar = new ProgressBar(this);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        progressBar.setLayoutParams(progressParams);
        progressBar.setIndeterminate(true);

        loadingOverlay.addView(progressBar);
        root.addView(loadingOverlay);
    }

    protected void hideLoading() {
        if (loadingOverlay != null) {
            ViewGroup root = findViewById(android.R.id.content);
            root.removeView(loadingOverlay);
            loadingOverlay = null;
        }
    }
}
