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
        suggestionText: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {
        // Guard against an unsynced/legacy local trip (tripId == 0). Without a
        // real server trip_id, /reviews/submit fails the FK check on trip_reviews.
        if (tripId <= 0) {
            val msg = "This trip wasn't synced with the server. " +
                    "Please rescan a node QR to start a fresh trip before reviewing."
            _submitState.value = ReviewSubmitState.Error(msg)
            onError(msg)
            return@launch
        }
        _submitState.value = ReviewSubmitState.Submitting
        try {
            val request = ReviewSubmitRequest(
                tripId         = tripId,
                siteId         = siteId,
                firebaseUid    = TripManager.USER_ID,
                starRating     = starRating,
                q1             = q1,
                q2             = q2,
                q3             = q3,
                suggestionText = suggestionText?.takeIf { it.isNotBlank() }
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
