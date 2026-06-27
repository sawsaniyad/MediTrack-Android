package com.samiraa_raghadm_sawsana.meditrack.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder> {

    public interface OnMedicationClickListener {
        void onMedicationClick(Medication medication);
    }

    private final List<Medication> medications = new ArrayList<>();
    private final Map<Integer, IntakeLog> todayLogsByMedicationId = new HashMap<>();
    private final Map<Integer, List<Schedule>> schedulesByMedication = new HashMap<>();
    private final OnMedicationClickListener listener;

    public MedicationAdapter(OnMedicationClickListener listener) {
        this.listener = listener;
    }

    public void updateList(List<Medication> newList,
                           List<IntakeLog> todayLogs,
                           Map<Integer, List<Schedule>> schedulesMap,
                           Map<Integer, IntakeLog> todayLogsMap) {
        medications.clear();
        if (newList != null) {
            medications.addAll(newList);
        }
        todayLogsByMedicationId.clear();
        if (todayLogsMap != null) {
            todayLogsByMedicationId.putAll(todayLogsMap);
        } else if (todayLogs != null) {
            for (IntakeLog log : todayLogs) {
                todayLogsByMedicationId.put(log.getMedicationId(), log);
            }
        }
        schedulesByMedication.clear();
        if (schedulesMap != null) {
            schedulesByMedication.putAll(schedulesMap);
        }
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

        List<Schedule> schedules = schedulesByMedication.get(medication.getId());
        holder.tvNextTime.setText(buildNextTimeLabel(holder.itemView, schedules));

        bindImage(holder, medication.getImagePath());
        bindStatus(holder.itemView, holder, medication.getId(), schedules);

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

    private void bindImage(MedicationViewHolder holder, String imagePath) {
        // Set placeholder and record which path this holder is bound to BEFORE
        // the async load starts. The stale-check below uses this tag to drop
        // results that arrive after the holder has been rebound to a new item.
        holder.ivMedicationImage.setImageResource(R.drawable.ic_medication_placeholder);
        holder.boundImagePath = imagePath;

        if (imagePath == null || imagePath.isEmpty()) {
            return;
        }

        AppExecutors.getInstance().diskIO(() -> {
            Bitmap bmp = BitmapFactory.decodeFile(imagePath);
            if (bmp == null) {
                return;
            }
            Bitmap thumb = Bitmap.createScaledBitmap(bmp, 80, 80, true);
            // Free the full-size source only when a separate scaled copy was made.
            // Never recycle a bitmap that is still (or will be) held by an ImageView —
            // hardware-accelerated rendering may read it asynchronously after recycle().
            if (thumb != bmp) {
                bmp.recycle();
            }
            AppExecutors.getInstance().mainThread(() -> {
                // Drop stale result if the holder was rebound to a different item.
                if (imagePath.equals(holder.boundImagePath)) {
                    holder.ivMedicationImage.setImageBitmap(thumb);
                }
            });
        });
    }

    private void bindStatus(View itemView, MedicationViewHolder holder, int medicationId,
                            List<Schedule> schedules) {
        StatusInfo status = resolveStatus(itemView, medicationId, schedules);
        holder.tvStatus.setText(status.label);
        GradientDrawable badge = (GradientDrawable) holder.viewStatusBadge.getBackground();
        if (badge != null) {
            badge.setColor(status.color);
        } else {
            holder.viewStatusBadge.setBackgroundColor(status.color);
        }
    }

    private StatusInfo resolveStatus(View itemView, int medicationId, List<Schedule> schedules) {
        IntakeLog todayLog = todayLogsByMedicationId.get(medicationId);
        if (todayLog != null && todayLog.isTaken()) {
            return new StatusInfo(itemView.getContext().getString(R.string.status_taken),
                    colorFromRes(itemView, R.color.status_taken));
        }
        if (isMissed(itemView, schedules, todayLog)) {
            return new StatusInfo(itemView.getContext().getString(R.string.status_missed),
                    colorFromRes(itemView, R.color.status_missed));
        }
        return new StatusInfo(itemView.getContext().getString(R.string.status_pending),
                colorFromRes(itemView, R.color.status_pending));
    }

    private boolean isMissed(View itemView, List<Schedule> schedules, IntakeLog todayLog) {
        if (todayLog != null && !todayLog.isTaken()) {
            return isScheduledTimePassed(todayLog.getScheduledDatetime());
        }
        int todayDow = LocalDate.now().getDayOfWeek().getValue();
        LocalTime now = LocalTime.now();
        if (schedules == null) {
            return false;
        }
        for (Schedule schedule : schedules) {
            if (!isDayIncluded(schedule.getDaysOfWeek(), todayDow)) {
                continue;
            }
            try {
                LocalTime intakeTime = LocalTime.parse(schedule.getIntakeTime(),
                        DateTimeFormatter.ofPattern("HH:mm"));
                if (intakeTime.isBefore(now)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean isScheduledTimePassed(String scheduledDatetime) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(scheduledDatetime);
            return dateTime.isBefore(LocalDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }

    private String buildNextTimeLabel(View itemView, List<Schedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return itemView.getContext().getString(R.string.next_time_none);
        }
        int todayDow = LocalDate.now().getDayOfWeek().getValue();
        LocalTime now = LocalTime.now();
        String next = null;
        for (Schedule schedule : schedules) {
            if (!isDayIncluded(schedule.getDaysOfWeek(), todayDow)) {
                continue;
            }
            String time = schedule.getIntakeTime();
            try {
                LocalTime intakeTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
                if (!intakeTime.isBefore(now)) {
                    if (next == null || time.compareTo(next) < 0) {
                        next = time;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (next == null) {
            return itemView.getContext().getString(R.string.next_time_today,
                    schedules.get(0).getIntakeTime());
        }
        return itemView.getContext().getString(R.string.next_time_upcoming, next);
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
        String boundImagePath;

        MedicationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMedicationImage = itemView.findViewById(R.id.ivMedicationImage);
            tvMedicationName = itemView.findViewById(R.id.tvMedicationName);
            tvDosage = itemView.findViewById(R.id.tvDosage);
            tvNextTime = itemView.findViewById(R.id.tvNextTime);
            viewStatusBadge = itemView.findViewById(R.id.viewStatusBadge);
            tvStatus = itemView.findViewById(R.id.tvStatus);
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
}
