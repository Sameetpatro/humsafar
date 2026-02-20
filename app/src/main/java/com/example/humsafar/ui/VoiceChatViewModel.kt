// app/src/main/java/com/example/humsafar/ui/VoiceChatViewModel.kt
// NEW FILE

package com.example.humsafar.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.audio.AudioPlayer
import com.example.humsafar.audio.AudioRecorder
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

/**
 * AndroidViewModel (not ViewModel) because we need Application context for:
 *   • AudioRecord (system service)
 *   • MediaPlayer temp file (cacheDir)
 *   • SharedPreferences (LanguagePreferences)
 *
 * Owns AudioRecorder + AudioPlayer so they survive config changes.
 * If the user rotates mid-recording, capture continues uninterrupted.
 */
class VoiceChatViewModel(app: Application) : AndroidViewModel(app) {

    private val recorder  = AudioRecorder()
    private val player    = AudioPlayer()
    val langPrefs         = LanguagePreferences(app)

    // Site context set by the host composable before recording starts
    var currentSiteName: String = "Heritage Site"
    var currentSiteId:   String = ""

    // ── Exposed state ─────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    // Full conversation displayed as cards in the voice screen
    private val _messages = MutableStateFlow<List<VoiceChatResponse>>(emptyList())
    val messages: StateFlow<List<VoiceChatResponse>> = _messages.asStateFlow()

    private var recordingJob: Job? = null

    // ── Recording ─────────────────────────────────────────────────────────

    /** Called when the mic button is pressed DOWN. */
    fun startRecording() {
        if (_uiState.value !is VoiceUiState.Idle) return
        _uiState.value = VoiceUiState.Recording

        recordingJob = viewModelScope.launch {
            try {
                // startRecording() suspends here until stopRecording() is called
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

    /** Called when the mic button is released (or tapped again). */
    fun stopAndSend() {
        if (_uiState.value !is VoiceUiState.Recording) return
        recorder.stopRecording()
        // recordingJob continues from startRecording() → hits sendToBackend()
    }

    // ── Backend ───────────────────────────────────────────────────────────

    private suspend fun sendToBackend(wavBytes: ByteArray) {
        val lang = langPrefs.selectedLanguage

        try {
            val audioPart = MultipartBody.Part.createFormData(
                name     = "audio",
                filename = "recording.wav",
                body     = wavBytes.toRequestBody("audio/wav".toMediaType())
            )

            val response = VoiceRetrofitClient.api.sendVoiceMessage(
                audio    = audioPart,
                siteName = currentSiteName.toRequestBody("text/plain".toMediaType()),
                siteId   = currentSiteId.toRequestBody("text/plain".toMediaType()),
                language = lang.bcp47Code.toRequestBody("text/plain".toMediaType()),
                langName = lang.name.toRequestBody("text/plain".toMediaType())
            )

            Log.i(TAG, "Response: userText='${response.userText.take(60)}'")
            _messages.value = _messages.value + response
            _uiState.value  = VoiceUiState.Success(response)

            // Auto-play TTS audio; await completion before resetting to Idle
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