package com.example.humsafar.network

data class ChatRequest(
    val message: String,
    val site_name: String,
    val site_id: String,
    val history: List<Map<String, String>>
)

data class ChatResponse(
    val reply: String
)
