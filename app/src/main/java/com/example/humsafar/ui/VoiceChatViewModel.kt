// app/src/main/java/com/example/humsafar/ui/VoiceChatViewModel.kt

package com.example.humsafar.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.audio.AudioPlayer
import com.example.humsafar.audio.AudioRecorder
import com.example.humsafar.data.ActiveSiteManager
import com.example.humsafar.models.VoiceChatResponse
import com.example.humsafar.models.VoiceUiState
import com.example.humsafar.network.VoiceRetrofitClient
import com.example.humsafar.prefs.LanguagePreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private const val TAG = "VoiceChatVM"

class VoiceChatViewModel(app: Application) : AndroidViewModel(app) {

    private val recorder  = AudioRecorder()
    private val player    = AudioPlayer()
    val langPrefs         = LanguagePreferences(app)

    // Set by VoiceChatScreen from nav args
    var currentSiteName: String = "Heritage Site"
    var currentSiteId:   String = ""   // always the SITE id (heritage_sites.id)
    var currentNodeId:   String = ""   // node id if launched from a node, else ""

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<VoiceChatResponse>>(emptyList())
    val messages: StateFlow<List<VoiceChatResponse>> = _messages.asStateFlow()

    private var recordingJob: Job? = null

    fun startRecording() {
        if (_uiState.value !is VoiceUiState.Idle) return
        _uiState.value = VoiceUiState.Recording

        recordingJob = viewModelScope.launch {
            try {
                val wavBytes = recorder.startRecording()
                Log.i(TAG, "Captured ${wavBytes.size} bytes")
                _uiState.value = VoiceUiState.Processing
                sendToBackend(wavBytes)
            } catch (e: Exception) {
                Log.e(TAG, "Recording failed", e)
                _uiState.value = VoiceUiState.Error(e.message ?: "Recording failed")
            }
        }
    }

    fun stopAndSend() {
        if (_uiState.value !is VoiceUiState.Recording) return
        recorder.stopRecording()
    }

    private suspend fun sendToBackend(wavBytes: ByteArray) {
        val lang = langPrefs.selectedLanguage

        // ── Resolve site_id ───────────────────────────────────────────────
        // Priority: currentSiteId from nav args → ActiveSiteManager fallback
        val resolvedSiteId = currentSiteId.ifBlank {
            ActiveSiteManager.activeSiteId?.toString() ?: ""
        }

        // ── Resolve node_id ───────────────────────────────────────────────
        // Priority: currentNodeId from nav args → ActiveSiteManager fallback
        val resolvedNodeId = currentNodeId.ifBlank {
            ActiveSiteManager.activeNodeId.value?.toString() ?: ""
        }

        Log.i(TAG, "Sending voice: site=$resolvedSiteId node=$resolvedNodeId lang=${lang.name}")

        try {
            val audioPart = MultipartBody.Part.createFormData(
                name     = "audio",
                filename = "recording.wav",
                body     = wavBytes.toRequestBody("audio/wav".toMediaType())
            )

            val response = VoiceRetrofitClient.api.sendVoiceMessage(
                audio    = audioPart,
                siteName = currentSiteName.toRequestBody("text/plain".toMediaType()),
                siteId   = resolvedSiteId.toRequestBody("text/plain".toMediaType()),
                language = lang.bcp47Code.toRequestBody("text/plain".toMediaType()),
                langName = lang.name.toRequestBody("text/plain".toMediaType()),
                nodeId   = resolvedNodeId.toRequestBody("text/plain".toMediaType())
            )

            Log.i(TAG, "Response: userText='${response.userText.take(60)}'")
            _messages.value = _messages.value + response
            _uiState.value  = VoiceUiState.Success(response)

            player.play(
                base64Audio = response.audioBase64,
                cacheDir    = getApplication<Application>().cacheDir,
                format      = response.audioFormat
            )

        } catch (e: Exception) {
            Log.e(TAG, "Backend call failed", e)
            _uiState.value = VoiceUiState.Error(e.message ?: "Network error")
        } finally {
            _uiState.value = VoiceUiState.Idle
        }
    }

    fun resetError() { _uiState.value = VoiceUiState.Idle }

    override fun onCleared() {
        recorder.stopRecording()
        player.stopPlayback()
        recordingJob?.cancel()
        super.onCleared()
    }
}