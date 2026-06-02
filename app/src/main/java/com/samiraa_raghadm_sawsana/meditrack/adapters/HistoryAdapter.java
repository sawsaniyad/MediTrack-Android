package com.samiraa_raghadm_sawsana.meditrack.adapters;

import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<IntakeLog> logs = new ArrayList<>();
    private final Map<Integer, String> medicationNames = new HashMap<>();

    public void updateList(List<IntakeLog> newLogs, Map<Integer, String> names) {
        logs.clear();
        if (newLogs != null) {
            logs.addAll(newLogs);
        }
        medicationNames.clear();
        if (names != null) {
            medicationNames.putAll(names);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        IntakeLog log = logs.get(position);
        String medName = medicationNames.get(log.getMedicationId());
        if (medName == null) {
            medName = holder.itemView.getContext().getString(
                    R.string.history_med_fallback, log.getMedicationId());
        }

        holder.tvScheduledTime.setText(holder.itemView.getContext().getString(
                R.string.history_scheduled, log.getScheduledDatetime()));
        holder.tvHistoryMedName.setText(medName);

        if (log.isTaken() && !TextUtils.isEmpty(log.getActualDatetime())) {
            holder.tvActualTime.setText(holder.itemView.getContext().getString(
                    R.string.history_taken_at, log.getActualDatetime()));
        } else {
            holder.tvActualTime.setText(R.string.history_not_taken);
        }

        int colorRes;
        int statusLabelRes;
        if (log.isTaken()) {
            colorRes = R.color.status_taken;
            statusLabelRes = R.string.status_taken;
        } else if (isMissedLog(log)) {
            colorRes = R.color.status_missed;
            statusLabelRes = R.string.status_missed;
        } else {
            colorRes = R.color.status_pending;
            statusLabelRes = R.string.status_pending;
        }

        holder.tvHistoryStatus.setText(statusLabelRes);
        GradientDrawable badge = (GradientDrawable) holder.viewHistoryBadge.getBackground();
        int color = holder.itemView.getContext().getResources()
                .getColor(colorRes, holder.itemView.getContext().getTheme());
        if (badge != null) {
            badge.setColor(color);
        } else {
            holder.viewHistoryBadge.setBackgroundColor(color);
        }

        if (log.isWasDelayed()) {
            holder.tvDelayInfo.setVisibility(View.VISIBLE);
            holder.tvDelayInfo.setText(R.string.history_delayed);
        } else {
            holder.tvDelayInfo.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    private boolean isMissedLog(IntakeLog log) {
        return !log.isTaken();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        final TextView tvScheduledTime;
        final TextView tvHistoryMedName;
        final TextView tvActualTime;
        final View viewHistoryBadge;
        final TextView tvHistoryStatus;
        final TextView tvDelayInfo;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvScheduledTime = itemView.findViewById(R.id.tvScheduledTime);
            tvHistoryMedName = itemView.findViewById(R.id.tvHistoryMedName);
            tvActualTime = itemView.findViewById(R.id.tvActualTime);
            viewHistoryBadge = itemView.findViewById(R.id.viewHistoryBadge);
            tvHistoryStatus = itemView.findViewById(R.id.tvHistoryStatus);
            tvDelayInfo = itemView.findViewById(R.id.tvDelayInfo);
        }
    }
}
