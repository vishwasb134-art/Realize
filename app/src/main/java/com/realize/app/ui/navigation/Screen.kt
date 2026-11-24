package com.realize.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class representing each top-level destination in the bottom navigation bar.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Default.TaskAlt)
    data object Schedule : Screen("schedule", "Schedule", Icons.Default.Schedule)
    data object Usage : Screen("usage", "Usage", Icons.Default.HourglassBottom)
    data object Places : Screen("places", "Places", Icons.Default.Place)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val items: List<Screen>
            get() = listOf(Home, Schedule, Usage, Places, Settings)
    }
}
