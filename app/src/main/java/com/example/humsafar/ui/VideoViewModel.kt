// app/src/main/java/com/example/humsafar/ui/VideoViewModel.kt
// Handles video generation polling with clear error states

package com.example.humsafar.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.network.GenerateVideoRequest
import com.example.humsafar.network.VideoRetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "VideoViewModel"
private const val POLL_INTERVAL_MS = 3000L
private const val MAX_POLLS        = 60  // 3 min max (60 × 3s)

sealed class VideoUiState {
    data object Idle                                  : VideoUiState()
    data class  Generating(val progress: Int, val message: String) : VideoUiState()
    data class  Ready(val videoUrl: String)           : VideoUiState()
    data class  Error(val message: String)            : VideoUiState()
}

class VideoViewModel : ViewModel() {

    private val _state = MutableStateFlow<VideoUiState>(VideoUiState.Idle)
    val state: StateFlow<VideoUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    /**
     * Call this when user taps "Watch as Video".
     * @param botText  The last chatbot response text to narrate
     * @param siteName Heritage site name
     * @param siteId   Heritage site ID
     */
    fun requestVideo(botText: String, siteName: String, siteId: String) {
        if (_state.value is VideoUiState.Generating) {
            Log.w(TAG, "Already generating — ignoring duplicate request")
            return
        }

        Log.i(TAG, "requestVideo: site=$siteName textLen=${botText.length}")
        _state.value = VideoUiState.Generating(0, "Starting…")

        viewModelScope.launch {
            try {
                // 1. POST /generate → get job_id
                Log.i(TAG, "Calling /generate…")
                val response = VideoRetrofitClient.api.generateVideo(
                    GenerateVideoRequest(
                        botText  = botText,
                        siteName = siteName,
                        siteId   = siteId
                    )
                )
                val jobId = response.jobId
                Log.i(TAG, "Job created: jobId=$jobId status=${response.status}")

                // 2. Poll /status/{jobId} until ready or failed
                startPolling(jobId)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start video generation", e)
                _state.value = VideoUiState.Error("Could not start: ${e.message}")
            }
        }
    }

    private fun startPolling(jobId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var polls = 0
            while (polls < MAX_POLLS) {
                delay(POLL_INTERVAL_MS)
                polls++

                try {
                    val status = VideoRetrofitClient.api.getStatus(jobId)
                    Log.i(TAG, "Poll $polls: status=${status.status} progress=${status.progress}% msg=${status.message}")

                    when (status.status) {
                        "ready" -> {
                            val url = status.videoUrl
                            if (url.isNullOrBlank()) {
                                _state.value = VideoUiState.Error("Video URL missing in ready response")
                            } else {
                                Log.i(TAG, "Video ready: $url")
                                _state.value = VideoUiState.Ready(url)
                            }
                            return@launch
                        }
                        "failed" -> {
                            Log.e(TAG, "Video generation failed: ${status.message}")
                            _state.value = VideoUiState.Error(status.message.ifBlank { "Generation failed" })
                            return@launch
                        }
                        else -> {
                            // still generating
                            _state.value = VideoUiState.Generating(
                                progress = status.progress,
                                message  = status.message.ifBlank { "Processing…" }
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Poll $polls failed: ${e.message}")
                    // Don't fail on a single poll error — backend might be busy
                    if (polls >= MAX_POLLS) {
                        _state.value = VideoUiState.Error("Timed out after ${MAX_POLLS * POLL_INTERVAL_MS / 1000}s")
                    }
                }
            }

            // Exhausted all polls
            _state.value = VideoUiState.Error("Generation timed out — please try again")
        }
    }

    fun reset() {
        pollJob?.cancel()
        _state.value = VideoUiState.Idle
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}