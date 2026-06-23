package com.samiraa_raghadm_sawsana.meditrack.adapters;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.samiraa_raghadm_sawsana.meditrack.R;
import com.samiraa_raghadm_sawsana.meditrack.models.IntakeLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryAdapter extends BaseAdapter {

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

    @Override
    public int getCount() {
        return logs.size();
    }

    @Override
    public IntakeLog getItem(int position) {
        return logs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return logs.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        IntakeLog log = getItem(position);
        String medName = medicationNames.get(log.getMedicationId());
        if (TextUtils.isEmpty(medName)) {
            medName = log.getMedicationName();
        }
        if (TextUtils.isEmpty(medName)) {
            medName = parent.getContext().getString(
                    R.string.history_med_fallback, log.getMedicationId());
        }

        holder.tvHistoryMedName.setText(medName);
        holder.tvScheduledTime.setText(parent.getContext().getString(
                R.string.history_scheduled, log.getScheduledDatetime()));

        if (log.isTaken() && !TextUtils.isEmpty(log.getActualDatetime())) {
            holder.tvActualTime.setText(parent.getContext().getString(
                    R.string.history_taken_at, log.getActualDatetime()));
        } else {
            holder.tvActualTime.setText(R.string.history_not_taken);
        }

        bindStatus(holder, log);

        if (log.isWasDelayed()) {
            holder.tvDelayInfo.setVisibility(View.VISIBLE);
            holder.tvDelayInfo.setText(R.string.history_delayed);
        } else {
            holder.tvDelayInfo.setVisibility(View.GONE);
        }

        return convertView;
    }

    private void bindStatus(ViewHolder holder, IntakeLog log) {
        String status = log.getStatus();
        if (IntakeLog.STATUS_TAKEN.equals(status) || log.isTaken()) {
            holder.tvHistoryStatus.setText(R.string.status_taken);
            holder.tvHistoryStatus.setBackgroundResource(R.drawable.bg_rounded_green);
            holder.tvHistoryStatus.setTextColor(Color.parseColor("#2E7D32"));
            holder.viewHistoryBadge.setBackgroundColor(Color.parseColor("#4CAF50"));
            holder.tvActualTime.setTextColor(Color.parseColor("#4CAF50"));
            return;
        }
        if (IntakeLog.STATUS_MISSED.equals(status)) {
            holder.tvHistoryStatus.setText(R.string.status_missed);
            holder.tvHistoryStatus.setBackgroundResource(R.drawable.bg_rounded_red);
            holder.tvHistoryStatus.setTextColor(Color.parseColor("#C62828"));
            holder.viewHistoryBadge.setBackgroundColor(Color.parseColor("#F44336"));
            holder.tvActualTime.setTextColor(Color.parseColor("#F44336"));
            return;
        }

        if (IntakeLog.STATUS_SNOOZED.equals(status)) {
            holder.tvHistoryStatus.setText(R.string.snooze);
        } else {
            holder.tvHistoryStatus.setText(R.string.status_pending);
        }
        holder.tvHistoryStatus.setBackgroundResource(R.drawable.bg_rounded_amber);
        holder.tvHistoryStatus.setTextColor(Color.parseColor("#EF6C00"));
        holder.viewHistoryBadge.setBackgroundColor(Color.parseColor("#FF9800"));
        holder.tvActualTime.setTextColor(Color.parseColor("#FF9800"));
    }

    private static final class ViewHolder {
        final TextView tvScheduledTime;
        final TextView tvHistoryMedName;
        final TextView tvActualTime;
        final View viewHistoryBadge;
        final TextView tvHistoryStatus;
        final TextView tvDelayInfo;

        ViewHolder(View itemView) {
            tvScheduledTime = itemView.findViewById(R.id.tvScheduledTime);
            tvHistoryMedName = itemView.findViewById(R.id.tvHistoryMedName);
            tvActualTime = itemView.findViewById(R.id.tvActualTime);
            viewHistoryBadge = itemView.findViewById(R.id.viewHistoryBadge);
            tvHistoryStatus = itemView.findViewById(R.id.tvHistoryStatus);
            tvDelayInfo = itemView.findViewById(R.id.tvDelayInfo);
        }
    }
}
