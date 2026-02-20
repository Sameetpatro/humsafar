// app/src/main/java/com/example/humsafar/geofence/BootReceiver.kt

package com.example.humsafar.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "BootReceiver"

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isBootComplete = action == Intent.ACTION_BOOT_COMPLETED ||
                action == "android.intent.action.QUICKBOOT_POWERON"
        if (!isBootComplete) return

        Log.i(TAG, "Boot completed — re-registering geofences and activity recognition")

        // Re-register geofences (cleared on every reboot by the OS)
        GeofenceSyncManager(context).syncAndRegister()

        // Restart activity recognition updates (also cleared on reboot)
        // This restores motion-aware detection without requiring the user
        // to open the app after reboot.
        ActivityRecognitionManager(context).startTracking()
    }
}