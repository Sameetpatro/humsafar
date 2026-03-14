// app/src/main/java/com/example/humsafar/ui/NodeDetailViewModel.kt
// UPDATED: keeps exact same pattern as original but now Node.images is populated
// (no changes needed here — images come from the updated ApiModels.kt + backend)

package com.example.humsafar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.data.TripManager
import com.example.humsafar.network.SiteDetail
import com.example.humsafar.network.Node
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NodeDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<NodeDetailUiState>(NodeDetailUiState.Loading)
    val uiState: StateFlow<NodeDetailUiState> = _uiState.asStateFlow()

    fun loadNode(nodeId: Int, siteId: Int) = viewModelScope.launch {
        _uiState.value = NodeDetailUiState.Loading
        try {
            val resp = HumsafarClient.api.getSiteDetail(siteId)
            if (!resp.isSuccessful || resp.body() == null) {
                _uiState.value = NodeDetailUiState.Error("Could not load site (${resp.code()})")
                return@launch
            }

            val site = resp.body()!!
            val node = site.nodes.find { it.id == nodeId }
                ?: run {
                    _uiState.value = NodeDetailUiState.Error("Node not found in site")
                    return@launch
                }

            _uiState.value = NodeDetailUiState.Ready(
                node     = node,
                site     = site,
                allNodes = site.nodes.sortedBy { it.sequenceOrder }
            )
        } catch (e: Exception) {
            _uiState.value = NodeDetailUiState.Error(e.message ?: "Unknown error")
        }
    }

    /** Ends trip via API (user_visit_history inserted). Does NOT clear TripManager — clear when leaving TripCompletionScreen. */
    fun endTrip(
        visitedNodeIds: List<Int>,
        lastLat: Double,
        lastLng: Double
    ) = viewModelScope.launch {
        val trip = TripManager.current()
        if (trip.tripId != 0) {
            try {
                val visitedNodes = visitedNodeIds.joinToString(",")
                val lat = if (lastLat != 0.0) lastLat else null
                val lng = if (lastLng != 0.0) lastLng else null
                HumsafarClient.api.endTrip(trip.tripId, visitedNodes, lat, lng)
            } catch (_: Exception) { }
        }
    }
}

sealed class NodeDetailUiState {
    data object Loading : NodeDetailUiState()

    data class Ready(
        val node:     Node,
        val site:     SiteDetail,
        val allNodes: List<Node>
    ) : NodeDetailUiState()

    data class Error(val message: String) : NodeDetailUiState()
}