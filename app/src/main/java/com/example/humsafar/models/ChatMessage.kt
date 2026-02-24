// app/src/main/java/com/example/humsafar/models/ChatMessage.kt

package com.example.humsafar.models

data class ChatMessage(
    val text:      String,
    val isUser:    Boolean,
    val isLoading: Boolean = false
)
