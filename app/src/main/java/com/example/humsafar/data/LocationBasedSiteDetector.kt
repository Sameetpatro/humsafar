// app/src/main/java/com/example/humsafar/data/LocationBasedSiteDetector.kt
//
// RESPONSIBILITY: Convert raw GPS coordinates into a confirmed site_id.
//
// FLOW:
//   GPS tick every 3s
//     → throttle: skip if moved < 30m AND < 20s elapsed
//     → GET /sites/nearby?lat={lat}&lng={lng}&max_range_km=10
//     → backend runs:
//         SELECT id, name, lat, lng, geofence_radius_meters,
//                haversine(lat, lng, $lat, $lng) AS dist
//         FROM heritage_sites
//         WHERE dist <= max_range_km * 1000
//         ORDER BY dist ASC
//       and sets inside_geofence = (dist <= geofence_radius_meters)
//     → if inside_geofence=true → ActiveSiteManager.onEnterSite(site)
//     → if no site inside      → ActiveSiteManager.onExitAllSites()
//
// After onEnterSite fires, the entire app knows the site_id.
// No hardcoded IDs anywhere.

package com.example.humsafar.data

import android.util.Log
import com.example.humsafar.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

private const val TAG = "SiteDetector"

// All nearby sites (not just the one we're inside) — used for the map panel
data class NearbySiteUi(
    val id:             Int,
    val name:           String,
    val latitude:       Double,
    val longitude:      Double,
    val distanceMeters: Double,
    val insideGeofence: Boolean
)

object LocationBasedSiteDetector {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // All sites within 10km, sorted by distance — drives the "Nearby" list on the map
    private val _nearbySites = MutableStateFlow<List<NearbySiteUi>>(emptyList())
    val nearbySites: StateFlow<List<NearbySiteUi>> = _nearbySites.asStateFlow()

    // Throttle
    private var lastLat   = 0.0
    private var lastLng   = 0.0
    private var lastTime  = 0L
    private const val MIN_DISTANCE_M  = 30.0
    private const val MIN_INTERVAL_MS = 20_000L

    // Injected by whoever initialises this (e.g. MapScreen or Application)
    lateinit var api: ApiService

    // ─────────────────────────────────────────────────────────────────────────
    // Entry point — called by HumsafarLocationManager on each GPS update
    // ─────────────────────────────────────────────────────────────────────────

    fun onLocationUpdate(lat: Double, lng: Double) {
        val now   = System.currentTimeMillis()
        val moved = haversine(lat, lng, lastLat, lastLng)
        if (moved < MIN_DISTANCE_M && (now - lastTime) < MIN_INTERVAL_MS) return

        lastLat  = lat
        lastLng  = lng
        lastTime = now

        scope.launch { fetchNearbySites(lat, lng) }
    }

    fun forceRefresh(lat: Double, lng: Double) {
        lastTime = 0L
        onLocationUpdate(lat, lng)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Backend call — runs the haversine SQL query
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun fetchNearbySites(lat: Double, lng: Double) {
        try {
            Log.d(TAG, "→ GET /sites/nearby lat=$lat lng=$lng")

            val response = api.getNearbySites(lat = lat, lng = lng, maxRangeKm = 10.0)

            if (!response.isSuccessful || response.body() == null) {
                Log.w(TAG, "getNearbySites failed: ${response.code()}")
                return
            }

            val sites = response.body()!!
            Log.d(TAG, "← ${sites.size} sites within 10km")

            // Map to UI model
            val uiSites = sites.map { s ->
                NearbySiteUi(
                    id             = s.id,
                    name           = s.name,
                    latitude       = s.latitude,
                    longitude      = s.longitude,
                    distanceMeters = s.distanceMeters,
                    insideGeofence = s.insideGeofence
                )
            }
            _nearbySites.value = uiSites

            // Find the site we are physically inside (closest one if overlapping)
            val insideSite = uiSites
                .filter { it.insideGeofence }
                .minByOrNull { it.distanceMeters }

            if (insideSite != null) {
                // Entered or still inside a site — give ActiveSiteManager the DB id
                ActiveSiteManager.onEnterSite(
                    ActiveSite(
                        id                   = insideSite.id,
                        name                 = insideSite.name,
                        latitude             = insideSite.latitude,
                        longitude            = insideSite.longitude,
                        geofenceRadiusMeters = 0,  // not needed client-side
                        distanceMeters       = insideSite.distanceMeters
                    )
                )
            } else {
                // Not inside any geofence
                ActiveSiteManager.onExitAllSites()
            }

        } catch (e: Exception) {
            Log.e(TAG, "fetchNearbySites error: ${e.message}", e)
        }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R    = 6371000.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLon / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}