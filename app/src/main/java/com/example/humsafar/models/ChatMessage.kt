// app/src/main/java/com/example/humsafar/models/ChatMessage.kt
// Shared chat message model used by ChatbotActivity and GlassChatBubble

package com.example.humsafar.models

enum class VideoType { OVERVIEW, PROMPT }

data class ChatMessage(
    val text:           String,
    val isUser:         Boolean,
    val isLoading:      Boolean = false,
    val videoAvailable: Boolean = false,
    val videoType:      VideoType = VideoType.PROMPT,
    val videoId:        String    = "",
    val userPrompt:     String    = ""
)