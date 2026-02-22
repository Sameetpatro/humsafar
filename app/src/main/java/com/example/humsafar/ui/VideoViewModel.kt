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

    private val _uiState = MutableStateFlow<VideoUiState>(VideoUiState.Hidden)
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    fun requestVideo(botText: String, siteName: String, siteId: String) {
        if (_uiState.value is VideoUiState.Generating) return
        _uiState.value = VideoUiState.Loading

        viewModelScope.launch {
            delay(800)
            _uiState.value = VideoUiState.Generating(0, "Starting…")
            try {
                val resp = VideoServiceClient.api.generateVideo(
                    GenerateVideoRequest(botText = botText, siteName = siteName, siteId = siteId)
                )
                Log.i(TAG, "job_id=${resp.jobId}")
                pollStatus(resp.jobId)
            } catch (e: Exception) {
                Log.e(TAG, "Start failed", e)
                _uiState.value = VideoUiState.Error("Could not start: ${e.message}")
            }
        }
    }

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
                    Log.i(TAG, "Poll $polls: ${s.status} ${s.progress}%")
                    when (s.status) {
                        "ready"  -> {
                            _uiState.value = if (!s.videoUrl.isNullOrBlank())
                                VideoUiState.ReadyToPlay(s.videoUrl)
                            else
                                VideoUiState.Error("URL missing")
                            return@launch
                        }
                        "failed" -> {
                            _uiState.value = VideoUiState.Error(s.message.ifBlank { "Failed" })
                            return@launch
                        }
                        else -> _uiState.value = VideoUiState.Generating(
                            s.progress, s.message.ifBlank { "Processing…" }
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Poll error: ${e.message}")
                }
            }
            _uiState.value = VideoUiState.Error("Timed out — please try again")
        }
    }

    override fun onCleared() { pollJob?.cancel(); super.onCleared() }
}