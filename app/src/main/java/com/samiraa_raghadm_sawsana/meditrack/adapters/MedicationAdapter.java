package com.samiraa_raghadm_sawsana.meditrack.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;
import com.samiraa_raghadm_sawsana.meditrack.models.Medication;
import com.samiraa_raghadm_sawsana.meditrack.models.Schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder> {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public interface OnMedicationActionListener {
        void onMedicationClick(Medication medication);

        void onMarkTaken(Medication medication, String scheduledDatetime);

        void onMarkMissed(Medication medication, String scheduledDatetime);
    }

    private final List<Medication> medications = new ArrayList<>();
    private final List<IntakeLog> selectedDateLogs = new ArrayList<>();
    private final OnMedicationActionListener listener;
    private Map<Integer, List<Schedule>> schedulesByMedication;
    private LocalDate selectedDate = LocalDate.now();

    public MedicationAdapter(OnMedicationActionListener listener) {
        this.listener = listener;
    }

    public void updateList(List<Medication> newList,
                           List<IntakeLog> dayLogs,
                           Map<Integer, List<Schedule>> schedulesMap,
                           LocalDate selectedDate) {
        medications.clear();
        if (newList != null) {
            medications.addAll(newList);
        }

        selectedDateLogs.clear();
        if (dayLogs != null) {
            selectedDateLogs.addAll(dayLogs);
        }

        schedulesByMedication = schedulesMap;
        this.selectedDate = selectedDate != null ? selectedDate : LocalDate.now();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medication, parent, false);
        return new MedicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        Medication medication = medications.get(position);
        holder.tvMedicationName.setText(medication.getName());
        holder.tvDosage.setText(
                TextUtils.isEmpty(medication.getDosage()) ? "" : medication.getDosage());

        List<Schedule> relevantSchedules = getRelevantSchedules(medication.getId());
        List<IntakeLog> medicationLogs = getLogsForMedication(medication.getId());

        holder.tvNextTime.setText(buildNextTimeLabel(holder.itemView, relevantSchedules));

        bindImage(holder, medication.getImagePath());
        bindStatus(holder.itemView, holder, medicationLogs, relevantSchedules);
        bindQuickActions(holder, medication, medicationLogs, relevantSchedules);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMedicationClick(medication);
            }
        });
    }

    @Override
    public int getItemCount() {
        return medications.size();
    }

    public Medication getMedicationAt(int position) {
        if (position < 0 || position >= medications.size()) {
            return null;
        }
        return medications.get(position);
    }

    private List<Schedule> getRelevantSchedules(int medicationId) {
        List<Schedule> schedules = schedulesByMedication != null
                ? schedulesByMedication.get(medicationId) : null;
        List<Schedule> relevantSchedules = new ArrayList<>();
        if (schedules == null) {
            return relevantSchedules;
        }
        int dayValue = selectedDate.getDayOfWeek().getValue();
        for (Schedule schedule : schedules) {
            if (schedule.isEnabled() && isDayIncluded(schedule.getDaysOfWeek(), dayValue)) {
                relevantSchedules.add(schedule);
            }
        }
        return relevantSchedules;
    }

    private void bindImage(MedicationViewHolder holder, String imagePath) {
        holder.boundImagePath = imagePath;
        holder.ivMedicationImage.setImageResource(R.drawable.ic_medication_placeholder);

        if (imagePath != null && !imagePath.isEmpty()) {
            AppExecutors.getInstance().diskIO(() -> {
                Bitmap bmp = BitmapFactory.decodeFile(imagePath);
                if (bmp != null) {
                    Bitmap thumb = Bitmap.createScaledBitmap(bmp, 80, 80, true);
                    AppExecutors.getInstance().mainThread(() -> {
                        if (imagePath.equals(holder.boundImagePath)) {
                            holder.ivMedicationImage.setImageBitmap(thumb);
                        }
                    });
                } else {
                    AppExecutors.getInstance().mainThread(() -> {
                        if (imagePath.equals(holder.boundImagePath)) {
                            holder.ivMedicationImage.setImageResource(
                                    R.drawable.ic_medication_placeholder);
                        }
                    });
                }
            });
        }
    }

    private void bindStatus(View itemView,
                            MedicationViewHolder holder,
                            List<IntakeLog> medicationLogs,
                            List<Schedule> schedules) {
        StatusInfo status = resolveStatus(itemView, medicationLogs, schedules);
        holder.tvStatus.setText(status.label);
        GradientDrawable badge = (GradientDrawable) holder.viewStatusBadge.getBackground();
        if (badge != null) {
            badge.setColor(status.color);
        } else {
            holder.viewStatusBadge.setBackgroundColor(status.color);
        }
    }

    private void bindQuickActions(MedicationViewHolder holder,
                                  Medication medication,
                                  List<IntakeLog> medicationLogs,
                                  List<Schedule> schedules) {
        ActionWindow actionWindow = findActionWindow(schedules, medicationLogs);
        boolean showActions = actionWindow != null && listener != null;
        holder.llQuickActions.setVisibility(showActions ? View.VISIBLE : View.GONE);

        holder.btnMarkTaken.setOnClickListener(null);
        holder.btnMarkMissed.setOnClickListener(null);

        if (!showActions) {
            return;
        }

        holder.btnMarkTaken.setOnClickListener(v ->
                listener.onMarkTaken(medication, actionWindow.scheduledDatetime));
        holder.btnMarkMissed.setOnClickListener(v ->
                listener.onMarkMissed(medication, actionWindow.scheduledDatetime));
    }

    private ActionWindow findActionWindow(List<Schedule> schedules, List<IntakeLog> medicationLogs) {
        if (!selectedDate.equals(LocalDate.now()) || schedules.isEmpty()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        ActionWindow bestWindow = null;
        for (Schedule schedule : schedules) {
            try {
                LocalTime intakeTime = LocalTime.parse(schedule.getIntakeTime(), TIME_FORMATTER);
                LocalDateTime start = selectedDate.atTime(intakeTime);
                LocalDateTime end = start.plusMinutes(10);
                if (now.isBefore(start) || now.isAfter(end)) {
                    continue;
                }
                String scheduledDatetime = start.format(DATE_TIME_FORMATTER);
                if (hasResolvedLog(medicationLogs, scheduledDatetime)) {
                    continue;
                }
                if (bestWindow == null || start.isAfter(bestWindow.start)) {
                    bestWindow = new ActionWindow(start, scheduledDatetime);
                }
            } catch (Exception ignored) {
            }
        }
        return bestWindow;
    }

    private boolean hasResolvedLog(List<IntakeLog> medicationLogs, String scheduledDatetime) {
        for (IntakeLog log : medicationLogs) {
            if (!scheduledDatetime.equals(log.getScheduledDatetime())) {
                continue;
            }
            if (log.isTaken() || IntakeLog.STATUS_MISSED.equals(log.getStatus())) {
                return true;
            }
        }
        return false;
    }

    private StatusInfo resolveStatus(View itemView,
                                     List<IntakeLog> medicationLogs,
                                     List<Schedule> schedules) {
        if (schedules.isEmpty()) {
            return new StatusInfo(itemView.getContext().getString(R.string.status_pending),
                    colorFromRes(itemView, R.color.status_pending));
        }

        if (selectedDate.isBefore(LocalDate.now())) {
            return new StatusInfo(itemView.getContext().getString(R.string.status_missed),
                    colorFromRes(itemView, R.color.status_missed));
        }

        if (selectedDate.isAfter(LocalDate.now())) {
            return new StatusInfo(itemView.getContext().getString(R.string.status_pending),
                    colorFromRes(itemView, R.color.status_pending));
        }

        LocalDateTime now = LocalDateTime.now();
        ScheduleStatusSlot activeOrLatestPastSlot = null;
        ScheduleStatusSlot nextUpcomingSlot = null;

        for (Schedule schedule : schedules) {
            try {
                LocalTime intakeTime = LocalTime.parse(schedule.getIntakeTime(), TIME_FORMATTER);
                LocalDateTime scheduledStart = selectedDate.atTime(intakeTime);
                ScheduleStatusSlot slot = new ScheduleStatusSlot(
                        scheduledStart,
                        findLogForScheduledDatetime(
                                medicationLogs, scheduledStart.format(DATE_TIME_FORMATTER)));

                if (scheduledStart.isAfter(now)) {
                    if (nextUpcomingSlot == null
                            || scheduledStart.isBefore(nextUpcomingSlot.start)) {
                        nextUpcomingSlot = slot;
                    }
                    continue;
                }

                if (activeOrLatestPastSlot == null
                        || scheduledStart.isAfter(activeOrLatestPastSlot.start)) {
                    activeOrLatestPastSlot = slot;
                }
            } catch (Exception ignored) {
            }
        }

        if (activeOrLatestPastSlot != null && activeOrLatestPastSlot.isWithinActionWindow(now)
                && !activeOrLatestPastSlot.isResolved()) {
            return new StatusInfo(itemView.getContext().getString(R.string.status_pending),
                    colorFromRes(itemView, R.color.status_pending));
        }

        if (nextUpcomingSlot != null) {
            return new StatusInfo(itemView.getContext().getString(R.string.status_pending),
                    colorFromRes(itemView, R.color.status_pending));
        }

        if (activeOrLatestPastSlot != null) {
            if (activeOrLatestPastSlot.isTaken()) {
                return new StatusInfo(itemView.getContext().getString(R.string.status_taken),
                        colorFromRes(itemView, R.color.status_taken));
            }
            if (activeOrLatestPastSlot.isMissed() || activeOrLatestPastSlot.isWindowExpired(now)) {
                return new StatusInfo(itemView.getContext().getString(R.string.status_missed),
                        colorFromRes(itemView, R.color.status_missed));
            }
        }

        return new StatusInfo(itemView.getContext().getString(R.string.status_pending),
                colorFromRes(itemView, R.color.status_pending));
    }

    private IntakeLog findLogForScheduledDatetime(List<IntakeLog> medicationLogs,
                                                  String scheduledDatetime) {
        for (int i = medicationLogs.size() - 1; i >= 0; i--) {
            IntakeLog log = medicationLogs.get(i);
            if (scheduledDatetime.equals(log.getScheduledDatetime())) {
                return log;
            }
        }
        return null;
    }

    private List<IntakeLog> getLogsForMedication(int medicationId) {
        List<IntakeLog> logs = new ArrayList<>();
        for (IntakeLog log : selectedDateLogs) {
            if (log.getMedicationId() == medicationId) {
                logs.add(log);
            }
        }
        return logs;
    }

    private String buildNextTimeLabel(View itemView, List<Schedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return itemView.getContext().getString(R.string.next_time_none);
        }

        String nextTime = null;
        if (selectedDate.equals(LocalDate.now())) {
            LocalTime now = LocalTime.now();
            for (Schedule schedule : schedules) {
                try {
                    LocalTime intakeTime = LocalTime.parse(schedule.getIntakeTime(), TIME_FORMATTER);
                    if (!intakeTime.isBefore(now)) {
                        String time = schedule.getIntakeTime();
                        if (nextTime == null || time.compareTo(nextTime) < 0) {
                            nextTime = time;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            if (nextTime != null) {
                return itemView.getContext().getString(R.string.next_time_upcoming, nextTime);
            }
        }

        nextTime = schedules.get(0).getIntakeTime();
        return itemView.getContext().getString(R.string.next_time_today, nextTime);
    }

    private boolean isDayIncluded(String daysOfWeek, int dayValue) {
        if (TextUtils.isEmpty(daysOfWeek)) {
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

    private int colorFromRes(View view, int resId) {
        return view.getContext().getResources().getColor(resId, view.getContext().getTheme());
    }

    static class MedicationViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivMedicationImage;
        final TextView tvMedicationName;
        final TextView tvDosage;
        final TextView tvNextTime;
        final View viewStatusBadge;
        final TextView tvStatus;
        final LinearLayout llQuickActions;
        final TextView btnMarkTaken;
        final TextView btnMarkMissed;
        String boundImagePath;

        MedicationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMedicationImage = itemView.findViewById(R.id.ivMedicationImage);
            tvMedicationName = itemView.findViewById(R.id.tvMedicationName);
            tvDosage = itemView.findViewById(R.id.tvDosage);
            tvNextTime = itemView.findViewById(R.id.tvNextTime);
            viewStatusBadge = itemView.findViewById(R.id.viewStatusBadge);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            llQuickActions = itemView.findViewById(R.id.llQuickActions);
            btnMarkTaken = itemView.findViewById(R.id.btnMarkTaken);
            btnMarkMissed = itemView.findViewById(R.id.btnMarkMissed);
        }
    }

    private static class StatusInfo {
        final String label;
        final int color;

        StatusInfo(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    private static class ActionWindow {
        final LocalDateTime start;
        final String scheduledDatetime;

        ActionWindow(LocalDateTime start, String scheduledDatetime) {
            this.start = start;
            this.scheduledDatetime = scheduledDatetime;
        }
    }

    private static class ScheduleStatusSlot {
        final LocalDateTime start;
        final IntakeLog log;

        ScheduleStatusSlot(LocalDateTime start, IntakeLog log) {
            this.start = start;
            this.log = log;
        }

        boolean isWithinActionWindow(LocalDateTime now) {
            return !now.isBefore(start) && now.isBefore(start.plusMinutes(10));
        }

        boolean isWindowExpired(LocalDateTime now) {
            return !start.plusMinutes(10).isAfter(now);
        }

        boolean isResolved() {
            return isTaken() || isMissed();
        }

        boolean isTaken() {
            return log != null && log.isTaken();
        }

        boolean isMissed() {
            return log != null && IntakeLog.STATUS_MISSED.equals(log.getStatus());
        }
    }
}
