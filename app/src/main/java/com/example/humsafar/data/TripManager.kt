// app/src/main/java/com/example/humsafar/data/TripManager.kt
// REWRITTEN — uses FastAPI trip_id (Int) instead of Spring Boot sessionId (String).

package com.example.humsafar.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.humsafar.models.TripSnapshot
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object TripManager {

    private const val TAG = "TripManager"

    private lateinit var prefs: SharedPreferences

    private val _state = MutableStateFlow(TripSnapshot())
    val state: StateFlow<TripSnapshot> = _state.asStateFlow()

    // Process-lifetime scope. Used for fire-and-forget API calls (e.g.
    // /trips/end) that must survive ViewModel destruction caused by
    // navigation popUpTo. Tied to a SupervisorJob so one failed call
    // never tears down sibling jobs.
    private val appScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // NEW — reads Firebase UID at call time (not at init time)
    val USER_ID: String get() = com.example.humsafar.auth.AuthManager.uid

    fun init(context: Context) {
        prefs = context.getSharedPreferences("trip_prefs_v2", Context.MODE_PRIVATE)
        _state.value = TripSnapshot(
            tripId          = prefs.getInt("tripId", 0),
            siteId          = prefs.getInt("siteId", 0),
            siteName        = prefs.getString("siteName", "") ?: "",
            currentNodeId   = prefs.getInt("currentNodeId", 0),
            currentNodeName = prefs.getString("currentNodeName", "") ?: "",
            isTripActive    = prefs.getBoolean("isTripActive", false),
            startedAtMs     = prefs.getLong("startedAtMs", 0L),
            lastLat         = prefs.getFloat("lastLat", 0f).toDouble(),
            lastLng         = prefs.getFloat("lastLng", 0f).toDouble(),
            visitedNodeIds  = prefs.getString("visitedNodes", "")
                ?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        )
    }

    /** Called after /trips/start succeeds (King node scan). */
    fun activateTrip(
        tripId:   Int,
        siteId:   Int,
        siteName: String,
        nodeId:   Int,
        nodeName: String
    ) {
        QuizPrepareManager.resetForNewTrip()
        write(
            _state.value.copy(
                tripId          = tripId,
                siteId          = siteId,
                siteName        = siteName,
                currentNodeId   = nodeId,
                currentNodeName = nodeName,
                isTripActive    = true,
                startedAtMs     = System.currentTimeMillis(),
                visitedNodeIds  = listOf(nodeId)
            )
        )
        QuizPrepareManager.prepareOnNodeScan(tripId, listOf(nodeId))
    }

    /** Called after scanning a normal node. */
    fun updateCurrentNode(nodeId: Int, nodeName: String) {
        val visited = (_state.value.visitedNodeIds + nodeId).distinct()
        write(
            _state.value.copy(
                currentNodeId   = nodeId,
                currentNodeName = nodeName,
                visitedNodeIds  = visited
            )
        )
        QuizPrepareManager.prepareOnNodeScan(_state.value.tripId, visited)
    }

    fun updateLocation(lat: Double, lng: Double) {
        _state.value = _state.value.copy(lastLat = lat, lastLng = lng)
    }

    fun isActive(): Boolean = _state.value.isTripActive

    fun current(): TripSnapshot = _state.value

    /**
     * Fire-and-forget call to POST /trips/end. Runs on [appScope] so the
     * request finishes even after the user navigates away from
     * NodeDetailScreen (which previously cancelled the in-flight HTTP call
     * via viewModelScope, leaving user_visit_history empty).
     *
     * Safe to call when there is no active server-side trip — it no-ops if
     * tripId is 0.
     */
    fun endTripOnServer(
        visitedNodeIds: List<Int> = _state.value.visitedNodeIds,
        lastLat:        Double    = _state.value.lastLat,
        lastLng:        Double    = _state.value.lastLng,
    ) {
        val trip = _state.value
        if (trip.tripId == 0) {
            Log.w(TAG, "endTripOnServer skipped: no server trip_id (was the trip started locally?)")
            return
        }
        val tripId = trip.tripId
        appScope.launch {
            try {
                val visitedNodes = visitedNodeIds.joinToString(",")
                val lat = lastLat.takeIf { it != 0.0 }
                val lng = lastLng.takeIf { it != 0.0 }
                val resp = HumsafarClient.api.endTrip(
                    tripId       = tripId,
                    visitedNodes = visitedNodes,
                    entryLat     = lat,
                    entryLng     = lng
                )
                if (!resp.isSuccessful) {
                    Log.w(TAG, "POST /trips/end returned ${resp.code()} for trip_id=$tripId")
                } else {
                    Log.i(TAG, "Trip $tripId ended successfully on server")
                }
            } catch (e: Exception) {
                Log.e(TAG, "POST /trips/end failed for trip_id=$tripId", e)
            }
        }
    }

    fun clear() {
        write(TripSnapshot())
    }

    private fun write(snapshot: TripSnapshot) {
        _state.value = snapshot
        prefs.edit()
            .putInt("tripId",            snapshot.tripId)
            .putInt("siteId",            snapshot.siteId)
            .putString("siteName",       snapshot.siteName)
            .putInt("currentNodeId",     snapshot.currentNodeId)
            .putString("currentNodeName", snapshot.currentNodeName)
            .putBoolean("isTripActive",  snapshot.isTripActive)
            .putLong("startedAtMs",      snapshot.startedAtMs)
            .putFloat("lastLat",         snapshot.lastLat.toFloat())
            .putFloat("lastLng",         snapshot.lastLng.toFloat())
            .putString("visitedNodes",   snapshot.visitedNodeIds.joinToString(","))
            .apply()
    }
}