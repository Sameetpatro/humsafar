// app/src/main/java/com/example/humsafar/geofence/HumsafarGeofenceManager.kt

package com.example.humsafar.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.humsafar.models.HeritageSite
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

private const val TAG = "GeofenceManager"

class HumsafarGeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    /**
     * FIXED BUG 3: FLAG_MUTABLE was added in API 31 (Android S).
     * Your minSdk is 29, so on API 29 and 30 the previous code would
     * crash with NoSuchFieldError at runtime.
     *
     * Correct fix: use FLAG_MUTABLE only on API 31+.
     * On API 29/30, FLAG_UPDATE_CURRENT alone is sufficient AND correct —
     * PendingIntents were mutable by default before API 31.
     *
     * Why FLAG_MUTABLE is needed on API 31+ for geofencing:
     * The GeofencingClient needs to mutate the Intent inside the PendingIntent
     * to attach geofence trigger data before delivering it to your receiver.
     * FLAG_IMMUTABLE would block this and the receiver would get an empty Intent.
     */
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceTransitionReceiver::class.java).apply {
            action = GeofenceConstants.ACTION_GEOFENCE_EVENT
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: must explicitly declare mutability
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            // API 29-30: mutable by default, FLAG_UPDATE_CURRENT is sufficient
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    /**
     * Returns true only when all permissions needed for background geofencing
     * are granted.
     *
     * Permission matrix:
     *   API 28-: ACCESS_FINE_LOCATION is enough for background
     *   API 29+: ACCESS_FINE_LOCATION + ACCESS_BACKGROUND_LOCATION both required
     */
    fun hasRequiredPermissions(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required below API 29
        }

        return fineGranted && backgroundGranted
    }

    /**
     * Returns true if only fine location is granted but background is not.
     * Use this to show a rationale UI explaining why background is needed
     * before launching the background location permission request.
     */
    fun hasForegroundLocationOnly(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return fineGranted && !backgroundGranted
    }

    /**
     * Register a list of HeritageSites as OS-managed geofences.
     *
     * This is safe to call multiple times. We always remove existing fences
     * first so that updated coordinates/radii from the backend take effect.
     *
     * CALLER RESPONSIBILITY: Check hasRequiredPermissions() before calling.
     */
    @Suppress("MissingPermission") // Permission check is done in hasRequiredPermissions()
    fun registerGeofences(
        sites: List<HeritageSite>,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (!hasRequiredPermissions()) {
            onFailure("Missing required location permissions")
            return
        }

        if (sites.isEmpty()) {
            onFailure("No sites provided to register")
            return
        }

        // Clamp to OS maximum of 100 geofences per app
        val clamped = sites.take(GeofenceConstants.MAX_GEOFENCES)
        if (sites.size > GeofenceConstants.MAX_GEOFENCES) {
            Log.w(TAG, "Truncated ${sites.size} sites to $MAX_GEOFENCES (OS limit)")
        }

        val geofences = clamped.map { buildGeofence(it) }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(
                // INITIAL_TRIGGER_ENTER: if the user is already inside a fence
                // when you register, fire immediately. Critical for the case
                // where the user opens the app while standing at a heritage site.
                GeofencingRequest.INITIAL_TRIGGER_ENTER or
                        GeofencingRequest.INITIAL_TRIGGER_DWELL
            )
            .addGeofences(geofences)
            .build()

        // Always remove stale registrations first.
        // If you just call addGeofences() without removing, the OS may keep
        // old entries with outdated radii alongside new ones for the same ID.
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnCompleteListener {
                // addOnCompleteListener fires regardless of remove success/failure.
                // We proceed either way — if there were no existing fences to remove,
                // that is fine.
                geofencingClient.addGeofences(request, geofencePendingIntent)
                    .addOnSuccessListener {
                        Log.i(TAG, "Successfully registered ${geofences.size} geofences")
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        val statusCode = (exception as? com.google.android.gms.common.api.ApiException)
                            ?.statusCode ?: -1
                        val message = when (statusCode) {
                            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE ->
                                "Geofencing not available (location disabled or Doze active)"
                            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES ->
                                "Too many geofences (max $MAX_GEOFENCES per app)"
                            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS ->
                                "Too many PendingIntents (max 5 per app)"
                            else -> "Registration failed: ${exception.message} (code $statusCode)"
                        }
                        Log.e(TAG, message, exception)
                        onFailure(message)
                    }
            }
    }

    private fun buildGeofence(site: HeritageSite): Geofence =
        Geofence.Builder()
            .setRequestId(site.id)
            .setCircularRegion(
                site.latitude,
                site.longitude,
                site.radius.toFloat()   // radius is Double in your model; toFloat() is correct
            )
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_DWELL or
                        Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .setLoiteringDelay(GeofenceConstants.LOITERING_DELAY_MS)
            // FIX: Use Geofence.NEVER_EXPIRE directly from SDK (= -1L)
            // instead of our own constant that was a redundant duplicate.
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            // 0ms responsiveness = trigger as fast as possible after movement detected.
            // Appropriate for a heritage tour app where timing matters.
            // Use 60_000 if you are more battery-conservative.
            .setNotificationResponsiveness(0)
            .build()

    /**
     * Removes ALL registered geofences tied to our PendingIntent.
     * Call on logout or when user disables background detection in settings.
     */
    fun removeAllGeofences(onComplete: () -> Unit = {}) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnCompleteListener {
                Log.i(TAG, "All geofences removed")
                onComplete()
            }
    }

    /**
     * Removes specific geofences by their site ID.
     * Use when a site is deleted from the backend.
     */
    fun removeGeofences(siteIds: List<String>, onComplete: () -> Unit = {}) {
        geofencingClient.removeGeofences(siteIds)
            .addOnCompleteListener {
                Log.i(TAG, "Removed geofences: $siteIds")
                onComplete()
            }
    }

    companion object {
        private const val MAX_GEOFENCES = GeofenceConstants.MAX_GEOFENCES
    }
}