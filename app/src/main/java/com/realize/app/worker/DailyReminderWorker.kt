package com.realize.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.realize.app.data.RealizeDatabase
import com.realize.app.notifications.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Background WorkManager worker that runs every morning to notify the student
 * of all assignments due today and tomorrow.
 */
class DailyReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = RealizeDatabase.getDatabase(context)
            val dao = database.assignmentDao()

            // Calculate start of today and end of tomorrow
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_YEAR, 2)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfTomorrow = calendar.timeInMillis

            val pendingList = dao.getPendingBetween(startOfToday, endOfTomorrow)

            if (pendingList.isNotEmpty()) {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

                val calendarTodayEnd = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis

                val todayTasks = pendingList.filter { it.dueDateTimeMillis <= calendarTodayEnd }
                val tomorrowTasks = pendingList.filter { it.dueDateTimeMillis > calendarTodayEnd }

                val summaryBuilder = StringBuilder()
                summaryBuilder.append("Good morning! Here is your plan:\n\n")

                if (todayTasks.isNotEmpty()) {
                    summaryBuilder.append("📌 DUE TODAY (${todayTasks.size}):\n")
                    todayTasks.forEach { task ->
                        summaryBuilder.append("• [${task.subject}] ${task.title} at ${timeFormat.format(task.dueDateTimeMillis)}\n")
                    }
                }

                if (tomorrowTasks.isNotEmpty()) {
                    if (todayTasks.isNotEmpty()) summaryBuilder.append("\n")
                    summaryBuilder.append("⏳ DUE TOMORROW (${tomorrowTasks.size}):\n")
                    tomorrowTasks.forEach { task ->
                        summaryBuilder.append("• [${task.subject}] ${task.title} at ${timeFormat.format(task.dueDateTimeMillis)}\n")
                    }
                }

                val title = "Realize: ${todayTasks.size} Due Today, ${tomorrowTasks.size} Due Tomorrow"
                val shortText = if (todayTasks.isNotEmpty()) {
                    "First up: [${todayTasks.first().subject}] ${todayTasks.first().title}"
                } else {
                    "${tomorrowTasks.size} upcoming assignments for tomorrow"
                }

                NotificationHelper.showDailyOverviewNotification(
                    context,
                    title,
                    shortText,
                    summaryBuilder.toString()
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
