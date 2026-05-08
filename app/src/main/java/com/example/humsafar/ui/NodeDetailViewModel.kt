// app/src/main/java/com/example/humsafar/ui/NodeDetailViewModel.kt

package com.example.humsafar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.data.TripManager
import com.example.humsafar.models.AmenityResponse
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
            val node = site.nodes.find { it.id == nodeId } ?: run {
                _uiState.value = NodeDetailUiState.Error("Node not found in site")
                return@launch
            }

            // Load amenities in parallel — fail silently so main content still shows
            val amenities: List<AmenityResponse> = try {
                val aResp = HumsafarClient.api.getAmenitiesNearNode(nodeId, topN = 2)
                if (aResp.isSuccessful) aResp.body() ?: emptyList() else emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            _uiState.value = NodeDetailUiState.Ready(
                node      = node,
                site      = site,
                allNodes  = site.nodes.sortedBy { it.sequenceOrder },
                amenities = amenities
            )
        } catch (e: Exception) {
            _uiState.value = NodeDetailUiState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Ends trip via API (user_visit_history inserted).
     *
     * Implementation moved to [TripManager.endTripOnServer] which uses a
     * process-scoped CoroutineScope. The previous viewModelScope.launch
     * was being cancelled when the surrounding NavBackStackEntry was
     * destroyed by `popUpTo("home")` during navigation to ReviewScreen,
     * which is why visit history was showing up empty.
     *
     * Kept here as a thin shim for any legacy caller; new code should
     * call TripManager.endTripOnServer(...) directly.
     */
    fun endTrip(
        visitedNodeIds: List<Int>,
        lastLat: Double,
        lastLng: Double
    ) {
        TripManager.endTripOnServer(
            visitedNodeIds = visitedNodeIds,
            lastLat        = lastLat,
            lastLng        = lastLng
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

sealed class NodeDetailUiState {
    data object Loading : NodeDetailUiState()

    data class Ready(
        val node:      Node,
        val site:      SiteDetail,
        val allNodes:  List<Node>,
        val amenities: List<AmenityResponse> = emptyList()   // ← NEW
    ) : NodeDetailUiState()

    data class Error(val message: String) : NodeDetailUiState()
}