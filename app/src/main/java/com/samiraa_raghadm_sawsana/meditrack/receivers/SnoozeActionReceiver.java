package com.samiraa_raghadm_sawsana.meditrack.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

public class SnoozeActionReceiver extends BroadcastReceiver {

    public static final String EXTRA_FROM_SNOOZE = "FROM_SNOOZE";

    @Override
    public void onReceive(Context context, Intent intent) {
        int medicationId = intent.getIntExtra("MEDICATION_ID", -1);
        int scheduleId   = intent.getIntExtra("SCHEDULE_ID", -1);
        String medicationName     = intent.getStringExtra("MEDICATION_NAME");
        String dosage             = intent.getStringExtra("DOSAGE");
        int    notificationId     = intent.getIntExtra("NOTIFICATION_ID", -1);
        String scheduledDatetime  = intent.getStringExtra(AlarmReceiver.EXTRA_SCHEDULED_DATETIME);

        if (medicationId == -1) return;

        // Cancel the ongoing notification — the picker dialog replaces it.
        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId);
        }

        // All other side-effects (clear window, cancel missed-check, mark SNOOZED)
        // happen only when the user confirms a duration in SnoozePickerActivity.
        Intent pickerIntent = new Intent(context,
                com.samiraa_raghadm_sawsana.meditrack.activities.SnoozePickerActivity.class);
        pickerIntent.putExtra("MEDICATION_ID", medicationId);
        pickerIntent.putExtra("MEDICATION_NAME", medicationName);
        pickerIntent.putExtra("DOSAGE", dosage);
        pickerIntent.putExtra("SCHEDULE_ID", scheduleId);
        pickerIntent.putExtra(AlarmReceiver.EXTRA_SCHEDULED_DATETIME, scheduledDatetime);
        pickerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(pickerIntent);
    }
}
