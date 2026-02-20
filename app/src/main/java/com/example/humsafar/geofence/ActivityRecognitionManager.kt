// app/src/main/java/com/example/humsafar/geofence/ActivityRecognitionManager.kt
//
// WHAT THIS ACTUALLY DOES:
// Asks Google Play Services to tell you what the user is physically doing.
// Every ~10 seconds you get a probability report like:
//   WALKING      → 82%
//   STILL        → 12%
//   IN_VEHICLE   → 3%
//   ON_BICYCLE   → 3%
//
// You use this to decide whether to act on a geofence trigger.
// If the user is IN_VEHICLE when they cross the Red Fort geofence boundary,
// they are almost certainly driving past — don't open the chatbot.
// If they are WALKING or STILL, they are probably visiting — show the UI.

package com.example.humsafar.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

private const val TAG = "ActivityRecognition"

// ── How often to request activity updates (milliseconds) ──────────────────
// 10 seconds is a good balance: responsive enough to detect transitions
// (sitting→walking), not so frequent it drains battery.
private const val DETECTION_INTERVAL_MS = 10_000L

class ActivityRecognitionManager(private val context: Context) {

    private val client = ActivityRecognition.getClient(context)

    /**
     * The PendingIntent that fires ActivityTransitionReceiver on activity changes.
     *
     * FLAG_MUTABLE: ActivityRecognition needs to write activity data into the Intent
     * before delivering it — same reason as geofencing PendingIntents.
     */
    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityTransitionReceiver::class.java).apply {
            action = ActivityTransitionReceiver.ACTION_ACTIVITY_UPDATE
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT
        PendingIntent.getBroadcast(context, 100, intent, flags)
        // requestCode=100 — different from geofence's 0, avoids PendingIntent collision
    }

    /**
     * Check if ACTIVITY_RECOGNITION permission is granted.
     *
     * This permission was added in API 29 (Android 10).
     * On API 28 and below, no permission is needed for ActivityRecognition.
     * On API 29+, it is a RUNTIME permission — user must explicitly grant it.
     * On API 33+, it still uses the same permission string.
     *
     * It shows as "Physical activity" in the system permission dialog.
     */
    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required below API 29
        }
    }

    /**
     * Start receiving activity updates.
     * DETECTION_INTERVAL_MS controls how often GPS/accelerometer is polled.
     * Results are delivered to ActivityTransitionReceiver via PendingIntent.
     */
    fun startTracking() {
        if (!hasPermission()) {
            Log.w(TAG, "ACTIVITY_RECOGNITION permission not granted — skipping")
            return
        }
        client.requestActivityUpdates(DETECTION_INTERVAL_MS, pendingIntent)
            .addOnSuccessListener {
                Log.i(TAG, "Activity recognition started (interval=${DETECTION_INTERVAL_MS}ms)")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to start activity recognition", e)
            }
    }

    /**
     * Stop activity updates. Call this on logout or when the user disables
     * background tracking in your settings screen.
     */
    fun stopTracking() {
        client.removeActivityUpdates(pendingIntent)
            .addOnCompleteListener {
                Log.i(TAG, "Activity recognition stopped")
            }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ActivityTransitionReceiver
// ════════════════════════════════════════════════════════════════════════════
//
// This BroadcastReceiver fires every DETECTION_INTERVAL_MS with the current
// activity probabilities. We store the most confident activity in a singleton
// so GeofenceTransitionReceiver can read it when a geofence fires.
//
// Declare in AndroidManifest.xml:
//   <receiver
//       android:name=".geofence.ActivityTransitionReceiver"
//       android:enabled="true"
//       android:exported="false" />

class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ACTIVITY_UPDATE) return

        if (!ActivityRecognitionResult.hasResult(intent)) {
            Log.w(TAG, "No ActivityRecognitionResult in intent")
            return
        }

        val result = ActivityRecognitionResult.extractResult(intent) ?: return
        val mostProbable = result.mostProbableActivity

        Log.d(TAG, "Activity: ${activityToString(mostProbable.type)} " +
                "(confidence=${mostProbable.confidence}%)")

        // Log all activities for debugging
        result.probableActivities.forEach { activity ->
            Log.v(TAG, "  ${activityToString(activity.type)}: ${activity.confidence}%")
        }

        // Store in singleton so GeofenceTransitionReceiver can read it
        CurrentActivityHolder.update(
            type       = mostProbable.type,
            confidence = mostProbable.confidence
        )
    }

    companion object {
        const val ACTION_ACTIVITY_UPDATE = "com.example.humsafar.ACTION_ACTIVITY_UPDATE"
    }
}

// ════════════════════════════════════════════════════════════════════════════
// CurrentActivityHolder — thread-safe in-memory store of current activity
// ════════════════════════════════════════════════════════════════════════════
//
// A simple @Volatile singleton. Both ActivityTransitionReceiver (writes)
// and GeofenceTransitionReceiver (reads) run on the main thread,
// so @Volatile is sufficient — no need for Mutex here.

object CurrentActivityHolder {

    @Volatile private var activityType: Int = DetectedActivity.UNKNOWN
    @Volatile private var confidence: Int   = 0

    fun update(type: Int, confidence: Int) {
        this.activityType = type
        this.confidence   = confidence
    }

    /**
     * Returns true if the user is clearly in a vehicle with high confidence.
     * Use this to suppress geofence triggers when the user drives past a site.
     *
     * Threshold: 70% confidence. Below this, we assume the user might be walking
     * and still show the heritage UI (false negative is better than false positive
     * here — missing a walking visitor is worse than a rare in-car trigger).
     */
    fun isUserInVehicle(): Boolean {
        return activityType == DetectedActivity.IN_VEHICLE && confidence >= 70
    }

    /**
     * Returns true if the user is on foot (walking or running).
     */
    fun isUserOnFoot(): Boolean {
        return (activityType == DetectedActivity.WALKING ||
                activityType == DetectedActivity.ON_FOOT ||
                activityType == DetectedActivity.RUNNING) && confidence >= 50
    }

    /**
     * Returns true if the user is stationary.
     * STILL inside a geofence = definitely visiting the site.
     */
    fun isUserStill(): Boolean {
        return activityType == DetectedActivity.STILL && confidence >= 60
    }

    fun currentActivityString(): String =
        "${activityToString(activityType)} (${confidence}%)"
}

fun activityToString(type: Int): String = when (type) {
    DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
    DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
    DetectedActivity.ON_FOOT    -> "ON_FOOT"
    DetectedActivity.WALKING    -> "WALKING"
    DetectedActivity.RUNNING    -> "RUNNING"
    DetectedActivity.STILL      -> "STILL"
    DetectedActivity.TILTING    -> "TILTING"
    DetectedActivity.UNKNOWN    -> "UNKNOWN"
    else                        -> "OTHER($type)"
}