// app/src/main/java/com/example/humsafar/models/VideoModels.kt

package com.example.humsafar.models

import com.google.gson.annotations.SerializedName

// ── Video type enum ───────────────────────────────────────────────────────────
enum class VideoType { OVERVIEW, PROMPT }

// ── Video generation status ───────────────────────────────────────────────────
enum class VideoStatus { NOT_STARTED, GENERATING, READY, FAILED }

// ── Updated ChatMessage with video metadata ───────────────────────────────────
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false,
    // Video feature flags
    val isBot: Boolean = !isUser,
    val videoAvailable: Boolean = false,
    val videoType: VideoType = VideoType.PROMPT,
    val videoId: String = "",           // monument_id for OVERVIEW, hash for PROMPT
    val userPrompt: String = ""         // original user query for PROMPT videos
)

// ── Network response models ───────────────────────────────────────────────────
data class VideoStatusResponse(
    @SerializedName("status")     val status: String,     // "ready" | "generating" | "failed"
    @SerializedName("url")        val url: String? = null,
    @SerializedName("progress")   val progress: Int = 0,  // 0–100
    @SerializedName("message")    val message: String = ""
)

data class GenerateVideoRequest(
    @SerializedName("prompt")     val prompt: String,
    @SerializedName("bot_text")   val botText: String,
    @SerializedName("site_id")    val siteId: String,
    @SerializedName("site_name")  val siteName: String
)

data class GenerateVideoResponse(
    @SerializedName("hash")       val hash: String,
    @SerializedName("status")     val status: String,
    @SerializedName("url")        val url: String? = null
)

// ── UI state for video loading ────────────────────────────────────────────────
sealed class VideoUiState {
    data object Hidden : VideoUiState()
    data object CinematicLoader : VideoUiState()    // artificial 1.5s delay
    data class Generating(val progress: Int, val stage: String) : VideoUiState()
    data class ReadyToPlay(val videoUrl: String) : VideoUiState()
    data class Error(val message: String) : VideoUiState()
}

// ── Cinematic loader animation stages ────────────────────────────────────────
val LOADER_STAGES = listOf(
    "Analyzing content…",
    "Crafting cinematic scenes…",
    "Adding narration & visuals…"
)