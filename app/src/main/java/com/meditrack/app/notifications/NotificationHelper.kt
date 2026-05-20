package com.meditrack.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.meditrack.app.R
import com.meditrack.app.activities.MedicationListActivity
import com.meditrack.app.receivers.AlarmReceiver

/**
 * NotificationHelper - יצירת ושליחת התראות
 * אחראית: רע'ד
 */
object NotificationHelper {

    const val CHANNEL_ID = "meditrack_reminders"
    const val CHANNEL_NAME = "תזכורות תרופות"
    const val ACTION_TAKEN = "com.meditrack.app.ACTION_TAKEN"
    const val ACTION_SNOOZE = "com.meditrack.app.ACTION_SNOOZE"
    const val EXTRA_LOG_ID = "log_id"
    const val EXTRA_MED_NAME = "med_name"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "תזכורות לנטילת תרופות"
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showMedicationReminder(context: Context, logId: Long, medicationName: String, dosage: String) {
        // Intent ל"נלקח"
        val takenIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TAKEN
            putExtra(EXTRA_LOG_ID, logId)
        }
        val takenPending = PendingIntent.getBroadcast(
            context, logId.toInt() * 10,
            takenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent ל"דחה"
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_LOG_ID, logId)
        }
        val snoozePending = PendingIntent.getBroadcast(
            context, logId.toInt() * 10 + 1,
            snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent לפתיחת האפליקציה
        val openIntent = Intent(context, MedicationListActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication)
            .setContentTitle("💊 זמן לקחת תרופה!")
            .setContentText("$medicationName - $dosage")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check, "נלקח ✓", takenPending)
            .addAction(R.drawable.ic_snooze, "דחה 10 דק'", snoozePending)
            .build()

        NotificationManagerCompat.from(context).notify(logId.toInt(), notification)
    }

    fun cancelNotification(context: Context, logId: Long) {
        NotificationManagerCompat.from(context).cancel(logId.toInt())
    }
}
