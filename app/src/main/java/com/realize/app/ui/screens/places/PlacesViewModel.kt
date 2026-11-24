package com.realize.app.ui.screens.places

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.realize.app.data.AvoidZone
import com.realize.app.data.PreferencesManager
import com.realize.app.data.RealizeDatabase
import com.realize.app.geofence.GeofenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlacesUiState(
    val hasFineLocation: Boolean = false,
    val hasBackgroundLocation: Boolean = false,
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val savingsGoal: String = "",
    val isLoadingLocation: Boolean = false,
    val statusMessage: String? = null
)

class PlacesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RealizeDatabase.getDatabase(application)
    private val avoidZoneDao = db.avoidZoneDao()
    private val prefs = PreferencesManager(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _uiState = MutableStateFlow(PlacesUiState())
    val uiState: StateFlow<PlacesUiState> = _uiState.asStateFlow()

    val avoidZonesFlow = avoidZoneDao.getAllAvoidZones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkPermissionsAndLoad()
    }

    fun checkPermissionsAndLoad() {
        val app = getApplication<Application>()
        val fineGranted = ContextCompat.checkSelfPermission(
            app,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val bgGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                app,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        _uiState.value = _uiState.value.copy(
            hasFineLocation = fineGranted,
            hasBackgroundLocation = bgGranted,
            savingsGoal = prefs.savingsGoal
        )

        if (fineGranted) {
            fetchCurrentLocation()
        }
    }

    fun updateSavingsGoal(goal: String) {
        prefs.savingsGoal = goal
        _uiState.value = _uiState.value.copy(savingsGoal = goal)
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(onSuccess: ((Double, Double) -> Unit)? = null) {
        val app = getApplication<Application>()
        if (!GeofenceManager.hasLocationPermission(app)) return

        _uiState.value = _uiState.value.copy(isLoadingLocation = true)

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        _uiState.value = _uiState.value.copy(
                            currentLat = loc.latitude,
                            currentLng = loc.longitude,
                            isLoadingLocation = false
                        )
                        onSuccess?.invoke(loc.latitude, loc.longitude)
                    } else {
                        // Fallback to last known location
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                _uiState.value = _uiState.value.copy(
                                    currentLat = lastLoc.latitude,
                                    currentLng = lastLoc.longitude,
                                    isLoadingLocation = false
                                )
                                onSuccess?.invoke(lastLoc.latitude, lastLoc.longitude)
                            } else {
                                _uiState.value = _uiState.value.copy(isLoadingLocation = false)
                            }
                        }.addOnFailureListener {
                            _uiState.value = _uiState.value.copy(isLoadingLocation = false)
                        }
                    }
                }
                .addOnFailureListener {
                    _uiState.value = _uiState.value.copy(
                        isLoadingLocation = false,
                        statusMessage = "Could not fetch GPS coordinates."
                    )
                }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoadingLocation = false)
        }
    }

    fun addAvoidZone(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val zone = AvoidZone(
                name = name.ifBlank { "Shop near Jain University College" },
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters.coerceAtLeast(10f),
                isEnabled = true
            )
            val newId = avoidZoneDao.insertAvoidZone(zone)
            val createdZone = zone.copy(id = newId)

            // Register geofence with Play Services
            GeofenceManager.addGeofence(app, createdZone)
        }
    }

    fun toggleAvoidZone(zone: AvoidZone, enabled: Boolean) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val updated = zone.copy(isEnabled = enabled)
            avoidZoneDao.updateAvoidZone(updated)

            if (enabled) {
                GeofenceManager.addGeofence(app, updated)
            } else {
                GeofenceManager.removeGeofence(app, zone.id)
            }
        }
    }

    fun deleteAvoidZone(zone: AvoidZone) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            GeofenceManager.removeGeofence(app, zone.id)
            avoidZoneDao.deleteAvoidZone(zone)
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
