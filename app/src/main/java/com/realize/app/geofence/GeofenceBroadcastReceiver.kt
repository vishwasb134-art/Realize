package com.realize.app.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.realize.app.data.PreferencesManager
import com.realize.app.data.RealizeDatabase
import com.realize.app.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by Google Play Services Geofencing API
 * when the user enters or dwells inside an avoid zone (such as a shop, mall, or temptation spot).
 * Dispatches high-priority reminder ending with ' for conciousness'.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        val isEnterOrDwell = transitionType == Geofence.GEOFENCE_TRANSITION_ENTER ||
                transitionType == Geofence.GEOFENCE_TRANSITION_DWELL

        if (!isEnterOrDwell) return

        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
        if (triggeringGeofences.isEmpty()) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = RealizeDatabase.getDatabase(context)
                val prefs = PreferencesManager(context)

                for (geofence in triggeringGeofences) {
                    val zoneId = geofence.requestId.toLongOrNull() ?: continue
                    val zone = db.avoidZoneDao().getZoneById(zoneId) ?: continue

                    if (!zone.isEnabled) continue

                    NotificationHelper.showAvoidZoneNotification(
                        context = context,
                        zoneName = zone.name,
                        savingsGoal = prefs.savingsGoal
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
