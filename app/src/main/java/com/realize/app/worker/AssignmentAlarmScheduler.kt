package com.realize.app.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.realize.app.data.Assignment
import com.realize.app.data.PreferencesManager
import com.realize.app.data.RealizeDatabase
import com.realize.app.receiver.AssignmentReminderReceiver
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules and manages all assignment alarms and daily WorkManager background reminders.
 */
object AssignmentAlarmScheduler {

    private const val DAILY_WORK_TAG = "realize_daily_reminder_work"

    /**
     * Schedules or reschedules the daily WorkManager reminder at user's chosen hour/minute.
     */
    fun scheduleDailyReminder(context: Context) {
        val prefs = PreferencesManager(context)
        val targetHour = prefs.reminderHour
        val targetMinute = prefs.reminderMinute

        val now = Calendar.getInstance()
        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelayMillis = targetCalendar.timeInMillis - now.timeInMillis

        val dailyRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .addTag(DAILY_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyRequest
        )
    }

    /**
     * Schedules two reminders for an individual assignment:
     * 1. One day before due date at 9:00 AM.
     * 2. On the morning of due date at 8:00 AM.
     */
    fun scheduleAssignmentReminders(context: Context, assignment: Assignment) {
        if (assignment.isCompleted) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. One day before reminder (at 9:00 AM the day before due date)
        val oneDayBeforeCalendar = Calendar.getInstance().apply {
            timeInMillis = assignment.dueDateTimeMillis
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (oneDayBeforeCalendar.timeInMillis > System.currentTimeMillis()) {
            val pendingIntent = createAlarmPendingIntent(
                context,
                assignment.id,
                AssignmentReminderReceiver.TYPE_ONE_DAY_BEFORE,
                (assignment.id * 10 + 1).toInt()
            )
            setExactAlarm(alarmManager, oneDayBeforeCalendar.timeInMillis, pendingIntent)
        }

        // 2. Morning of due date reminder (at 8:00 AM on the day of due date)
        val morningOfCalendar = Calendar.getInstance().apply {
            timeInMillis = assignment.dueDateTimeMillis
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Only schedule if the alarm time is still in the future AND before the actual due time
        if (morningOfCalendar.timeInMillis > System.currentTimeMillis() &&
            morningOfCalendar.timeInMillis < assignment.dueDateTimeMillis) {
            val pendingIntent = createAlarmPendingIntent(
                context,
                assignment.id,
                AssignmentReminderReceiver.TYPE_MORNING_OF,
                (assignment.id * 10 + 2).toInt()
            )
            setExactAlarm(alarmManager, morningOfCalendar.timeInMillis, pendingIntent)
        }
    }

    /**
     * Cancels any pending individual alarms for the given assignment ID.
     */
    fun cancelAssignmentReminders(context: Context, assignmentId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val piOneDay = createAlarmPendingIntent(
            context,
            assignmentId,
            AssignmentReminderReceiver.TYPE_ONE_DAY_BEFORE,
            (assignmentId * 10 + 1).toInt()
        )
        alarmManager.cancel(piOneDay)
        piOneDay.cancel()

        val piMorning = createAlarmPendingIntent(
            context,
            assignmentId,
            AssignmentReminderReceiver.TYPE_MORNING_OF,
            (assignmentId * 10 + 2).toInt()
        )
        alarmManager.cancel(piMorning)
        piMorning.cancel()
    }

    /**
     * Reschedules all pending assignment alarms (e.g. called after phone reboot).
     */
    suspend fun rescheduleAllPendingAssignments(context: Context) {
        val db = RealizeDatabase.getDatabase(context)
        val pendingAssignments = db.assignmentDao().getAllPendingList()
        pendingAssignments.forEach { assignment ->
            scheduleAssignmentReminders(context, assignment)
        }
    }

    private fun setExactAlarm(alarmManager: AlarmManager, timeMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        }
    }

    private fun createAlarmPendingIntent(
        context: Context,
        assignmentId: Long,
        type: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, AssignmentReminderReceiver::class.java).apply {
            putExtra(AssignmentReminderReceiver.EXTRA_ASSIGNMENT_ID, assignmentId)
            putExtra(AssignmentReminderReceiver.EXTRA_REMINDER_TYPE, type)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
