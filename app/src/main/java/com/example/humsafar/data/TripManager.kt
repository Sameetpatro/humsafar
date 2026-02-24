package com.example.humsafar.data

import android.content.Context
import android.content.SharedPreferences
import com.example.humsafar.models.MonumentNode
import com.example.humsafar.models.TripSnapshot
import com.example.humsafar.utils.haversineDistance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TripManager {

    private lateinit var prefs: SharedPreferences

    private val _state = MutableStateFlow(TripSnapshot())
    val state: StateFlow<TripSnapshot> = _state.asStateFlow()

    var cachedNodes: List<MonumentNode> = emptyList()

    const val USER_ID = "guest_user_001"

    fun init(context: Context) {
        prefs = context.getSharedPreferences("trip_prefs", Context.MODE_PRIVATE)
        _state.value = TripSnapshot(
            sessionId       = prefs.getString("sessionId", "") ?: "",
            monumentId      = prefs.getLong("monumentId", 0L),
            monumentName    = prefs.getString("monumentName", "") ?: "",
            currentNodeId   = prefs.getLong("currentNodeId", 0L),
            currentNodeName = prefs.getString("currentNodeName", "") ?: "",
            isTripActive    = prefs.getBoolean("isTripActive", false),
            lastLat         = prefs.getFloat("lastLat", 0f).toDouble(),
            lastLng         = prefs.getFloat("lastLng", 0f).toDouble(),
            visitedNodeIds  = prefs.getString("visitedNodes", "")
                ?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
        )
    }

    fun activateTrip(monumentId: Long, monumentName: String, nodeId: Long, nodeName: String) {
        write(
            _state.value.copy(
                monumentId      = monumentId,
                monumentName    = monumentName,
                currentNodeId   = nodeId,
                currentNodeName = nodeName,
                isTripActive    = true,
                visitedNodeIds  = listOf(nodeId)
            )
        )
    }

    fun updateCurrentNode(node: MonumentNode) {
        val visited = (_state.value.visitedNodeIds + node.id).distinct()
        write(
            _state.value.copy(
                currentNodeId   = node.id,
                currentNodeName = node.name,
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
        cachedNodes = emptyList()
    }

    fun nearestNode(lat: Double, lng: Double): MonumentNode? =
        cachedNodes.minByOrNull { haversineDistance(lat, lng, it.latitude, it.longitude) }

    fun nextRecommendedNode(): MonumentNode? {
        val visited = _state.value.visitedNodeIds
        return cachedNodes
            .filter { it.nodeType != "KING" && it.id !in visited }
            .minByOrNull { it.visitOrder }
    }

    private fun write(snapshot: TripSnapshot) {
        _state.value = snapshot
        prefs.edit()
            .putString("sessionId",       snapshot.sessionId)
            .putLong("monumentId",        snapshot.monumentId)
            .putString("monumentName",    snapshot.monumentName)
            .putLong("currentNodeId",     snapshot.currentNodeId)
            .putString("currentNodeName", snapshot.currentNodeName)
            .putBoolean("isTripActive",   snapshot.isTripActive)
            .putFloat("lastLat",          snapshot.lastLat.toFloat())
            .putFloat("lastLng",          snapshot.lastLng.toFloat())
            .putString("visitedNodes",    snapshot.visitedNodeIds.joinToString(","))
            .apply()
    }
}