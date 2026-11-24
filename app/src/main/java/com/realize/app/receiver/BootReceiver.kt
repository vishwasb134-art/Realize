package com.realize.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.realize.app.geofence.GeofenceManager
import com.realize.app.worker.AssignmentAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered automatically when the Android device finishes booting.
 * Reschedules all reminder alarms, geofences, and background services so the student stays protected.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // 1. Reschedule daily WorkManager morning summary
            AssignmentAlarmScheduler.scheduleDailyReminder(context)

            // 2. Restart Social Usage Monitoring & Study Guard Foreground Service
            if (com.realize.app.data.UsageStatsHelper.hasUsageAccessPermission(context)) {
                com.realize.app.service.UsageMonitoringService.startService(context)
            }

            // 3. Reschedule exact alarms for all active pending assignments
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    AssignmentAlarmScheduler.rescheduleAllPendingAssignments(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 4. Re-register all avoid zone geofences
            GeofenceManager.reRegisterAllGeofences(context)
        }
    }
}
