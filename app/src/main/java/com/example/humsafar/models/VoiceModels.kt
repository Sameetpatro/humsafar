// app/src/main/java/com/example/humsafar/models/VoiceModels.kt
// NEW FILE

package com.example.humsafar.models

import com.google.gson.annotations.SerializedName

// ── Network response ──────────────────────────────────────────────────────────
data class VoiceChatResponse(
    @SerializedName("user_text")    val userText:    String,
    @SerializedName("bot_text")     val botText:     String,
    @SerializedName("audio_base64") val audioBase64: String,
    @SerializedName("audio_format") val audioFormat: String = "wav"
)

// ── UI state types ────────────────────────────────────────────────────────────
sealed class VoiceUiState {
    data object Idle       : VoiceUiState()
    data object Recording  : VoiceUiState()
    data object Processing : VoiceUiState()
    data class  Success(val response: VoiceChatResponse) : VoiceUiState()
    data class  Error(val message: String)               : VoiceUiState()
}