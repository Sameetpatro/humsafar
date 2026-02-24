package com.example.humsafar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.data.TripManager
import com.example.humsafar.models.*
import com.example.humsafar.network.SiteClient
import com.example.humsafar.utils.haversineDistance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NodeDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<NodeDetailUiState>(NodeDetailUiState.Loading)
    val uiState: StateFlow<NodeDetailUiState> = _uiState.asStateFlow()

    private val _directionState = MutableStateFlow<DirectionInfo?>(null)
    val directionState: StateFlow<DirectionInfo?> = _directionState.asStateFlow()

    val videoViewModel = VideoViewModel()

    fun loadNode(nodeId: Long) = viewModelScope.launch {
        _uiState.value = NodeDetailUiState.Loading
        try {
            val scanResp = SiteClient.api.scanNode(nodeId)
            if (!scanResp.isSuccessful) {
                _uiState.value = NodeDetailUiState.Error("Failed to load node")
                return@launch
            }
            val data = scanResp.body()!!
            val monumentId = data.node.monumentId

            // Load all nodes + nearby in parallel
            val nodesResp  = SiteClient.api.getNodesForMonument(monumentId)
            val nearbyResp = SiteClient.api.getNearbyPlaces(monumentId)

            val allNodes = if (nodesResp.isSuccessful) nodesResp.body()!! else emptyList()
            val nearby   = if (nearbyResp.isSuccessful) nearbyResp.body()!! else emptyList()

            TripManager.cachedNodes = allNodes

            _uiState.value = NodeDetailUiState.Ready(
                node        = data.node,
                allNodes    = allNodes,
                nearbyPlaces = nearby,
                recommendedNext = data.recommendedNext
            )
        } catch (e: Exception) {
            _uiState.value = NodeDetailUiState.Error(e.message ?: "Unknown error")
        }
    }

    fun requestDirection() {
        val trip = TripManager.current()
        val state = _uiState.value as? NodeDetailUiState.Ready ?: return
        val lat  = trip.lastLat
        val lng  = trip.lastLng

        // Use Haversine to find nearest + recommend next unvisited
        val nearest = TripManager.nearestNode(lat, lng)
        val next    = TripManager.nextRecommendedNode()
            ?: state.recommendedNext

        val dist = nearest?.let {
            haversineDistance(lat, lng, it.latitude, it.longitude)
        } ?: 0.0

        _directionState.value = DirectionInfo(
            nearestNodeName     = nearest?.name ?: "Unknown",
            recommendedNodeName = next?.name ?: "End of tour",
            distanceMeters      = dist
        )
    }

    fun dismissDirection() { _directionState.value = null }

    fun endTrip() = viewModelScope.launch {
        try {
            SiteClient.api.endSession(TripManager.USER_ID)
        } catch (_: Exception) { }
        TripManager.clear()
    }

    fun startTripFromNormal(node: MonumentNode, alreadyVisited: List<Long>) = viewModelScope.launch {
        try {
            val resp = SiteClient.api.startSession(TripManager.USER_ID, node.monumentId)
            if (resp.isSuccessful) {
                TripManager.activateTrip(node.monumentId, node.name, node.id, node.name)
                // Mark already-visited nodes
                alreadyVisited.forEach { id ->
                    TripManager.cachedNodes.find { it.id == id }?.let {
                        TripManager.updateCurrentNode(it)
                    }
                }
                TripManager.updateCurrentNode(node)
            }
        } catch (_: Exception) { }
    }
}

sealed class NodeDetailUiState {
    data object Loading : NodeDetailUiState()
    data class Ready(
        val node: MonumentNode,
        val allNodes: List<MonumentNode>,
        val nearbyPlaces: List<NearbyPlace>,
        val recommendedNext: MonumentNode?
    ) : NodeDetailUiState()
    data class Error(val message: String) : NodeDetailUiState()
}