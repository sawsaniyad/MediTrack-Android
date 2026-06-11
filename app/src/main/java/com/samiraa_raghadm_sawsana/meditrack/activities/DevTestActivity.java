package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDao;
import com.samiraa_raghadm_sawsana.meditrack.receivers.BootReceiver;
import com.samiraa_raghadm_sawsana.meditrack.helpers.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Manual testing helpers — not linked from production UI.
 * Launch via: adb shell am start -n com.meditrack.app/.ui.DevTestActivity
 */
public class DevTestActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev_test);

        NotificationHelper.createNotificationChannel(this);

        findViewById(R.id.btnTestNotification).setOnClickListener(v ->
                NotificationHelper.showMedicationReminder(this, 1,
                        getString(R.string.dev_test_sample_name),
                        getString(R.string.dev_test_sample_dosage), 1));

        findViewById(R.id.btnTestBoot).setOnClickListener(v ->
                new BootReceiver().onReceive(this,
                        new Intent(Intent.ACTION_BOOT_COMPLETED)));

        findViewById(R.id.btnTestMarkTaken).setOnClickListener(v -> {
            String actualTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()).format(new Date());
            AppExecutors.getInstance().diskIO(() -> {
                MedicationDao dao = new MedicationDao(DatabaseHelper.getInstance(this));
                dao.markAsTaken(1, actualTime);
            });
            showToast(getString(R.string.dev_test_mark_taken_toast));
        });

        findViewById(R.id.btnTestHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        findViewById(R.id.btnClearDb).setOnClickListener(v ->
                AppExecutors.getInstance().diskIO(() -> {
                    DatabaseHelper.getInstance(this).resetDatabase();
                    AppExecutors.getInstance().mainThread(() ->
                            showToast(getString(R.string.dev_test_db_cleared)));
                }));
    }
}
