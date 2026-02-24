// app/src/main/java/com/example/humsafar/models/TripModels.kt
package com.example.humsafar.models

import com.google.gson.annotations.SerializedName

// ── QR ────────────────────────────────────────────────────────────────────────
data class QrValidationResponse(
    @SerializedName("nodeId")          val nodeId: String,
    @SerializedName("nodeType")        val nodeType: String,      // "KING" | "NORMAL"
    @SerializedName("tripId")          val tripId: String?,       // non-null if KING
    @SerializedName("recommendedNode") val recommendedNode: String?,
    @SerializedName("metadata")        val metadata: NodeMetadata?
)

// ── Trip ──────────────────────────────────────────────────────────────────────
data class TripStartResponse(
    @SerializedName("tripId") val tripId: String
)

data class TripSnapshot(
    val tripId: String       = "",
    val currentNodeId: String = "",
    val isTripActive: Boolean = false,
    val lastLat: Double      = 0.0,
    val lastLng: Double      = 0.0,
    val pendingEnd: Boolean  = false
)

// ── Node ──────────────────────────────────────────────────────────────────────
data class NodeMetadataResponse(
    @SerializedName("nodeId")      val nodeId: String,
    @SerializedName("name")        val name: String,
    @SerializedName("imageUrls")   val imageUrls: List<String>,
    @SerializedName("hasVideo")    val hasVideo: Boolean,
    @SerializedName("lat")         val lat: Double,
    @SerializedName("lng")         val lng: Double,
    @SerializedName("radiusMeters") val radiusMeters: Double
)

data class NodeMetadata(
    val name: String,
    val lat: Double,
    val lng: Double
)

// ── Direction ─────────────────────────────────────────────────────────────────
data class DirectionResponse(
    @SerializedName("nearestNode")     val nearestNode: String,
    @SerializedName("recommendedNode") val recommendedNode: String,
    @SerializedName("distance")        val distanceMeters: Double,
    @SerializedName("directionHint")   val directionHint: String
)

// ── Chat ──────────────────────────────────────────────────────────────────────
data class ChatRequest(
    @SerializedName("tripId")        val tripId: String,
    @SerializedName("currentNodeId") val currentNodeId: String,
    @SerializedName("userMessage")   val userMessage: String,
    @SerializedName("currentLat")    val currentLat: Double,
    @SerializedName("currentLng")    val currentLng: Double
)

data class ChatResponse(
    @SerializedName("reply") val reply: String
)

// ── Cached node for offline Haversine fallback ────────────────────────────────
data class CachedNodeCoord(
    val nodeId: String,
    val name: String,
    val lat: Double,
    val lng: Double
)