package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.content.Intent;
import android.graphics.Color;
import android.view.MenuItem;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.samiraa_raghadm_sawsana.meditrack.R;

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

    protected void setupOverflowMenu(MaterialToolbar toolbar, @Nullable Integer currentItemId) {
        toolbar.inflateMenu(R.menu.menu_medication_list);
        if (currentItemId != null) {
            MenuItem currentItem = toolbar.getMenu().findItem(currentItemId);
            if (currentItem != null) {
                currentItem.setVisible(false);
            }
        }
        toolbar.setOnMenuItemClickListener(this::handleOverflowMenuItem);
    }

    protected boolean handleOverflowMenuItem(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        if (id == R.id.action_history) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_exit) {
            finishAffinity();
            return true;
        }
        return false;
    }

    protected void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.about_close, null)
                .show();
    }
}
