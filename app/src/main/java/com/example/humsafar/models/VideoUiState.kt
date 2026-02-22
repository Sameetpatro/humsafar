// app/src/main/java/com/example/humsafar/models/VideoUiState.kt
// Sealed class for video generation UI state.
// Kept in models so ChatbotActivity, VoiceChatScreen, and VideoViewModel
// all import from the same place — no ambiguity.

package com.example.humsafar.models

sealed class VideoUiState {
    data object Idle        : VideoUiState()
    data object Dismissed   : VideoUiState()

    data class Generating(
        val progress: Int    = 0,
        val message:  String = "Starting…"
    ) : VideoUiState()

    data class ReadyToPlay(
        val videoUrl: String
    ) : VideoUiState()

    data class Error(
        val message: String
    ) : VideoUiState()
}