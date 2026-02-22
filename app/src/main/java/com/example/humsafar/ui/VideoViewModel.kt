// app/src/main/java/com/example/humsafar/ui/VideoViewModel.kt

package com.example.humsafar.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.models.VideoUiState
import com.example.humsafar.network.GenerateVideoRequest
import com.example.humsafar.network.VideoServiceClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG              = "VideoViewModel"
private const val POLL_INTERVAL_MS = 3_000L
private const val MAX_POLLS        = 60

class VideoViewModel : ViewModel() {

    // Start hidden — nothing shown until user taps "Watch as Video"
    private val _uiState = MutableStateFlow<VideoUiState>(VideoUiState.Hidden)
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    fun requestVideo(botText: String, siteName: String, siteId: String) {
        // Don't start a second job if one is already running
        if (_uiState.value is VideoUiState.Generating) return

        _uiState.value = VideoUiState.Loading

        viewModelScope.launch {
            // Brief pause so the Loading animation is visible before the API call
            delay(400)
            try {
                val resp = VideoServiceClient.api.generateVideo(
                    GenerateVideoRequest(
                        botText  = botText,
                        siteName = siteName,
                        siteId   = siteId
                    )
                )
                Log.i(TAG, "Job started: job_id=${resp.jobId}")
                pollStatus(resp.jobId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start generation", e)
                _uiState.value = VideoUiState.Error("Could not start: ${e.message}")
            }
        }
    }

    /** Called by the close/cancel button in both the loader and player overlays. */
    fun dismiss() {
        pollJob?.cancel()
        _uiState.value = VideoUiState.Hidden
    }

    private fun pollStatus(jobId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var polls = 0
            while (polls < MAX_POLLS) {
                delay(POLL_INTERVAL_MS)
                polls++
                try {
                    val s = VideoServiceClient.api.getStatus(jobId)
                    Log.i(TAG, "Poll $polls/${MAX_POLLS}: status=${s.status} progress=${s.progress}%")
                    when (s.status) {
                        "ready" -> {
                            _uiState.value = if (!s.videoUrl.isNullOrBlank())
                                VideoUiState.ReadyToPlay(s.videoUrl)
                            else
                                VideoUiState.Error("Video URL missing from response")
                            return@launch
                        }
                        "failed" -> {
                            _uiState.value = VideoUiState.Error(
                                s.message.ifBlank { "Generation failed on server" }
                            )
                            return@launch
                        }
                        else -> {
                            // "generating" or any other in-progress status
                            _uiState.value = VideoUiState.Generating(
                                progress = s.progress,
                                message  = s.message.ifBlank { "Processing…" }
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Transient network error — log and keep polling
                    Log.w(TAG, "Poll error (will retry): ${e.message}")
                }
            }
            _uiState.value = VideoUiState.Error(
                "Timed out after ${MAX_POLLS * POLL_INTERVAL_MS / 1000}s — please try again"
            )
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}