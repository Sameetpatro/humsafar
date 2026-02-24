// app/src/main/java/com/example/humsafar/ui/NodeDetailViewModel.kt
// REWRITTEN — loads node data from FastAPI /sites/{site_id} + TripManager state.

package com.example.humsafar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.data.TripManager
import com.example.humsafar.models.SiteDetail
import com.example.humsafar.models.SiteNode
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NodeDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<NodeDetailUiState>(NodeDetailUiState.Loading)
    val uiState: StateFlow<NodeDetailUiState> = _uiState.asStateFlow()

    /**
     * Load the node by fetching the full site, then picking the node by ID.
     * [nodeId] and [siteId] come from QrScanResult after a successful scan.
     */
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

    fun endTrip() = viewModelScope.launch {
        val tripId = TripManager.current().tripId
        if (tripId != 0) {
            try { HumsafarClient.api.endTrip(tripId) } catch (_: Exception) { }
        }
        TripManager.clear()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

sealed class NodeDetailUiState {
    data object Loading : NodeDetailUiState()

    data class Ready(
        val node:     SiteNode,
        val site:     SiteDetail,
        val allNodes: List<SiteNode>
    ) : NodeDetailUiState()

    data class Error(val message: String) : NodeDetailUiState()
}