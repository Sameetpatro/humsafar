package com.example.humsafar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.models.NodeInsights
import com.example.humsafar.models.SiteInsights
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface InsightsUiState {
    data object Loading : InsightsUiState
    data class Ready(val site: SiteInsights) : InsightsUiState
    data class Error(val message: String) : InsightsUiState
}

class InsightsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<InsightsUiState>(InsightsUiState.Loading)
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    private val _nodeInsight = MutableStateFlow<NodeInsights?>(null)
    val nodeInsight: StateFlow<NodeInsights?> = _nodeInsight.asStateFlow()

    fun loadSite(siteId: Int) {
        _uiState.value = InsightsUiState.Loading
        viewModelScope.launch {
            runCatching {
                val resp = HumsafarClient.api.getSiteInsights(siteId)
                if (resp.isSuccessful && resp.body() != null) {
                    _uiState.value = InsightsUiState.Ready(resp.body()!!)
                } else {
                    _uiState.value = InsightsUiState.Error("Couldn't load insights (HTTP ${resp.code()})")
                }
            }.onFailure {
                _uiState.value = InsightsUiState.Error(it.message ?: "Network error")
            }
        }
    }

    fun loadNode(nodeId: Int) {
        viewModelScope.launch {
            runCatching {
                val resp = HumsafarClient.api.getNodeInsights(nodeId)
                if (resp.isSuccessful) _nodeInsight.value = resp.body()
            }
        }
    }
}
