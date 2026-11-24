package com.realize.app.ui.screens.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.realize.app.data.AppUsageInfo
import com.realize.app.data.PreferencesManager
import com.realize.app.data.RealizeDatabase
import com.realize.app.data.StudyBlock
import com.realize.app.data.StudyBlockRepository
import com.realize.app.data.UsageStatsHelper
import com.realize.app.service.UsageMonitoringService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class ScheduleUiState(
    val studyBlocks: List<StudyBlock> = emptyList(),
    val activeBlockNow: StudyBlock? = null,
    val isLockerEnabled: Boolean = true,
    val lockAllGames: Boolean = true,
    val lockedPackages: Set<String> = emptySet(),
    val installedApps: List<AppUsageInfo> = emptyList(),
    val detectedGamesCount: Int = 0,
    val isUsageAccessGranted: Boolean = false,
    val strictAlertMessage: String? = null
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RealizeDatabase.getDatabase(application)
    private val repository = StudyBlockRepository(db.studyBlockDao())
    private val prefs = PreferencesManager(application)

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    val studyBlocksFlow = repository.getAllStudyBlocksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadSettings()
        refreshInstalledApps()
        checkActiveSessionNow()
    }

    fun loadSettings() {
        val hasUsage = UsageStatsHelper.hasUsageAccessPermission(getApplication())
        _uiState.value = _uiState.value.copy(
            isLockerEnabled = prefs.isLockerEnabled,
            lockAllGames = prefs.lockAllGames,
            lockedPackages = prefs.lockedPackages,
            isUsageAccessGranted = hasUsage
        )
    }

    fun checkActiveSessionNow() {
        viewModelScope.launch {
            val active = repository.getActiveStudyBlockNow()
            _uiState.value = _uiState.value.copy(activeBlockNow = active)
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val installed = UsageStatsHelper.getInstalledLauncherApps(app, prefs.lockedPackages)
            val gamesCount = installed.count { it.isGame }
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    installedApps = installed,
                    detectedGamesCount = gamesCount
                )
            }
        }
    }

    /**
     * Attempts to toggle the master locker switch.
     * STRICT COMMITMENT: If currently in an active study session, the locker cannot be disabled!
     */
    fun setLockerEnabled(enabled: Boolean) {
        val active = _uiState.value.activeBlockNow
        if (!enabled && active != null) {
            _uiState.value = _uiState.value.copy(
                strictAlertMessage = "🔒 Strict Focus Active: You cannot turn off the app locker until your study session (${active.title}) finishes for conciousness."
            )
            return
        }

        prefs.isLockerEnabled = enabled
        _uiState.value = _uiState.value.copy(isLockerEnabled = enabled)
        if (enabled) {
            UsageMonitoringService.startService(getApplication())
        }
    }

    /**
     * Attempts to toggle the lock all games switch.
     * STRICT COMMITMENT: Cannot unlock games during an active study session!
     */
    fun setLockAllGames(enabled: Boolean) {
        val active = _uiState.value.activeBlockNow
        if (!enabled && active != null) {
            _uiState.value = _uiState.value.copy(
                strictAlertMessage = "🔒 Strict Focus Active: You cannot unlock games during an active study session for conciousness."
            )
            return
        }

        prefs.lockAllGames = enabled
        _uiState.value = _uiState.value.copy(lockAllGames = enabled)
    }

    /**
     * Attempts to toggle an app lock.
     * STRICT COMMITMENT: Cannot remove an app from the locked list during an active study block!
     */
    fun toggleAppLocked(packageName: String) {
        val current = prefs.lockedPackages.toMutableSet()
        val isRemoving = current.contains(packageName)
        val active = _uiState.value.activeBlockNow

        if (isRemoving && active != null) {
            _uiState.value = _uiState.value.copy(
                strictAlertMessage = "🔒 Strict Focus Active: You cannot remove locks for apps during active study time for conciousness."
            )
            return
        }

        if (isRemoving) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        prefs.lockedPackages = current
        _uiState.value = _uiState.value.copy(lockedPackages = current)
    }

    fun addStudyBlock(
        title: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        isDailyRecurring: Boolean,
        forTomorrow: Boolean = false
    ) {
        viewModelScope.launch {
            val targetEpochDay = if (forTomorrow) {
                LocalDate.now().plusDays(1).toEpochDay()
            } else if (!isDailyRecurring) {
                LocalDate.now().toEpochDay()
            } else {
                0L
            }

            val newBlock = StudyBlock(
                title = title.ifBlank { "Study Session" },
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                isDailyRecurring = isDailyRecurring,
                targetDateEpochDay = targetEpochDay,
                isEnabled = true
            )
            repository.insertStudyBlock(newBlock)
            checkActiveSessionNow()
            UsageMonitoringService.startService(getApplication())
        }
    }

    /**
     * Attempts to toggle a study block on/off.
     * STRICT COMMITMENT: The currently active study block CANNOT be turned off!
     */
    fun toggleBlockEnabled(block: StudyBlock, enabled: Boolean) {
        val active = _uiState.value.activeBlockNow
        if (!enabled && active != null && active.id == block.id) {
            _uiState.value = _uiState.value.copy(
                strictAlertMessage = "🔒 Strict Focus Active: You cannot turn off this study time while it is currently in progress for conciousness."
            )
            return
        }

        viewModelScope.launch {
            repository.toggleEnabled(block.id, enabled)
            checkActiveSessionNow()
        }
    }

    /**
     * Attempts to delete a study block.
     * STRICT COMMITMENT: The currently active study block CANNOT be deleted!
     */
    fun deleteStudyBlock(block: StudyBlock) {
        val active = _uiState.value.activeBlockNow
        if (active != null && active.id == block.id) {
            _uiState.value = _uiState.value.copy(
                strictAlertMessage = "🔒 Strict Focus Active: You cannot delete this study time while it is currently in progress for conciousness."
            )
            return
        }

        viewModelScope.launch {
            repository.deleteStudyBlock(block)
            checkActiveSessionNow()
        }
    }

    fun clearStrictAlert() {
        _uiState.value = _uiState.value.copy(strictAlertMessage = null)
    }
}
