package com.realize.app.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.realize.app.data.AvoidZone
import com.realize.app.data.RealizeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages Google Play Services Geofencing for Avoid Zones.
 */
object GeofenceManager {

    const val ACTION_GEOFENCE_EVENT = "com.realize.app.ACTION_GEOFENCE_EVENT"

    private fun getGeofencingClient(context: Context): GeofencingClient {
        return LocationServices.getGeofencingClient(context)
    }

    private fun getGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    /**
     * Registers a single avoid zone with Google Play Services Geofencing.
     */
    fun addGeofence(
        context: Context,
        zone: AvoidZone,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        if (!hasLocationPermission(context)) {
            onFailure(SecurityException("Location permission not granted"))
            return
        }

        try {
            val geofence = Geofence.Builder()
                .setRequestId(zone.id.toString())
                .setCircularRegion(
                    zone.latitude,
                    zone.longitude,
                    zone.radiusMeters.coerceAtLeast(10f) // Minimum 10 meters
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL
                )
                .setLoiteringDelay(5000) // 5 seconds dwell
                .build()

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(
                    GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL
                )
                .addGeofence(geofence)
                .build()

            val client = getGeofencingClient(context)
            val pendingIntent = getGeofencePendingIntent(context)

            client.addGeofences(request, pendingIntent)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onFailure(e) }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    /**
     * Removes a geofence by its database ID.
     */
    fun removeGeofence(
        context: Context,
        zoneId: Long,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val client = getGeofencingClient(context)
        client.removeGeofences(listOf(zoneId.toString()))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /**
     * Re-registers all active avoid zones from the database.
     * Called on device reboot and app initialization.
     */
    fun reRegisterAllGeofences(context: Context) {
        if (!hasLocationPermission(context)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = RealizeDatabase.getDatabase(context)
                val enabledZones = db.avoidZoneDao().getEnabledAvoidZones()
                if (enabledZones.isEmpty()) return@launch

                val geofences = enabledZones.map { zone ->
                    Geofence.Builder()
                        .setRequestId(zone.id.toString())
                        .setCircularRegion(
                            zone.latitude,
                            zone.longitude,
                            zone.radiusMeters.coerceAtLeast(10f)
                        )
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(
                            Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL
                        )
                        .setLoiteringDelay(5000)
                        .build()
                }

                val request = GeofencingRequest.Builder()
                    .setInitialTrigger(
                        GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL
                    )
                    .addGeofences(geofences)
                    .build()

                val client = getGeofencingClient(context)
                val pendingIntent = getGeofencePendingIntent(context)

                client.addGeofences(request, pendingIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
