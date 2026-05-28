package com.meditrack.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.meditrack.app.db.DatabaseHelper;
import com.meditrack.app.db.MedicationDao;
import com.meditrack.app.services.AlarmScheduler;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            MedicationDao dao = new MedicationDao(DatabaseHelper.getInstance(context));
            AlarmScheduler.scheduleAllAlarms(context.getApplicationContext(), dao);
        }
    }
}
