// app/src/main/java/com/example/humsafar/ui/HeritageDetailViewModel.kt
// NEW FILE — same pattern as NodeDetailViewModel, uses HumsafarClient

package com.example.humsafar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.network.SiteDetail
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HeritageDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HeritageDetailUiState>(HeritageDetailUiState.Loading)
    val uiState: StateFlow<HeritageDetailUiState> = _uiState.asStateFlow()

    private var lastLoadedId = -1

    fun loadSite(siteId: Int) = viewModelScope.launch {
        if (siteId == lastLoadedId && _uiState.value is HeritageDetailUiState.Ready) return@launch
        lastLoadedId = siteId
        _uiState.value = HeritageDetailUiState.Loading

        try {
            val resp = HumsafarClient.api.getSiteDetail(siteId)
            if (!resp.isSuccessful || resp.body() == null) {
                _uiState.value = HeritageDetailUiState.Error("Could not load site (${resp.code()})")
                return@launch
            }
            _uiState.value = HeritageDetailUiState.Ready(resp.body()!!)
        } catch (e: Exception) {
            _uiState.value = HeritageDetailUiState.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class HeritageDetailUiState {
    data object Loading : HeritageDetailUiState()
    data class Ready(val site: SiteDetail) : HeritageDetailUiState()
    data class Error(val message: String) : HeritageDetailUiState()
}