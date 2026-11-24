package com.realize.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user preferences and settings via SharedPreferences.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("realize_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_SOCIAL_LIMIT_MINUTES = "social_limit_minutes"
        private const val KEY_SELECTED_SOCIAL_PACKAGES = "selected_social_packages"
        private const val KEY_SMART_MESSAGES_ENABLED = "smart_messages_enabled"
        private const val KEY_SAVINGS_GOAL = "savings_goal"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_THEME_MODE = "theme_mode" // 0: System, 1: Light, 2: Dark

        // Study Schedule App Locker
        private const val KEY_LOCKER_ENABLED = "study_locker_enabled"
        private const val KEY_LOCKED_PACKAGES = "study_locked_packages"
        private const val KEY_LOCK_ALL_GAMES = "study_lock_all_games"

        // Default popular social media packages (WhatsApp is explicitly excluded as per spec)
        val DEFAULT_SOCIAL_PACKAGES = setOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.snapchat.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.reddit.frontpage"
        )

        // Default apps to strictly lock during study hours
        val DEFAULT_LOCKED_PACKAGES = setOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.snapchat.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.reddit.frontpage"
        )
    }

    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 8)
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value).apply()

    var reminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTE, value).apply()

    var socialLimitMinutes: Int
        get() = prefs.getInt(KEY_SOCIAL_LIMIT_MINUTES, 30)
        set(value) = prefs.edit().putInt(KEY_SOCIAL_LIMIT_MINUTES, value).apply()

    var selectedSocialPackages: Set<String>
        get() = prefs.getStringSet(KEY_SELECTED_SOCIAL_PACKAGES, DEFAULT_SOCIAL_PACKAGES) ?: DEFAULT_SOCIAL_PACKAGES
        set(value) = prefs.edit().putStringSet(KEY_SELECTED_SOCIAL_PACKAGES, value).apply()

    var isSmartMessagesEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMART_MESSAGES_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SMART_MESSAGES_ENABLED, value).apply()

    var savingsGoal: String
        get() = prefs.getString(KEY_SAVINGS_GOAL, "Save for my semester goals and avoid impulse buys.") ?: ""
        set(value) = prefs.edit().putString(KEY_SAVINGS_GOAL, value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0) // 0 = System, 1 = Light, 2 = Dark
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    // Study Schedule App Locker Settings
    var isLockerEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCKER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_LOCKER_ENABLED, value).apply()

    var lockedPackages: Set<String>
        get() = prefs.getStringSet(KEY_LOCKED_PACKAGES, DEFAULT_LOCKED_PACKAGES) ?: DEFAULT_LOCKED_PACKAGES
        set(value) = prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, value).apply()

    var lockAllGames: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ALL_GAMES, true)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_ALL_GAMES, value).apply()
}
