package com.meditrack.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.meditrack.app.database.DatabaseHelper
import com.meditrack.app.database.MedicationDAO
import com.meditrack.app.database.ScheduleDAO
import com.meditrack.app.models.Schedule
import com.meditrack.app.receivers.AlarmReceiver
import java.util.Calendar

/**
 * AlarmScheduler - עוזר לקביעת ובטול אזעקות עם AlarmManager
 * אחראית: רע'ד (בשיתוף עם כלל הצוות)
 */
object AlarmScheduler {

    fun scheduleForMedication(context: Context, medicationId: Long, medName: String, dosage: String, schedules: List<Schedule>) {
        schedules.forEach { schedule ->
            scheduleSingle(context, medicationId, medName, dosage, schedule)
        }
    }

    private fun scheduleSingle(
        context: Context,
        medId: Long,
        medName: String,
        dosage: String,
        schedule: Schedule
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = buildAlarmIntent(context, medId, medName, dosage, schedule.id)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(medId, schedule.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = nextTriggerTime(schedule)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }

    fun cancelForMedication(context: Context, medicationId: Long, scheduleIds: List<Long>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduleIds.forEach { scheduleId ->
            val intent = Intent(context, AlarmReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                requestCode(medicationId, scheduleId),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pending?.let { alarmManager.cancel(it) }
        }
    }

    fun scheduleSnooze(context: Context, logId: Long, delayMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_LOG_ID, logId)
        }
        val pending = PendingIntent.getBroadcast(
            context, (logId + 100000).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + delayMinutes * 60 * 1000L
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    fun rescheduleAll(context: Context) {
        val dbHelper = DatabaseHelper(context)
        val medDAO = MedicationDAO(dbHelper)
        val schDAO = ScheduleDAO(dbHelper)

        medDAO.getActive().forEach { med ->
            val schedules = schDAO.getByMedication(med.id)
            scheduleForMedication(context, med.id, med.name, med.dosage, schedules)
        }
    }

    private fun nextTriggerTime(schedule: Schedule): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, schedule.hour)
            set(Calendar.MINUTE, schedule.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun buildAlarmIntent(context: Context, medId: Long, medName: String, dosage: String, scheduleId: Long) =
        Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_MED_ID, medId)
            putExtra(AlarmReceiver.EXTRA_MED_NAME, medName)
            putExtra(AlarmReceiver.EXTRA_MED_DOSAGE, dosage)
            putExtra(AlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }

    private fun requestCode(medId: Long, scheduleId: Long) = (medId * 1000 + scheduleId).toInt()
}
