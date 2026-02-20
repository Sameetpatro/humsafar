// app/src/main/java/com/example/humsafar/geofence/GeofencePermissionHandler.kt
//
// UPDATED: Now requests ACTIVITY_RECOGNITION permission.
// This is why you never saw a motion permission dialog — it was never requested.
// Fixed here.

package com.example.humsafar.geofence

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

private const val TAG = "GeofencePermissions"

/**
 * Full permission chain for geofencing + motion detection:
 *
 * STEP 1 — POST_NOTIFICATIONS (API 33+ only)
 *   Shows as: "Allow Humsafar to send notifications?"
 *   Why: so the heritage site notification can appear when backgrounded
 *
 * STEP 2 — ACTIVITY_RECOGNITION (API 29+ only)
 *   Shows as: "Allow Humsafar to track your physical activity?"
 *   Why: to detect if user is walking vs driving past a heritage site
 *   THIS WAS MISSING IN THE PREVIOUS VERSION — that's why you never saw the dialog
 *
 * STEP 3 — ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION
 *   Shows as: "Allow Humsafar to access your location?"
 *   Why: base requirement for geofencing
 *
 * STEP 4 — ACCESS_BACKGROUND_LOCATION (API 29+ only, AFTER step 3)
 *   Shows as: system Settings screen on API 30+, not a dialog
 *   Why: so geofences fire when app is not open
 *   MUST be requested separately AFTER fine location is granted
 */
@Composable
fun GeofencePermissionHandler() {
    val context = LocalContext.current

    val geofenceManager         = remember { HumsafarGeofenceManager(context) }
    val activityRecognitionMgr  = remember { ActivityRecognitionManager(context) }
    val syncManager             = remember { GeofenceSyncManager(context, geofenceManager) }

    DisposableEffect(Unit) {
        onDispose { syncManager.cancel() }
    }

    // ── Final step: everything granted, start everything ─────────────────
    fun onAllPermissionsReady() {
        Log.i(TAG, "All permissions granted — starting geofences + activity recognition")
        syncManager.syncAndRegister()
        activityRecognitionMgr.startTracking()
    }

    // ── Step 4: Background location ──────────────────────────────────────
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.i(TAG, "Background location granted")
        } else {
            Log.w(TAG, "Background location denied — foreground-only geofencing")
        }
        // Either way: start what we can. Geofencing works in foreground without it.
        onAllPermissionsReady()
    }

    // ── Step 3: Fine + Coarse location ───────────────────────────────────
    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Must ask background SEPARATELY after fine is granted
                backgroundLocationLauncher.launch(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                onAllPermissionsReady()
            }
        } else {
            Log.e(TAG, "Fine location denied — geofencing will not work")
        }
    }

    // ── Step 2: Activity Recognition ─────────────────────────────────────
    // THIS IS THE MISSING PIECE from the previous version.
    // Shows the "physical activity" permission dialog.
    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.i(TAG, "ACTIVITY_RECOGNITION granted — motion-aware detection enabled")
        } else {
            // Geofencing still works, but without vehicle suppression.
            // User will get false triggers when driving past heritage sites.
            Log.w(TAG, "ACTIVITY_RECOGNITION denied — geofencing without motion awareness")
        }
        // Proceed to location permissions regardless
        fineLocationLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // ── Step 1: Notifications (API 33+) ──────────────────────────────────
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Proceed to activity recognition regardless of result
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            fineLocationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ── Kick off the chain ────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        if (geofenceManager.hasRequiredPermissions() && activityRecognitionMgr.hasPermission()) {
            Log.i(TAG, "All permissions already granted — re-registering")
            onAllPermissionsReady()
            return@LaunchedEffect
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // API 33+: notifications → activity → location → background
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // API 29-32: activity → location → background
                activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            else -> {
                // API 28 and below: just location
                fineLocationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
}