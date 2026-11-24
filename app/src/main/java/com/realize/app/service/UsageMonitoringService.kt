package com.realize.app.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.realize.app.data.PreferencesManager
import com.realize.app.data.RealizeDatabase
import com.realize.app.data.StudyBlockRepository
import com.realize.app.data.UsageStatsHelper
import com.realize.app.notifications.NotificationHelper
import com.realize.app.ui.screens.schedule.LockScreenActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service running in the background to monitor:
 * 1. Social media app screen time (30-minute alerts).
 * 2. Active study schedule time: locks Instagram and games during study hours.
 *
 * Declared with Android 14+ foregroundServiceType="specialUse".
 */
class UsageMonitoringService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var currentActivePackage: String? = null
    private var continuousStartMillis: Long = 0L
    private var lastAlertTimestamp: Long = 0L

    private lateinit var prefs: PreferencesManager
    private lateinit var studyBlockRepo: StudyBlockRepository

    companion object {
        var isServiceRunning: Boolean = false
            private set

        @Volatile
        var snoozeUntilMillis: Long = 0L

        fun startService(context: Context) {
            val intent = Intent(context, UsageMonitoringService::class.java)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, UsageMonitoringService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        prefs = PreferencesManager(this)
        val db = RealizeDatabase.getDatabase(this)
        studyBlockRepo = StudyBlockRepository(db.studyBlockDao())

        // Promote to foreground service immediately
        val initialNotification = NotificationHelper.buildServiceNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID_SERVICE,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID_SERVICE, initialNotification)
            }
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID_SERVICE, initialNotification)
        }

        startMonitoringLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val inStudySession = checkStudyBlockAndLockApps()

                    // If in study session, check frequently (every 1.5 seconds) for instantaneous app locking
                    // If not in study session, check every 15 seconds for social limits
                    if (inStudySession) {
                        delay(1500L)
                    } else {
                        checkSocialMediaUsage()
                        delay(15_000L)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(5_000L)
                }
            }
        }
    }

    /**
     * Checks if current time is within an active study schedule block.
     * If so, and the foreground app is Instagram, a game, or in the locked list,
     * immediately displays the LockScreenActivity.
     *
     * @return true if currently in an active study block, false otherwise.
     */
    private suspend fun checkStudyBlockAndLockApps(): Boolean {
        if (!prefs.isLockerEnabled) return false
        if (!UsageStatsHelper.hasUsageAccessPermission(this)) return false

        val activeBlock = studyBlockRepo.getActiveStudyBlockNow() ?: return false

        val foregroundPackage = UsageStatsHelper.getForegroundApp(this) ?: return true

        // Do not lock Realize app itself
        if (foregroundPackage == packageName) return true

        val shouldLock = prefs.lockedPackages.contains(foregroundPackage) ||
                (prefs.lockAllGames && UsageStatsHelper.isGame(this, foregroundPackage))

        if (shouldLock) {
            // Immediately launch LockScreenActivity
            val lockIntent = LockScreenActivity.createIntent(
                context = this,
                packageName = foregroundPackage,
                studyTitle = activeBlock.title,
                endHour = activeBlock.endHour,
                endMinute = activeBlock.endMinute
            )
            startActivity(lockIntent)
        }

        updateServiceNotification("Focus Active: ${activeBlock.title} • Instagram & Games locked")
        return true
    }

    /**
     * Checks continuous and total daily usage on social media apps.
     */
    private suspend fun checkSocialMediaUsage() {
        if (!UsageStatsHelper.hasUsageAccessPermission(this)) return

        val limitMinutes = prefs.socialLimitMinutes
        val selectedPackages = prefs.selectedSocialPackages

        val foregroundPackage = UsageStatsHelper.getForegroundApp(this) ?: return
        val now = System.currentTimeMillis()

        if (selectedPackages.contains(foregroundPackage)) {
            if (currentActivePackage == foregroundPackage) {
                // Continued session
            } else {
                // New session started on this social app
                currentActivePackage = foregroundPackage
                continuousStartMillis = now
            }

            val continuousDurationMinutes = ((now - continuousStartMillis) / (60 * 1000)).toInt()

            // Read total screen time today for this app
            val todayStats = UsageStatsHelper.getTodaySocialUsage(this, setOf(foregroundPackage))
            val appUsage = todayStats.firstOrNull()
            val totalTodayMinutes = appUsage?.totalMinutes ?: continuousDurationMinutes

            // Update persistent service notification with live time
            val (appName, _) = UsageStatsHelper.getAppDetails(packageManager, foregroundPackage)
            updateServiceNotification("Monitoring: $appName open ($totalTodayMinutes min today)")

            // Check if limit is reached (continuous OR total daily usage)
            if (continuousDurationMinutes >= limitMinutes || totalTodayMinutes >= limitMinutes) {
                // Check if user has snoozed alerts
                if (now < snoozeUntilMillis) {
                    return
                }

                // Check 15-minute repeat nudge interval
                val fifteenMinutes = 15 * 60 * 1000L
                if (now - lastAlertTimestamp >= fifteenMinutes) {
                    lastAlertTimestamp = now

                    // Query nearest 2 pending assignments
                    val db = RealizeDatabase.getDatabase(this)
                    val nearest2 = db.assignmentDao().getTop2NearestPending()

                    val usedMinutesToReport = maxOf(continuousDurationMinutes, totalTodayMinutes)

                    NotificationHelper.showSocialLimitNotification(
                        context = this,
                        appName = appName,
                        usedMinutes = usedMinutesToReport,
                        nearestAssignments = nearest2
                    )
                }
            }
        } else {
            // User exited social app
            if (currentActivePackage != null) {
                currentActivePackage = null
                continuousStartMillis = 0L
                updateServiceNotification("Realize Focus Guard active • Monitoring screen time")
            }
        }
    }

    private fun updateServiceNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val updatedNotification = NotificationHelper.buildServiceNotification(this, text)
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID_SERVICE, updatedNotification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceJob.cancel()
    }
}
