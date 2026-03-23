package com.example.humsafar.data

import android.util.Log
import com.example.humsafar.network.HumsafarApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

private const val TAG = "SiteDetector"

// Geofence radius — if user is within this many meters of a site, they are "inside"
private const val GEOFENCE_RADIUS_METERS = 300.0

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

    // All sites sorted by distance (drives the nearby panel)
    private val _nearbySites = MutableStateFlow<List<NearbySiteUi>>(emptyList())
    val nearbySites: StateFlow<List<NearbySiteUi>> = _nearbySites.asStateFlow()

    // The site the user is currently standing inside (null = not inside any)
    private val _currentSite = MutableStateFlow<NearbySiteUi?>(null)
    val currentSite: StateFlow<NearbySiteUi?> = _currentSite.asStateFlow()

    // Throttle
    private var lastLat  = 0.0
    private var lastLng  = 0.0
    private var lastTime = 0L
    private const val MIN_DISTANCE_M  = 20.0
    private const val MIN_INTERVAL_MS = 15_000L

    lateinit var api: HumsafarApiService

    // Called on every GPS tick
    fun onLocationUpdate(lat: Double, lng: Double) {
        val now   = System.currentTimeMillis()
        val moved = haversineMeters(lat, lng, lastLat, lastLng)
        if (moved < MIN_DISTANCE_M && (now - lastTime) < MIN_INTERVAL_MS) return

        lastLat  = lat
        lastLng  = lng
        lastTime = now

        scope.launch { checkSites(lat, lng) }
    }

    // Force an immediate check
    fun forceRefresh(lat: Double, lng: Double) {
        lastTime = 0L
        lastLat  = 0.0
        lastLng  = 0.0
        scope.launch { checkSites(lat, lng) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core logic:
    //   1. Fetch ALL sites from backend
    //   2. Calculate distance from user to each site using haversine
    //   3. If distance <= GEOFENCE_RADIUS_METERS → user is inside
    // ─────────────────────────────────────────────────────────────────────────
    private suspend fun checkSites(userLat: Double, userLng: Double) {
        try {
            Log.d(TAG, "Checking sites for lat=$userLat lng=$userLng")

            val response = api.getNearbySites(
                lat        = userLat,
                lng        = userLng,
                maxRangeKm = 10_000.0   // huge range = get all sites in DB
            )

            if (!response.isSuccessful || response.body() == null) {
                Log.w(TAG, "API failed: ${response.code()}")
                return
            }

            val sites = response.body()!!
            Log.d(TAG, "Fetched ${sites.size} sites")

            // Calculate distance to each site and flag if inside geofence
            val processed = sites.map { site ->
                val dist   = haversineMeters(userLat, userLng, site.latitude, site.longitude)
                val inside = dist <= GEOFENCE_RADIUS_METERS
                Log.d(TAG, "  ${site.name}: ${dist.toInt()}m  inside=$inside")

                NearbySiteUi(
                    id             = site.id,
                    name           = site.name,
                    latitude       = site.latitude,
                    longitude      = site.longitude,
                    distanceMeters = dist,
                    insideGeofence = inside
                )
            }.sortedBy { it.distanceMeters }

            _nearbySites.value = processed

            // Pick the closest site the user is inside
            val insideSite = processed.firstOrNull { it.insideGeofence }

            if (insideSite != null) {
                Log.i(TAG, "INSIDE: ${insideSite.name} @ ${insideSite.distanceMeters.toInt()}m")
                _currentSite.value = insideSite
                ActiveSiteManager.onEnterSite(
                    ActiveSite(
                        id                   = insideSite.id,
                        name                 = insideSite.name,
                        latitude             = insideSite.latitude,
                        longitude            = insideSite.longitude,
                        geofenceRadiusMeters = GEOFENCE_RADIUS_METERS.toInt(),
                        distanceMeters       = insideSite.distanceMeters
                    )
                )
            } else {
                _currentSite.value = null
                ActiveSiteManager.onExitAllSites()
            }

        } catch (e: Exception) {
            Log.e(TAG, "checkSites error: ${e.message}", e)
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R    = 6371000.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLon / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}