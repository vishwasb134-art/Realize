package com.realize.app.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Data model for individual app usage statistics.
 */
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val totalTimeForegroundMillis: Long,
    val isSelectedSocial: Boolean = false,
    val isGame: Boolean = false
) {
    val totalMinutes: Int
        get() = (totalTimeForegroundMillis / (1000 * 60)).toInt()

    val formattedTime: String
        get() {
            val totalSeconds = totalTimeForegroundMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                totalSeconds > 0 -> "${totalSeconds}s"
                else -> "0m"
            }
        }
}

/**
 * Data model for daily bar chart (e.g. 7 days).
 */
data class DailyUsageBar(
    val dayLabel: String, // e.g. "Mon", "Tue"
    val dateLabel: String, // e.g. "Nov 23"
    val totalMinutes: Int,
    val isToday: Boolean
)

/**
 * Helper class for interacting with Android's UsageStatsManager, PackageManager, and Game Detection.
 */
object UsageStatsHelper {

    /**
     * Checks if the user has granted the PACKAGE_USAGE_STATS permission.
     */
    fun hasUsageAccessPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Checks whether a package is recognized by Android as a Game.
     */
    fun isGame(context: Context, packageName: String): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appInfo.category == ApplicationInfo.CATEGORY_GAME
            } else {
                @Suppress("DEPRECATION")
                (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            }
        } catch (e: Exception) {
            // Check common game package naming heuristics if metadata is missing
            val lower = packageName.lowercase()
            lower.contains(".game") || lower.contains(".games") || lower.contains("supercell") ||
                lower.contains("king.candycrush") || lower.contains("subwaysurf") || lower.contains("pubg") ||
                lower.contains("freefire") || lower.contains("roblox") || lower.contains("minecraft")
        }
    }

    /**
     * Reads today's foreground usage for the user-selected social applications.
     */
    fun getTodaySocialUsage(context: Context, selectedPackages: Set<String>): List<AppUsageInfo> {
        if (!hasUsageAccessPermission(context)) return emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager

        // Today start (midnight) to now
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return emptyList()

        val aggregatedMap = mutableMapOf<String, Long>()
        for (stats in usageStatsList) {
            val existing = aggregatedMap[stats.packageName] ?: 0L
            if (stats.totalTimeInForeground > existing) {
                aggregatedMap[stats.packageName] = stats.totalTimeInForeground
            }
        }

        val result = mutableListOf<AppUsageInfo>()
        for (pkg in selectedPackages) {
            val timeMillis = aggregatedMap[pkg] ?: 0L
            val (appName, icon) = getAppDetails(packageManager, pkg)
            result.add(
                AppUsageInfo(
                    packageName = pkg,
                    appName = appName,
                    icon = icon,
                    totalTimeForegroundMillis = timeMillis,
                    isSelectedSocial = true,
                    isGame = isGame(context, pkg)
                )
            )
        }

        return result.sortedByDescending { it.totalTimeForegroundMillis }
    }

    /**
     * Reads screen time history for the past 7 days for the selected social apps.
     */
    fun getPast7DaysUsage(context: Context, selectedPackages: Set<String>): List<DailyUsageBar> {
        if (!hasUsageAccessPermission(context)) return emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        val result = mutableListOf<DailyUsageBar>()

        for (daysAgo in 6 downTo 0) {
            val startCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                timeInMillis = startCal.timeInMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            val statsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startCal.timeInMillis,
                endCal.timeInMillis
            )

            var dayTotalMillis = 0L
            if (statsList != null) {
                for (stats in statsList) {
                    if (stats.packageName in selectedPackages) {
                        dayTotalMillis += stats.totalTimeInForeground
                    }
                }
            }

            result.add(
                DailyUsageBar(
                    dayLabel = dayFormat.format(startCal.time),
                    dateLabel = dateFormat.format(startCal.time),
                    totalMinutes = (dayTotalMillis / (1000 * 60)).toInt(),
                    isToday = (daysAgo == 0)
                )
            )
        }

        return result
    }

    /**
     * Identifies the application currently in the foreground by reading the most recent MOVE_TO_FOREGROUND or ACTIVITY_RESUMED event.
     */
    fun getForegroundApp(context: Context): String? {
        if (!hasUsageAccessPermission(context)) return null

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        // Query recent 1 minute events for high responsiveness
        val events = usageStatsManager.queryEvents(now - 60 * 1000, now)

        val event = UsageEvents.Event()
        var lastForegroundApp: String? = null
        var lastEventTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                if (event.timeStamp > lastEventTime) {
                    lastEventTime = event.timeStamp
                    lastForegroundApp = event.packageName
                }
            }
        }

        return lastForegroundApp
    }

    /**
     * Lists launcher apps installed on the device, annotated with game detection.
     */
    fun getInstalledLauncherApps(context: Context, selectedPackages: Set<String>): List<AppUsageInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)

        val appList = mutableListOf<AppUsageInfo>()
        val seen = mutableSetOf<String>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName || seen.contains(pkg)) continue
            seen.add(pkg)

            val appName = resolveInfo.loadLabel(packageManager).toString()
            val icon = resolveInfo.loadIcon(packageManager)
            val game = isGame(context, pkg)

            appList.add(
                AppUsageInfo(
                    packageName = pkg,
                    appName = appName,
                    icon = icon,
                    totalTimeForegroundMillis = 0L,
                    isSelectedSocial = selectedPackages.contains(pkg),
                    isGame = game
                )
            )
        }

        return appList.sortedWith(
            compareByDescending<AppUsageInfo> { it.isSelectedSocial }
                .thenByDescending { it.isGame }
                .thenBy { it.appName.lowercase() }
        )
    }

    fun getAppDetails(packageManager: PackageManager, packageName: String): Pair<String, Drawable?> {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val name = packageManager.getApplicationLabel(appInfo).toString()
            val icon = packageManager.getApplicationIcon(appInfo)
            Pair(name, icon)
        } catch (e: Exception) {
            val simpleName = when (packageName) {
                "com.instagram.android" -> "Instagram"
                "com.google.android.youtube" -> "YouTube"
                "com.snapchat.android" -> "Snapchat"
                "com.facebook.katana" -> "Facebook"
                "com.twitter.android" -> "X / Twitter"
                "com.reddit.frontpage" -> "Reddit"
                else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
            Pair(simpleName, null)
        }
    }
}
