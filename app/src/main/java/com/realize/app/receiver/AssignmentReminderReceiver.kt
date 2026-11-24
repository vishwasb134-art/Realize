package com.realize.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.realize.app.data.RealizeDatabase
import com.realize.app.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Receives alarms for individual assignment reminders:
 * 1. One day before due date.
 * 2. On the morning of due date.
 */
class AssignmentReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ASSIGNMENT_ID = "extra_assignment_id"
        const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
        const val TYPE_ONE_DAY_BEFORE = "type_one_day_before"
        const val TYPE_MORNING_OF = "type_morning_of"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val assignmentId = intent.getLongExtra(EXTRA_ASSIGNMENT_ID, -1L)
        val reminderType = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: TYPE_MORNING_OF

        if (assignmentId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = RealizeDatabase.getDatabase(context)
                val assignment = db.assignmentDao().getAssignmentById(assignmentId)

                // Only send notification if assignment exists and is not yet completed
                if (assignment != null && !assignment.isCompleted) {
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val dueTimeStr = timeFormat.format(assignment.dueDateTimeMillis)

                    val (title, body) = when (reminderType) {
                        TYPE_ONE_DAY_BEFORE -> {
                            "⏳ Due Tomorrow: ${assignment.subject}" to
                                    "\"${assignment.title}\" is due tomorrow at $dueTimeStr. Finish early and stay ahead!"
                        }
                        else -> {
                            "🚨 Due Today: ${assignment.subject}" to
                                    "\"${assignment.title}\" is due today at $dueTimeStr! Don't let it slip."
                        }
                    }

                    NotificationHelper.showAssignmentReminderNotification(
                        context = context,
                        notificationId = (assignment.id * 10 + if (reminderType == TYPE_ONE_DAY_BEFORE) 1 else 2).toInt(),
                        title = title,
                        content = body
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
