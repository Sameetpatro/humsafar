// app/src/main/java/com/example/humsafar/location/HotspotMonitor.kt
package com.example.humsafar.location

import com.example.humsafar.utils.haversineDistance

/**
 * Purely functional — no state, no lifecycle.
 * LocationService calls checkExit() on every location tick.
 * Radius comes from backend node metadata, not hardcoded here.
 */
object HotspotMonitor {

    private var centerLat: Double = 0.0
    private var centerLng: Double = 0.0
    private var radiusMeters: Double = 150.0

    fun configure(lat: Double, lng: Double, radiusMeters: Double) {
        centerLat = lat
        centerLng = lng
        this.radiusMeters = radiusMeters
    }

    fun checkExit(lat: Double, lng: Double): Boolean {
        if (centerLat == 0.0) return false
        return haversineDistance(lat, lng, centerLat, centerLng) > radiusMeters
    }

    fun reset() {
        centerLat = 0.0; centerLng = 0.0; radiusMeters = 150.0
    }
}