package com.realize.app

import android.app.Application
import com.realize.app.notifications.NotificationHelper
import com.realize.app.worker.AssignmentAlarmScheduler
import org.osmdroid.config.Configuration

/**
 * Main Application class for Realize.
 * Initializes notification channels, WorkManager schedules, and OSMDroid configurations.
 */
class RealizeApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Create all required notification channels
        NotificationHelper.createNotificationChannels(this)

        // 2. Configure OSMDroid user agent for map loading
        Configuration.getInstance().userAgentValue = packageName

        // 3. Ensure the daily morning assignment reminder worker is scheduled
        AssignmentAlarmScheduler.scheduleDailyReminder(this)
    }
}
