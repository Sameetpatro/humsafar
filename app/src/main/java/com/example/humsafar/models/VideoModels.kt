// app/src/main/java/com/example/humsafar/models/VideoModels.kt

package com.example.humsafar.models

import com.google.gson.annotations.SerializedName

// ── Network response models ───────────────────────────────────────────────────
data class VideoStatusResponse(
    @SerializedName("status")     val status: String,
    @SerializedName("url")        val url: String? = null,
    @SerializedName("progress")   val progress: Int = 0,
    @SerializedName("message")    val message: String = ""
)

data class GenerateVideoRequest(
    @SerializedName("prompt")     val prompt: String = "",
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
    data object Hidden          : VideoUiState()   // nothing shown
    data object Loading         : VideoUiState()   // brief pre-generation pause
    data class  Generating(
        val progress: Int    = 0,
        val message:  String = "Starting…"
    )                           : VideoUiState()
    data class  ReadyToPlay(val videoUrl: String) : VideoUiState()
    data class  Error(val message: String)        : VideoUiState()
}

// ── Cinematic loader animation stages ────────────────────────────────────────
val LOADER_STAGES = listOf(
    "Analyzing content…",
    "Crafting cinematic scenes…",
    "Adding narration & visuals…"
)