// app/src/main/java/com/example/humsafar/geofence/GeofenceHandlerService.kt

package com.example.humsafar.geofence

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.humsafar.MainActivity
// FIX BUG 6: R must be explicitly imported from your own package.
// Without this import the file won't compile because R.mipmap.ic_launcher
// is ambiguous when multiple libraries define R classes.
import com.example.humsafar.R
import com.example.humsafar.data.HeritageRepository

private const val TAG = "GeofenceService"

class GeofenceHandlerService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ── CRITICAL RULE ──────────────────────────────────────────────────────
        // startForeground() MUST be called within 5 seconds of onStartCommand().
        // If you do ANY blocking work (DB query, network call) before this call
        // and it takes > 5s, the OS will throw a ForegroundServiceDidNotStartInTimeException
        // (API 31+) or ANR-crash the app on older versions.
        //
        // Pattern: build the notification synchronously from what you already know
        // (site name from Intent extras), call startForeground(), THEN do async work.
        val siteId   = intent?.getStringExtra(EXTRA_SITE_ID) ?: ""
        val siteName = HeritageRepository.sites
            .find { it.id == siteId }?.name ?: "Heritage Site"

        // startForeground() is the FIRST thing we do — no exceptions.
        startForeground(GeofenceConstants.NOTIFICATION_ID, buildNotification(siteName))

        when (intent?.action) {
            ACTION_SITE_ENTERED -> {
                Log.i(TAG, "Handling entry to: $siteName (id=$siteId)")
                // Place async work here — the foreground notification is already up.
                // Examples: log to analytics, save to Room DB, prefetch chatbot context.
            }
            else -> {
                // Unknown action — service started spuriously. Stop immediately.
                Log.w(TAG, "Service started with unknown action: ${intent?.action}")
                stopSelf(startId)
            }
        }

        // START_NOT_STICKY: if the OS kills this service, do NOT restart it.
        // The next geofence transition will start it again when needed.
        // Using START_REDELIVER_INTENT or START_STICKY here would cause the
        // notification to reappear unexpectedly after the user dismisses it.
        return START_NOT_STICKY
    }

    /**
     * Builds the foreground notification shown when the user enters a site.
     * Tapping it opens MainActivity with a flag so you can navigate to the
     * chatbot or show a site detail screen.
     */
    private fun buildNotification(siteName: String): Notification {
        // createNotificationChannel() is idempotent — safe to call every time.
        // On API < 26 it is a no-op.
        createNotificationChannel()

        // Tapping the notification deep-links back into MainActivity.
        // FLAG_IMMUTABLE is correct here: we do NOT need the system to mutate
        // this PendingIntent (contrast with the geofence PendingIntent which uses FLAG_MUTABLE).
        val tapPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_FROM_GEOFENCE, true)
                putExtra(EXTRA_SITE_ID, siteName)
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, GeofenceConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📍 Heritage Site Nearby")
            .setContentText("You are at $siteName — tap to explore!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_LOCATION_SHARING)
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            // Ongoing = false: user can dismiss the notification.
            // Set to true only if you want to force them to interact with the app.
            .setOngoing(false)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                GeofenceConstants.NOTIFICATION_CHANNEL_ID,
                "Heritage Site Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you enter a heritage site"
                // Don't enable sound for a location service — it would be
                // annoying if the user gets audibly notified just for walking near a site.
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            // createNotificationChannel is idempotent: creating an existing channel
            // with the same ID does nothing, so calling this repeatedly is safe.
            manager.createNotificationChannel(channel)
        }
    }

    // A foreground service must override onBind(). Return null for started services.
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_SITE_ENTERED  = "com.example.humsafar.SITE_ENTERED"
        const val EXTRA_SITE_ID        = "extra_site_id"
        const val EXTRA_FROM_GEOFENCE  = "from_geofence"
    }
}