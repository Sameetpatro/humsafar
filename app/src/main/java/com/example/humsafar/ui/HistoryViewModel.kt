// app/src/main/java/com/example/humsafar/ui/HistoryViewModel.kt
//
// Drives the History screen.
// Endpoint: GET /reviews/users/{firebase_uid}/history
// The list comes from the user_visit_history table — each row is one
// trip the user has completed, with timing, node-completion stats, and
// whether they've already submitted a review for that trip.

package com.example.humsafar.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.models.VisitHistoryItem
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "HistoryViewModel"

sealed class HistoryUiState {
    data object Loading                                       : HistoryUiState()
    data object NotLoggedIn                                   : HistoryUiState()
    data object Empty                                         : HistoryUiState()
    data class  Ready(val items: List<VisitHistoryItem>)      : HistoryUiState()
    data class  Error(val message: String)                    : HistoryUiState()
}

class HistoryViewModel : ViewModel() {

    private val _state = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    fun refresh() = load(showLoadingState = false)

    fun load(showLoadingState: Boolean = true) = viewModelScope.launch {
        val firebaseUid = AuthManager.currentUser.value?.uid
        if (firebaseUid.isNullOrBlank()) {
            _state.value = HistoryUiState.NotLoggedIn
            return@launch
        }

        if (showLoadingState && _state.value !is HistoryUiState.Ready) {
            _state.value = HistoryUiState.Loading
        }
        _refreshing.value = true

        try {
            val resp = HumsafarClient.api.getUserVisitHistory(firebaseUid)
            if (resp.isSuccessful) {
                val items = resp.body().orEmpty()
                Log.i(TAG, "Loaded ${items.size} history entries for $firebaseUid")
                _state.value = if (items.isEmpty()) {
                    HistoryUiState.Empty
                } else {
                    HistoryUiState.Ready(items)
                }
            } else if (resp.code() == 404) {
                // User not yet registered on backend — treat as empty rather
                // than error; the UserRepository sync will catch up shortly.
                Log.w(TAG, "User not registered on backend yet (404)")
                _state.value = HistoryUiState.Empty
            } else {
                val err = "Failed to load history (${resp.code()})"
                Log.w(TAG, err)
                _state.value = HistoryUiState.Error(err)
            }
        } catch (e: Exception) {
            Log.e(TAG, "load history exception: ${e.message}", e)
            _state.value = HistoryUiState.Error(e.message ?: "Connection error")
        } finally {
            _refreshing.value = false
        }
    }
}
