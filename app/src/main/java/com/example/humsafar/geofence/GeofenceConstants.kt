// app/src/main/java/com/example/humsafar/geofence/GeofenceConstants.kt

package com.example.humsafar.geofence

object GeofenceConstants {

    const val ACTION_GEOFENCE_EVENT = "com.example.humsafar.ACTION_GEOFENCE_EVENT"

    // OS hard limit: max 100 geofences per app across all PendingIntents
    const val MAX_GEOFENCES = 100

    // How long user must stay inside before GEOFENCE_TRANSITION_DWELL fires.
    // 30 seconds avoids false triggers from driving past a site.
    const val LOITERING_DELAY_MS = 30_000

    // Notification
    const val NOTIFICATION_CHANNEL_ID = "geofence_channel"
    const val NOTIFICATION_ID         = 1001
}