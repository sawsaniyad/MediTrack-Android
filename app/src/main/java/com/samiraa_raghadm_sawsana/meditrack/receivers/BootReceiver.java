package com.samiraa_raghadm_sawsana.meditrack.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.samiraa_raghadm_sawsana.meditrack.helpers.AppExecutors;
import com.samiraa_raghadm_sawsana.meditrack.database.DatabaseHelper;
import com.samiraa_raghadm_sawsana.meditrack.database.MedicationDAO;
import com.samiraa_raghadm_sawsana.meditrack.helpers.AlarmScheduler;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || "android.intent.action.TIME_SET".equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            // FIXED: moved to diskIO — AlarmScheduler.scheduleAllAlarms uses diskIO
            // internally
            AppExecutors.getInstance().diskIO(() -> {
                MedicationDAO dao = new MedicationDAO(DatabaseHelper.getInstance(context));
                AlarmScheduler.scheduleAllAlarms(context.getApplicationContext(), dao);
            });
        }
    }
}
