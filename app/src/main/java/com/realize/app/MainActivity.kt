package com.realize.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.realize.app.data.PreferencesManager
import com.realize.app.ui.navigation.Screen
import com.realize.app.ui.screens.home.AssignmentViewModel
import com.realize.app.ui.screens.home.HomeScreen
import com.realize.app.ui.screens.onboarding.OnboardingScreen
import com.realize.app.ui.screens.places.PlacesScreen
import com.realize.app.ui.screens.places.PlacesViewModel
import com.realize.app.ui.screens.schedule.ScheduleScreen
import com.realize.app.ui.screens.schedule.ScheduleViewModel
import com.realize.app.ui.screens.settings.SettingsScreen
import com.realize.app.ui.screens.usage.UsageScreen
import com.realize.app.ui.screens.usage.UsageViewModel
import com.realize.app.ui.theme.RealizeTheme

class MainActivity : ComponentActivity() {

    private val assignmentViewModel: AssignmentViewModel by viewModels()
    private val scheduleViewModel: ScheduleViewModel by viewModels()
    private val usageViewModel: UsageViewModel by viewModels()
    private val placesViewModel: PlacesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = PreferencesManager(this)

        setContent {
            var themeMode by remember { mutableIntStateOf(prefs.themeMode) }
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                1 -> false
                2 -> true
                else -> systemDark
            }

            var isOnboardingCompleted by remember { mutableStateOf(prefs.isOnboardingCompleted) }

            RealizeTheme(darkTheme = isDark) {
                if (!isOnboardingCompleted) {
                    OnboardingScreen(
                        onFinished = {
                            prefs.isOnboardingCompleted = true
                            isOnboardingCompleted = true
                        }
                    )
                } else {
                    RealizeMainApp(
                        assignmentViewModel = assignmentViewModel,
                        scheduleViewModel = scheduleViewModel,
                        usageViewModel = usageViewModel,
                        placesViewModel = placesViewModel,
                        onThemeChanged = { newMode -> themeMode = newMode }
                    )
                }
            }
        }
    }
}

@Composable
fun RealizeMainApp(
    assignmentViewModel: AssignmentViewModel,
    scheduleViewModel: ScheduleViewModel,
    usageViewModel: UsageViewModel,
    placesViewModel: PlacesViewModel,
    onThemeChanged: (Int) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Screen.items.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(imageVector = screen.icon, contentDescription = screen.title)
                        },
                        label = { Text(screen.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(viewModel = assignmentViewModel)
            }
            composable(Screen.Schedule.route) {
                ScheduleScreen(viewModel = scheduleViewModel)
            }
            composable(Screen.Usage.route) {
                UsageScreen(viewModel = usageViewModel)
            }
            composable(Screen.Places.route) {
                PlacesScreen(viewModel = placesViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onThemeChanged = onThemeChanged)
            }
        }
    }
}
