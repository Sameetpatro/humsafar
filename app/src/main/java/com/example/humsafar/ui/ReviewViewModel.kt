package com.example.humsafar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.data.TripManager
import com.example.humsafar.models.ReviewSubmitRequest
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReviewSubmitState {
    data object Idle : ReviewSubmitState()
    data object Submitting : ReviewSubmitState()
    data object Success : ReviewSubmitState()
    data class Error(val message: String) : ReviewSubmitState()
}

class ReviewViewModel : ViewModel() {
    private val _submitState = MutableStateFlow<ReviewSubmitState>(ReviewSubmitState.Idle)
    val submitState: StateFlow<ReviewSubmitState> = _submitState.asStateFlow()

    fun submitReview(
        tripId: Int,
        siteId: Int,
        starRating: Int,
        q1: Int,
        q2: Int,
        q3: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        _submitState.value = ReviewSubmitState.Submitting
        try {
            val request = ReviewSubmitRequest(
                tripId = tripId,
                siteId = siteId,
                userId = TripManager.USER_ID,
                starRating = starRating,
                q1 = q1,
                q2 = q2,
                q3 = q3,
                suggestionText = null
            )
            val resp = HumsafarClient.api.submitReview(request)
            if (resp.isSuccessful) {
                _submitState.value = ReviewSubmitState.Success
                onSuccess()
            } else {
                val msg = resp.errorBody()?.string() ?: "Failed to submit review"
                _submitState.value = ReviewSubmitState.Error(msg)
                onError(msg)
            }
        } catch (e: Exception) {
            _submitState.value = ReviewSubmitState.Error(e.message ?: "Connection error")
            onError(e.message ?: "Connection error")
        }
    }

    fun resetState() {
        _submitState.value = ReviewSubmitState.Idle
    }
}
