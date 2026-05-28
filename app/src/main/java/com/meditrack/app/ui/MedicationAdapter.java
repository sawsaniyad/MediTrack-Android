package com.meditrack.app.ui;

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

import com.meditrack.app.R;
import com.meditrack.app.data.IntakeLog;
import com.meditrack.app.data.Medication;
import com.meditrack.app.data.Schedule;
import com.meditrack.app.db.MedicationDao;

import java.io.File;
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
    private final List<IntakeLog> todayLogs = new ArrayList<>();
    private final Map<Integer, List<Schedule>> schedulesByMedication = new HashMap<>();
    private final MedicationDao medicationDao;
    private final OnMedicationClickListener listener;

    public MedicationAdapter(List<Medication> medications, MedicationDao medicationDao,
                             OnMedicationClickListener listener) {
        if (medications != null) {
            this.medications.addAll(medications);
        }
        this.medicationDao = medicationDao;
        this.listener = listener;
    }

    public void updateList(List<Medication> newList, List<IntakeLog> todayLogs,
                           Map<Integer, List<Schedule>> schedulesMap) {
        medications.clear();
        if (newList != null) {
            medications.addAll(newList);
        }
        this.todayLogs.clear();
        if (todayLogs != null) {
            this.todayLogs.addAll(todayLogs);
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
        if (schedules == null) {
            schedules = medicationDao.getSchedulesForMedication(medication.getId());
            schedulesByMedication.put(medication.getId(), schedules);
        }
        holder.tvNextTime.setText(buildNextTimeLabel(schedules));

        bindImage(holder.ivMedicationImage, medication.getImagePath());
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

    private void bindImage(ImageView imageView, String imagePath) {
        if (!TextUtils.isEmpty(imagePath)) {
            File file = new File(imagePath);
            if (file.exists()) {
                imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath));
                return;
            }
        }
        imageView.setImageResource(R.drawable.ic_medication);
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
        IntakeLog todayLog = findTodayLog(medicationId);
        if (todayLog != null && todayLog.isTaken()) {
            return new StatusInfo("נלקח", colorFromRes(itemView, R.color.status_taken));
        }
        if (isMissed(medicationId, schedules, todayLog)) {
            return new StatusInfo("הוחמץ", colorFromRes(itemView, R.color.status_missed));
        }
        return new StatusInfo("ממתין", colorFromRes(itemView, R.color.status_pending));
    }

    private IntakeLog findTodayLog(int medicationId) {
        for (IntakeLog log : todayLogs) {
            if (log.getMedicationId() == medicationId) {
                return log;
            }
        }
        List<IntakeLog> logs = medicationDao.getLogsByMedication(medicationId);
        String today = LocalDate.now().toString();
        for (IntakeLog log : logs) {
            if (log.getScheduledDatetime() != null
                    && log.getScheduledDatetime().startsWith(today)) {
                return log;
            }
        }
        return null;
    }

    private boolean isMissed(int medicationId, List<Schedule> schedules, IntakeLog todayLog) {
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
            LocalTime intakeTime = LocalTime.parse(schedule.getIntakeTime(),
                    DateTimeFormatter.ofPattern("HH:mm"));
            if (intakeTime.isBefore(now)) {
                return true;
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

    private String buildNextTimeLabel(List<Schedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return "אין שעה מתוכננת";
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
            return "היום: " + schedules.get(0).getIntakeTime();
        }
        return "הבא: " + next;
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
