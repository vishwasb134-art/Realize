package com.realize.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.realize.app.MainActivity
import com.realize.app.R
import com.realize.app.data.Assignment
import com.realize.app.receiver.UsageAlertReceiver
import com.realize.app.ui.components.calculateDaysLeft

/**
 * Handles creation of notification channels and building user-facing notifications.
 * Appends ' for conciousness' to all reminders and notifications as requested by the user.
 */
object NotificationHelper {

    const val CHANNEL_ASSIGNMENTS = "channel_assignments"
    const val CHANNEL_SOCIAL_LIMIT = "channel_social_limit"
    const val CHANNEL_PLACES = "channel_places"
    const val CHANNEL_SERVICE = "channel_service"

    const val NOTIFICATION_ID_SERVICE = 1001
    const val NOTIFICATION_ID_DAILY_REMINDER = 1002
    const val NOTIFICATION_ID_SOCIAL_LIMIT = 1003
    const val NOTIFICATION_ID_PLACE_ALERT = 1004

    private const val CONSCIOUSNESS_SUFFIX = " for conciousness"

    /**
     * Helper to guarantee that every notification message ends with ' for conciousness'.
     */
    fun withConsciousness(text: String): String {
        val trimmed = text.trim()
        return if (trimmed.endsWith("for conciousness", ignoreCase = true)) {
            trimmed
        } else {
            "$trimmed$CONSCIOUSNESS_SUFFIX"
        }
    }

    /**
     * Creates all notification channels required by the app.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Assignments Channel
            val assignmentChannel = NotificationChannel(
                CHANNEL_ASSIGNMENTS,
                "Assignments & Due Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for assignments due today, tomorrow, and morning reminders"
                enableVibration(true)
            }

            // 2. Social Limit Channel
            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL_LIMIT,
                "Social Media Limits",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when continuous or daily social app usage exceeds limit"
                enableVibration(true)
            }

            // 3. Places / Avoid Zones Channel
            val placesChannel = NotificationChannel(
                CHANNEL_PLACES,
                "Avoid Zones & Near Shop Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when entering shopping or temptation avoid zones"
                enableVibration(true)
            }

            // 4. Foreground Service Channel
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Realize Background Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Realize active to monitor social media time and study schedule"
                setShowBadge(false)
            }

            manager.createNotificationChannels(
                listOf(assignmentChannel, socialChannel, placesChannel, serviceChannel)
            )
        }
    }

    /**
     * Builds persistent low-priority notification for the Foreground Service.
     */
    fun buildServiceNotification(
        context: Context,
        appMonitoredText: String = "Monitoring social app screen time & study schedule"
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_SERVICE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayText = withConsciousness(appMonitoredText)

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Realize Focus Guard Active")
            .setContentText(displayText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Builds and displays an assignment reminder notification.
     */
    fun showAssignmentReminderNotification(
        context: Context,
        notificationId: Int,
        title: String,
        content: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val finalContent = withConsciousness(content)

        val notification = NotificationCompat.Builder(context, CHANNEL_ASSIGNMENTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(finalContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(finalContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    /**
     * Shows morning daily overview notification.
     */
    fun showDailyOverviewNotification(
        context: Context,
        title: String,
        content: String,
        bigSummary: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_DAILY_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val finalContent = withConsciousness(content)
        val finalSummary = withConsciousness(bigSummary)

        val notification = NotificationCompat.Builder(context, CHANNEL_ASSIGNMENTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(finalContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(finalSummary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_DAILY_REMINDER, notification)
    }

    /**
     * Shows high-priority Social Limit Alert with nearest 2 pending assignments and Snooze 10 min action.
     */
    fun showSocialLimitNotification(
        context: Context,
        appName: String,
        usedMinutes: Int,
        nearestAssignments: List<Assignment>,
        customMessage: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_SOCIAL_LIMIT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 10 min Action Intent
        val snoozeIntent = Intent(context, UsageAlertReceiver::class.java).apply {
            action = UsageAlertReceiver.ACTION_SNOOZE_10_MIN
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_SOCIAL_LIMIT,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val baseMessage = customMessage ?: "You have used $appName for $usedMinutes+ minutes. Stop and look at your pending work."

        val bigText = buildString {
            append(baseMessage)
            if (nearestAssignments.isNotEmpty()) {
                append("\n\nYour nearest pending assignments:\n")
                nearestAssignments.forEach { task ->
                    val daysLeft = calculateDaysLeft(task.dueDateTimeMillis, task.isCompleted).label
                    append("• [${task.subject}] ${task.title} ($daysLeft)\n")
                }
            } else {
                append("\n\n(No urgent assignments due, but consider a productive screen break!)")
            }
        }

        val finalContent = withConsciousness(baseMessage)
        val finalBigText = withConsciousness(bigText)

        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL_LIMIT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚨 $appName Limit Reached ($usedMinutes min)")
            .setContentText(finalContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(finalBigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Snooze 10 min",
                snoozePendingIntent
            )
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_SOCIAL_LIMIT, notification)
    }

    /**
     * Shows notification when near an avoid zone (near-shop alert).
     * Guaranteed to end with ' for conciousness'.
     */
    fun showAvoidZoneNotification(
        context: Context,
        zoneName: String,
        savingsGoal: String,
        customMessage: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_PLACE_ALERT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rawText = customMessage ?: "You are near $zoneName. You do not need to buy anything. Go back to your goal."
        val bigText = buildString {
            append(rawText)
            if (savingsGoal.isNotBlank()) {
                append("\n\n🎯 Savings Goal: $savingsGoal")
            }
        }

        val finalContent = withConsciousness(rawText)
        val finalBigText = withConsciousness(bigText)

        val notification = NotificationCompat.Builder(context, CHANNEL_PLACES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🛍️ Near Avoid Zone: $zoneName")
            .setContentText(finalContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(finalBigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_PLACE_ALERT, notification)
    }
}
