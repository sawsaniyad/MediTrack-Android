package com.samiraa_raghadm_sawsana.meditrack.activities;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.samiraa_raghadm_sawsana.meditrack.BuildConfig;
import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.adapters.MedicationAdapter;
import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDAO;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AlarmScheduler;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.helpers.NotificationHelper;
import com.samiraa_raghadm_sawsana.meditrack.helpers.PermissionManager;
import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;
import com.samiraa_raghadm_sawsana.meditrack.models.Schedule;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MedicationListActivity extends BaseActivity {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final long HOME_REFRESH_MS = 15000L;

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadMedications();
            refreshHandler.postDelayed(this, HOME_REFRESH_MS);
        }
    };

    private RecyclerView recyclerView;
    private View emptyStateContainer;
    private View viewProgressFill;
    private TextView tvProgressCount;
    private TextView tvCountTaken;
    private TextView tvCountPending;
    private TextView tvCountMissed;
    private MedicationAdapter adapter;
    private MedicationDAO dao;
    private ActivityResultLauncher<String> notifPermLauncher;

    private final BroadcastReceiver medicationDueReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int medicationId = intent.getIntExtra(getString(R.string.extra_medication_id), -1);
            String medName = intent.getStringExtra(getString(R.string.extra_medication_name));
            if (medName == null) {
                medName = getString(R.string.default_medication_name);
            }
            showMedicationDueSnackbar(medName, medicationId);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_list);

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
        }

        dao = new MedicationDAO(DatabaseHelper.getInstance(this));
        NotificationHelper.createNotificationChannel(this);

        notifPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (!granted) {
                        showToast(getString(R.string.msg_notifications_disabled));
                    }
                    scheduleAlarms();
                });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.getMenu().clear();
        setupOverflowMenu(toolbar, null);

        recyclerView = findViewById(R.id.recyclerViewMedications);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        viewProgressFill = findViewById(R.id.viewProgressFill);
        tvProgressCount = findViewById(R.id.tvProgressCount);
        tvCountTaken = findViewById(R.id.tvCountTaken);
        tvCountPending = findViewById(R.id.tvCountPending);
        tvCountMissed = findViewById(R.id.tvCountMissed);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicationAdapter(new MedicationAdapter.OnMedicationActionListener() {
            @Override
            public void onMedicationClick(Medication medication) {
                Intent intent = new Intent(MedicationListActivity.this, AddEditMedicationActivity.class);
                intent.putExtra(AddEditMedicationActivity.EXTRA_MEDICATION_ID, medication.getId());
                startActivity(intent);
            }

            @Override
            public void onMarkTaken(Medication medication, String scheduledDatetime) {
                markMedicationTakenFromCard(medication, scheduledDatetime);
            }

            @Override
            public void onMarkMissed(Medication medication, String scheduledDatetime) {
                markMedicationMissedFromCard(medication, scheduledDatetime);
            }
        });
        recyclerView.setAdapter(adapter);
        attachSwipeToDelete();

        FloatingActionButton fab = findViewById(R.id.fabAddMedication);
        fab.setOnClickListener(v -> startActivity(new Intent(this, AddEditMedicationActivity.class)));

        findViewById(R.id.btnOpenHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        requestNotificationPermission();
        loadMedications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                medicationDueReceiver,
                new IntentFilter(getString(R.string.broadcast_medication_due)));
        loadMedications();
        startAutoRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(medicationDueReceiver);
        stopAutoRefresh();
    }

    private void startAutoRefresh() {
        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.postDelayed(refreshRunnable, HOME_REFRESH_MS);
    }

    private void stopAutoRefresh() {
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (!PermissionManager.isGranted(this, "android.permission.POST_NOTIFICATIONS")) {
                if (shouldShowRequestPermissionRationale("android.permission.POST_NOTIFICATIONS")) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.perm_notif_title)
                            .setMessage(R.string.perm_notif_message)
                            .setPositiveButton(R.string.perm_notif_allow,
                                    (d, w) -> notifPermLauncher.launch("android.permission.POST_NOTIFICATIONS"))
                            .setNegativeButton(R.string.perm_notif_later, (d, w) -> scheduleAlarms())
                            .show();
                } else {
                    notifPermLauncher.launch("android.permission.POST_NOTIFICATIONS");
                }
            } else {
                scheduleAlarms();
            }
        } else {
            scheduleAlarms();
        }
    }

    private void scheduleAlarms() {
        AppExecutors.getInstance().diskIO(() ->
                AlarmScheduler.scheduleAllAlarms(getApplicationContext(), dao));
    }

    private void loadMedications() {
        LocalDate selectedDate = LocalDate.now();
        String selectedDateText = selectedDate.toString();

        AppExecutors.getInstance().diskIO(() -> {
            List<Medication> medications = dao.getActiveMedications();
            List<IntakeLog> dayLogs = dao.getLogsByDateRange(selectedDateText, selectedDateText);
            Map<Integer, List<Schedule>> schedulesMap = new HashMap<>();
            for (Medication medication : medications) {
                schedulesMap.put(medication.getId(), dao.getSchedulesForMedication(medication.getId()));
            }

            DashboardStats stats = buildDashboardStats(medications, dayLogs, schedulesMap, selectedDate);

            AppExecutors.getInstance().mainThread(() -> {
                if (medications.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyStateContainer.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateContainer.setVisibility(View.GONE);
                }
                adapter.updateList(medications, dayLogs, schedulesMap, selectedDate);
                bindDashboardStats(stats);
            });
        });
    }

    private DashboardStats buildDashboardStats(List<Medication> medications,
                                               List<IntakeLog> dayLogs,
                                               Map<Integer, List<Schedule>> schedulesMap,
                                               LocalDate selectedDate) {
        DashboardStats stats = new DashboardStats();
        for (Medication medication : medications) {
            List<Schedule> schedules = schedulesMap.get(medication.getId());
            if (schedules == null) {
                continue;
            }
            for (Schedule schedule : schedules) {
                if (!schedule.isEnabled() || !isScheduledForDate(schedule, selectedDate)) {
                    continue;
                }
                stats.total++;
                IntakeLog matchingLog = findMatchingLog(dayLogs, medication.getId(), schedule.getIntakeTime());
                if (matchingLog != null && matchingLog.isTaken()) {
                    stats.taken++;
                } else if (isMissedSlot(selectedDate, schedule, matchingLog)) {
                    stats.missed++;
                } else {
                    stats.pending++;
                }
            }
        }
        return stats;
    }

    private IntakeLog findMatchingLog(List<IntakeLog> logs, int medicationId, String intakeTime) {
        for (int i = logs.size() - 1; i >= 0; i--) {
            IntakeLog log = logs.get(i);
            if (log.getMedicationId() != medicationId) {
                continue;
            }
            String scheduledDatetime = log.getScheduledDatetime();
            if (scheduledDatetime != null && scheduledDatetime.length() >= 16
                    && intakeTime.equals(scheduledDatetime.substring(11, 16))) {
                return log;
            }
        }
        return null;
    }

    private IntakeLog findLogByScheduledDatetime(List<IntakeLog> logs, String scheduledDatetime) {
        for (int i = logs.size() - 1; i >= 0; i--) {
            IntakeLog log = logs.get(i);
            if (scheduledDatetime.equals(log.getScheduledDatetime())) {
                return log;
            }
        }
        return null;
    }

    private boolean isMissedSlot(LocalDate selectedDate, Schedule schedule, IntakeLog matchingLog) {
        if (matchingLog != null) {
            if (matchingLog.isTaken()) {
                return false;
            }
            String status = matchingLog.getStatus();
            if (IntakeLog.STATUS_MISSED.equals(status)) {
                return true;
            }
            if (selectedDate.isBefore(LocalDate.now())) {
                return true;
            }
            if (selectedDate.isAfter(LocalDate.now())) {
                return false;
            }
        }

        if (selectedDate.isBefore(LocalDate.now())) {
            return true;
        }
        if (selectedDate.isAfter(LocalDate.now())) {
            return false;
        }

        try {
            LocalTime intakeTime = LocalTime.parse(schedule.getIntakeTime(), TIME_FORMATTER);
            LocalDateTime scheduledStart = selectedDate.atTime(intakeTime);
            LocalDateTime actionWindowEnd = scheduledStart.plusMinutes(10);
            return !actionWindowEnd.isAfter(LocalDateTime.now());
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isDayIncluded(String daysOfWeek, int dayValue) {
        if (daysOfWeek == null || daysOfWeek.trim().isEmpty()) {
            return true;
        }
        String[] parts = daysOfWeek.split(",");
        for (String part : parts) {
            try {
                if (Integer.parseInt(part.trim()) == dayValue) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    private boolean isScheduledForDate(Schedule schedule, LocalDate selectedDate) {
        return isDayIncluded(schedule.getDaysOfWeek(), selectedDate.getDayOfWeek().getValue());
    }

    private void bindDashboardStats(DashboardStats stats) {
        tvProgressCount.setText(String.format(Locale.getDefault(), "%d / %d", stats.taken, stats.total));
        tvCountTaken.setText(String.format(Locale.getDefault(), "%s %d", getString(R.string.status_taken), stats.taken));
        tvCountPending.setText(String.format(Locale.getDefault(), "%s %d", getString(R.string.status_pending), stats.pending));
        tvCountMissed.setText(String.format(Locale.getDefault(), "%s %d", getString(R.string.status_missed), stats.missed));
        updateProgressFill(stats.total == 0 ? 0f : (float) stats.taken / (float) stats.total);
    }

    private void updateProgressFill(float progressRatio) {
        viewProgressFill.post(() -> {
            View parent = (View) viewProgressFill.getParent();
            int availableWidth = parent.getWidth();
            ViewGroup.LayoutParams params = viewProgressFill.getLayoutParams();
            params.width = Math.round(availableWidth * progressRatio);
            viewProgressFill.setLayoutParams(params);
        });
    }

    private void showMedicationDueSnackbar(String medName, int medicationId) {
        Snackbar.make(recyclerView,
                getString(R.string.msg_medication_due, medName),
                Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_action_taken, v -> markMedicationTaken(medicationId))
                .show();
    }

    private void attachSwipeToDelete() {
        final Drawable deleteIcon = ContextCompat.getDrawable(this, R.drawable.ic_delete);
        final int deleteRed = ContextCompat.getColor(this, R.color.delete_red);
        final String deleteLabel = getString(R.string.delete);

        final Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(deleteRed);

        final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(spToPx(15f));

        final int padding = (int) dpToPx(20f);

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                Medication medication = adapter.getMedicationAt(position);
                if (medication == null) {
                    return;
                }
                confirmDeleteFromSwipe(medication, position);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                // Only draw the delete background while swiping left (dX < 0).
                if (dX < 0) {
                    c.drawRect(itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom(), backgroundPaint);

                    if (deleteIcon != null) {
                        int iconHeight = deleteIcon.getIntrinsicHeight();
                        int iconWidth = deleteIcon.getIntrinsicWidth();
                        int iconTop = itemView.getTop()
                                + (itemView.getHeight() - iconHeight) / 2;
                        int iconRight = itemView.getRight() - padding;
                        int iconLeft = iconRight - iconWidth;
                        deleteIcon.setBounds(iconLeft, iconTop,
                                iconRight, iconTop + iconHeight);
                        deleteIcon.draw(c);

                        float textY = itemView.getTop() + itemView.getHeight() / 2f
                                - (textPaint.descent() + textPaint.ascent()) / 2f;
                        c.drawText(deleteLabel, iconLeft - dpToPx(12f), textY, textPaint);
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState,
                        isCurrentlyActive);
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    private void confirmDeleteFromSwipe(Medication medication, int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_medication_title)
                .setMessage(getString(R.string.delete_medication_message, medication.getName()))
                .setPositiveButton(R.string.delete_confirm, (dialog, which) -> deleteMedication(medication))
                // Restore the swiped-away row if the user backs out.
                .setNegativeButton(R.string.cancel, (dialog, which) -> adapter.notifyItemChanged(position))
                .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                .show();
    }

    private void deleteMedication(Medication medication) {
        AppExecutors.getInstance().diskIO(() -> {
            AlarmScheduler.cancelAlarmsForMedication(
                    getApplicationContext(), dao, medication.getId());
            dao.deleteMedication(medication.getId());
            AppExecutors.getInstance().mainThread(() -> {
                showToast(getString(R.string.msg_deleted_success));
                loadMedications();
            });
        });
    }

    private void markMedicationTaken(int medicationId) {
        String actualTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()).format(new Date());
        AppExecutors.getInstance().diskIO(() -> {
            List<IntakeLog> logs = dao.getLogsByMedication(medicationId);
            IntakeLog targetLog = findLatestOpenLog(logs);
            if (targetLog != null) {
                dao.markAsTaken(targetLog.getId(), actualTime);
            }
            AppExecutors.getInstance().mainThread(this::loadMedications);
        });
    }

    private void markMedicationTakenFromCard(Medication medication, String scheduledDatetime) {
        String actualTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()).format(new Date());
        AppExecutors.getInstance().diskIO(() -> {
            List<IntakeLog> logs = dao.getLogsByMedication(medication.getId());
            IntakeLog targetLog = findLogByScheduledDatetime(logs, scheduledDatetime);
            if (targetLog != null) {
                dao.markAsTaken(targetLog.getId(), actualTime);
            } else {
                IntakeLog log = new IntakeLog();
                log.setMedicationId(medication.getId());
                log.setMedicationName(medication.getName());
                log.setScheduledDatetime(scheduledDatetime);
                log.setTaken(true);
                log.setActualDatetime(actualTime);
                log.setStatus(IntakeLog.STATUS_TAKEN);
                dao.insertIntakeLog(log);
            }
            AppExecutors.getInstance().mainThread(this::loadMedications);
        });
    }

    private void markMedicationMissedFromCard(Medication medication, String scheduledDatetime) {
        AppExecutors.getInstance().diskIO(() -> {
            List<IntakeLog> logs = dao.getLogsByMedication(medication.getId());
            IntakeLog targetLog = findLogByScheduledDatetime(logs, scheduledDatetime);
            if (targetLog != null) {
                markLogAsMissed(targetLog.getId());
            } else {
                IntakeLog log = new IntakeLog();
                log.setMedicationId(medication.getId());
                log.setMedicationName(medication.getName());
                log.setScheduledDatetime(scheduledDatetime);
                log.setTaken(false);
                log.setStatus(IntakeLog.STATUS_MISSED);
                dao.insertIntakeLog(log);
            }
            AppExecutors.getInstance().mainThread(this::loadMedications);
        });
    }

    private IntakeLog findLatestOpenLog(List<IntakeLog> logs) {
        for (int i = logs.size() - 1; i >= 0; i--) {
            IntakeLog log = logs.get(i);
            if (!log.isTaken()) {
                return log;
            }
        }
        return null;
    }

    private void markLogAsMissed(int logId) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_LOG_TAKEN, 0);
        values.put(DatabaseHelper.COL_LOG_STATUS, IntakeLog.STATUS_MISSED);
        DatabaseHelper.getInstance(this)
                .getWritableDatabase()
                .update(DatabaseHelper.TABLE_INTAKE_LOG,
                        values,
                        DatabaseHelper.COL_LOG_ID + " = ?",
                        new String[]{String.valueOf(logId)});
    }

    @Override
    protected void showAboutDialog() {
        List<String> lines = new ArrayList<>();
        lines.add(getString(R.string.app_name));
        lines.add("ID: " + getPackageName());
        lines.add("");
        lines.add("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        lines.add("");
        lines.add(getString(R.string.splash_submitters));
        lines.add("");
        lines.add(getString(R.string.splash_submission_date));

        StringBuilder aboutMessage = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                aboutMessage.append('\n');
            }
            aboutMessage.append(lines.get(i));
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.about_dialog_title)
                .setMessage(aboutMessage.toString())
                .setPositiveButton(R.string.about_close, null)
                .show();
    }

    private static class DashboardStats {
        int total;
        int taken;
        int pending;
        int missed;
    }
}
