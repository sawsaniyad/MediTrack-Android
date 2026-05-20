package com.meditrack.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meditrack.app.database.DatabaseHelper
import com.meditrack.app.database.IntakeLogDAO
import com.meditrack.app.models.IntakeLog
import com.meditrack.app.notifications.NotificationHelper
import com.meditrack.app.utils.AlarmScheduler
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * AlarmReceiver - מקבל אירועי שעון מעורר ופעולות מהתראה
 * אחראית: רע'ד
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_MED_ID = "med_id"
        const val EXTRA_MED_NAME = "med_name"
        const val EXTRA_MED_DOSAGE = "med_dosage"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_LOG_ID = "log_id"
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // התראה חדשה - הגיע זמן לקחת תרופה
            Intent.ACTION_BOOT_COMPLETED -> {
                // לאחר אתחול המכשיר - קבע מחדש את כל האזעקות
                rescheduleAllAlarms(context)
            }

            NotificationHelper.ACTION_TAKEN -> {
                val logId = intent.getLongExtra(EXTRA_LOG_ID, -1L)
                if (logId != -1L) markAsTaken(context, logId)
            }

            NotificationHelper.ACTION_SNOOZE -> {
                val logId = intent.getLongExtra(EXTRA_LOG_ID, -1L)
                if (logId != -1L) snoozeReminder(context, logId)
            }

            else -> {
                // אזעקה רגילה - הצג התראה
                val medId = intent.getLongExtra(EXTRA_MED_ID, -1L)
                val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: return
                val medDosage = intent.getStringExtra(EXTRA_MED_DOSAGE) ?: ""
                val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)

                if (medId != -1L) fireReminder(context, medId, medName, medDosage, scheduleId)
            }
        }
    }

    private fun fireReminder(
        context: Context,
        medId: Long,
        medName: String,
        dosage: String,
        scheduleId: Long
    ) {
        executor.execute {
            val dbHelper = DatabaseHelper(context)
            val logDAO = IntakeLogDAO(dbHelper)

            // צור רשומת יומן
            val logId = logDAO.insert(
                IntakeLog(
                    medicationId = medId,
                    scheduledTime = System.currentTimeMillis(),
                    status = IntakeLog.Status.PENDING
                )
            )

            NotificationHelper.showMedicationReminder(context, logId, medName, dosage)
        }
    }

    private fun markAsTaken(context: Context, logId: Long) {
        executor.execute {
            val dbHelper = DatabaseHelper(context)
            val logDAO = IntakeLogDAO(dbHelper)
            logDAO.updateStatus(logId, IntakeLog.Status.TAKEN, System.currentTimeMillis())
            NotificationHelper.cancelNotification(context, logId)
        }
    }

    private fun snoozeReminder(context: Context, logId: Long) {
        executor.execute {
            val dbHelper = DatabaseHelper(context)
            val logDAO = IntakeLogDAO(dbHelper)
            logDAO.updateStatus(logId, IntakeLog.Status.SNOOZED)
            NotificationHelper.cancelNotification(context, logId)

            // קבע מחדש עוד 10 דקות
            AlarmScheduler.scheduleSnooze(context, logId, delayMinutes = 10)
        }
    }

    private fun rescheduleAllAlarms(context: Context) {
        executor.execute {
            AlarmScheduler.rescheduleAll(context)
        }
    }
}
