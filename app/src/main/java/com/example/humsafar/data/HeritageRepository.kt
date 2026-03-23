// app/src/main/java/com/example/humsafar/data/HeritageRepository.kt
//
// REWRITTEN — no longer hardcoded.
// Fetches all sites from GET /sites/nearby with a 10,000 km range
// so it always gets every site in the DB.
// Results are cached in memory for the app session.
// Falls back to an empty list if network is unavailable.

package com.example.humsafar.data

import android.util.Log
import com.example.humsafar.models.HeritageSite
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "HeritageRepository"

object HeritageRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory cache — updated every time fetchAll() succeeds
    private val _sites = MutableStateFlow<List<HeritageSite>>(emptyList())
    val sitesFlow: StateFlow<List<HeritageSite>> = _sites.asStateFlow()

    // Synchronous accessor used by geofence + map code that can't suspend
    val sites: List<HeritageSite> get() = _sites.value

    // Track whether we've done the initial load
    private var hasFetched = false
    val hasFetchedPublic: Boolean get() = hasFetched

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Call once on app start (e.g. from MapScreen or Application).
     * Fetches all sites and caches them. Safe to call multiple times.
     */
    fun fetchAll(lat: Double = 28.6, lng: Double = 77.2) {
        scope.launch { loadFromBackend(lat, lng) }
    }

    /**
     * Force a fresh fetch even if already loaded.
     */
    fun refresh(lat: Double = 28.6, lng: Double = 77.2) {
        hasFetched = false
        scope.launch { loadFromBackend(lat, lng) }
    }

    // ── Private ───────────────────────────────────────────────────────────

    private suspend fun loadFromBackend(lat: Double, lng: Double) {
        try {
            Log.d(TAG, "Fetching all sites from backend…")

            // 10,000 km radius = effectively the whole world
            val resp = HumsafarClient.api.getNearbySites(
                lat        = lat,
                lng        = lng,
                maxRangeKm = 10_000.0
            )

            if (resp.isSuccessful && resp.body() != null) {
                val fetched = resp.body()!!.map { s ->
                    HeritageSite(
                        id        = s.id.toString(),
                        name      = s.name,
                        latitude  = s.lat,
                        longitude = s.lng,
                        radius    = 500.0   // default geofence radius
                        // backend doesn't return radius in /nearby
                        // use a safe default; actual check is done
                        // server-side via inside_geofence flag
                    )
                }
                _sites.value = fetched
                hasFetched   = true
                Log.i(TAG, "Loaded ${fetched.size} sites: ${fetched.map { it.name }}")
            } else {
                Log.w(TAG, "getNearbySites failed: ${resp.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAll error: ${e.message}", e)
        }
    }
}