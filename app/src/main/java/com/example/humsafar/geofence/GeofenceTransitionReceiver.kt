// app/src/main/java/com/example/humsafar/geofence/GeofenceTransitionReceiver.kt
//
// UPDATED: Now reads CurrentActivityHolder before acting on geofence events.
// If the user is IN_VEHICLE with ≥70% confidence → suppress the trigger.
// This is the actual motion-aware behaviour that was claimed but missing before.

package com.example.humsafar.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

private const val TAG = "GeofenceReceiver"

class GeofenceTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != GeofenceConstants.ACTION_GEOFENCE_EVENT) return

        @Suppress("DEPRECATION")
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: run {
            Log.e(TAG, "Null GeofencingEvent — ignoring")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "GeofencingEvent error: ${errorCodeToString(geofencingEvent.errorCode)}")
            return
        }

        val transition       = geofencingEvent.geofenceTransition
        val triggeringFences = geofencingEvent.triggeringGeofences ?: emptyList<Geofence>()

        Log.d(TAG, "Transition=${transitionToString(transition)}, " +
                "fences=${triggeringFences.map { it.requestId }}, " +
                "currentActivity=${CurrentActivityHolder.currentActivityString()}")

        when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER,
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                // ── ACTUAL MOTION-AWARE CHECK ────────────────────────────
                // This is what was claimed but missing in the previous version.
                // Read the last known activity from CurrentActivityHolder
                // (updated every 10s by ActivityTransitionReceiver).
                //
                // If user is IN_VEHICLE → they are driving past the site.
                // Suppress the trigger entirely. No notification, no UI update.
                //
                // Example: user drives along a road 200m from the Taj Mahal.
                // Without this check: chatbot opens while they're driving at 60km/h.
                // With this check: nothing happens. They have to walk there.
                if (CurrentActivityHolder.isUserInVehicle()) {
                    Log.i(TAG, "Suppressing ENTER — user is IN_VEHICLE " +
                            "(${CurrentActivityHolder.currentActivityString()})")
                    return
                }

                triggeringFences.forEach { geofence ->
                    handleSiteEntered(context, geofence.requestId)
                }
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                // Always handle exits regardless of activity.
                // If we suppressed the ENTER, this EXIT will also be ignored
                // because insideSite was never set in the UI.
                triggeringFences.forEach { geofence ->
                    handleSiteExited(context, geofence.requestId)
                }
            }

            else -> Log.w(TAG, "Unknown transition: $transition")
        }
    }

    private fun handleSiteEntered(context: Context, siteId: String) {
        Log.i(TAG, "CONFIRMED ENTRY to site=$siteId " +
                "(activity=${CurrentActivityHolder.currentActivityString()})")

        // Start foreground service → shows notification
        val serviceIntent = Intent(context, GeofenceHandlerService::class.java).apply {
            action = GeofenceHandlerService.ACTION_SITE_ENTERED
            putExtra(GeofenceHandlerService.EXTRA_SITE_ID, siteId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // Notify foreground UI via LocalBroadcast
        LocalBroadcastManager.getInstance(context).sendBroadcast(
            Intent(ACTION_GEOFENCE_UI_UPDATE).apply {
                putExtra(EXTRA_SITE_ID, siteId)
                putExtra(EXTRA_TRANSITION, TRANSITION_ENTER)
            }
        )
    }

    private fun handleSiteExited(context: Context, siteId: String) {
        Log.i(TAG, "EXITED site=$siteId")
        LocalBroadcastManager.getInstance(context).sendBroadcast(
            Intent(ACTION_GEOFENCE_UI_UPDATE).apply {
                putExtra(EXTRA_SITE_ID, siteId)
                putExtra(EXTRA_TRANSITION, TRANSITION_EXIT)
            }
        )
    }

    private fun errorCodeToString(code: Int): String = when (code) {
        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE ->
            "GEOFENCE_NOT_AVAILABLE — location off or Doze active"
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES ->
            "GEOFENCE_TOO_MANY_GEOFENCES — exceeded 100 limit"
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS ->
            "GEOFENCE_TOO_MANY_PENDING_INTENTS — exceeded 5 limit"
        else -> "Unknown code: $code"
    }

    private fun transitionToString(t: Int) = when (t) {
        Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
        Geofence.GEOFENCE_TRANSITION_DWELL -> "DWELL"
        Geofence.GEOFENCE_TRANSITION_EXIT  -> "EXIT"
        else -> "UNKNOWN($t)"
    }

    companion object {
        const val ACTION_GEOFENCE_UI_UPDATE = "com.example.humsafar.GEOFENCE_UI_UPDATE"
        const val EXTRA_SITE_ID             = "extra_site_id"
        const val EXTRA_TRANSITION          = "extra_transition"
        const val TRANSITION_ENTER          = "enter"
        const val TRANSITION_EXIT           = "exit"
    }
}