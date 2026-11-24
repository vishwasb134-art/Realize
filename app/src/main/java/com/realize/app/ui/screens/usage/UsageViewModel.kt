package com.realize.app.ui.screens.usage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.realize.app.data.AppUsageInfo
import com.realize.app.data.DailyUsageBar
import com.realize.app.data.PreferencesManager
import com.realize.app.data.UsageStatsHelper
import com.realize.app.service.UsageMonitoringService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UsageUiState(
    val hasPermission: Boolean = false,
    val todayApps: List<AppUsageInfo> = emptyList(),
    val weeklyBars: List<DailyUsageBar> = emptyList(),
    val installedApps: List<AppUsageInfo> = emptyList(),
    val totalTodayMinutes: Int = 0,
    val limitMinutes: Int = 30,
    val selectedPackages: Set<String> = emptySet(),
    val isServiceActive: Boolean = false,
    val isLoading: Boolean = true
)

class UsageViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val _uiState = MutableStateFlow(UsageUiState())
    val uiState: StateFlow<UsageUiState> = _uiState.asStateFlow()

    init {
        refreshUsageData()
    }

    fun refreshUsageData() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val hasPerm = UsageStatsHelper.hasUsageAccessPermission(context)
            val selectedPkgs = prefs.selectedSocialPackages
            val limit = prefs.socialLimitMinutes

            if (hasPerm) {
                // Ensure foreground service is running
                if (!UsageMonitoringService.isServiceRunning) {
                    UsageMonitoringService.startService(context)
                }

                val todayList = UsageStatsHelper.getTodaySocialUsage(context, selectedPkgs)
                val weeklyList = UsageStatsHelper.getPast7DaysUsage(context, selectedPkgs)
                val totalMin = todayList.sumOf { it.totalMinutes }

                _uiState.update {
                    it.copy(
                        hasPermission = true,
                        todayApps = todayList,
                        weeklyBars = weeklyList,
                        totalTodayMinutes = totalMin,
                        limitMinutes = limit,
                        selectedPackages = selectedPkgs,
                        isServiceActive = UsageMonitoringService.isServiceRunning,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        hasPermission = false,
                        limitMinutes = limit,
                        selectedPackages = selectedPkgs,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val installed = UsageStatsHelper.getInstalledLauncherApps(context, prefs.selectedSocialPackages)
            _uiState.update { it.copy(installedApps = installed) }
        }
    }

    fun toggleAppSocial(packageName: String, isSocial: Boolean) {
        val currentSet = prefs.selectedSocialPackages.toMutableSet()
        if (isSocial) {
            currentSet.add(packageName)
        } else {
            currentSet.remove(packageName)
        }
        prefs.selectedSocialPackages = currentSet
        refreshUsageData()
        loadInstalledApps()
    }

    fun updateLimit(minutes: Int) {
        prefs.socialLimitMinutes = minutes
        _uiState.update { it.copy(limitMinutes = minutes) }
    }
}
