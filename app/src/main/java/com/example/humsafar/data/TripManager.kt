// app/src/main/java/com/example/humsafar/data/TripManager.kt
// REWRITTEN — uses FastAPI trip_id (Int) instead of Spring Boot sessionId (String).

package com.example.humsafar.data

import android.content.Context
import android.content.SharedPreferences
import com.example.humsafar.models.TripSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TripManager {

    private lateinit var prefs: SharedPreferences

    private val _state = MutableStateFlow(TripSnapshot())
    val state: StateFlow<TripSnapshot> = _state.asStateFlow()

    // Hardcoded guest user — replace with auth UID when you add login
    const val USER_ID = "guest_user_001"

    fun init(context: Context) {
        prefs = context.getSharedPreferences("trip_prefs_v2", Context.MODE_PRIVATE)
        _state.value = TripSnapshot(
            tripId          = prefs.getInt("tripId", 0),
            siteId          = prefs.getInt("siteId", 0),
            siteName        = prefs.getString("siteName", "") ?: "",
            currentNodeId   = prefs.getInt("currentNodeId", 0),
            currentNodeName = prefs.getString("currentNodeName", "") ?: "",
            isTripActive    = prefs.getBoolean("isTripActive", false),
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
        write(
            _state.value.copy(
                tripId          = tripId,
                siteId          = siteId,
                siteName        = siteName,
                currentNodeId   = nodeId,
                currentNodeName = nodeName,
                isTripActive    = true,
                visitedNodeIds  = listOf(nodeId)
            )
        )
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
    }

    fun updateLocation(lat: Double, lng: Double) {
        _state.value = _state.value.copy(lastLat = lat, lastLng = lng)
    }

    fun isActive(): Boolean = _state.value.isTripActive

    fun current(): TripSnapshot = _state.value

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
            .putFloat("lastLat",         snapshot.lastLat.toFloat())
            .putFloat("lastLng",         snapshot.lastLng.toFloat())
            .putString("visitedNodes",   snapshot.visitedNodeIds.joinToString(","))
            .apply()
    }
}