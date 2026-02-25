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

    fun endTrip() = viewModelScope.launch {
        val tripId = TripManager.current().tripId
        if (tripId != 0) {
            try {
                HumsafarClient.api.endTrip(tripId)
            } catch (_: Exception) { }
        }
        TripManager.clear()
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