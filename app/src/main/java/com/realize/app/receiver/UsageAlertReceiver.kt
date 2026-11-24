package com.realize.app.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.realize.app.notifications.NotificationHelper
import com.realize.app.service.UsageMonitoringService

/**
 * Handles the "Snooze 10 min" action from the social media usage alert notification.
 */
class UsageAlertReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE_10_MIN = "com.realize.app.ACTION_SNOOZE_10_MIN"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SNOOZE_10_MIN) {
            // 1. Set snooze for 10 minutes in the running monitoring service
            val snoozeUntil = System.currentTimeMillis() + 10 * 60 * 1000
            UsageMonitoringService.snoozeUntilMillis = snoozeUntil

            // 2. Dismiss the alert notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_SOCIAL_LIMIT)
        }
    }
}
