package com.example.humsafar.models

import com.google.gson.annotations.SerializedName

// ── Monument ──────────────────────────────────────────────────────────────────
data class Monument(
    @SerializedName("id")             val id: Long = 0,
    @SerializedName("name")           val name: String = "",
    @SerializedName("city")           val city: String = "",
    @SerializedName("state")          val state: String = "",
    @SerializedName("description")    val description: String = "",
    @SerializedName("history")        val history: String = "",
    @SerializedName("prompt")         val prompt: String = "",
    @SerializedName("latitude")       val latitude: Double = 0.0,
    @SerializedName("longitude")      val longitude: Double = 0.0,
    @SerializedName("photoUrls")      val photoUrls: List<String> = emptyList(),
    @SerializedName("videoUrl")       val videoUrl: String = "",
    @SerializedName("navigationInfo") val navigationInfo: String = "",
    @SerializedName("helplineNumber") val helplineNumber: String = ""
)

// ── Monument Node ─────────────────────────────────────────────────────────────
data class MonumentNode(
    @SerializedName("id")             val id: Long = 0,
    @SerializedName("name")           val name: String = "",
    @SerializedName("nodeType")       val nodeType: String = "NORMAL",
    @SerializedName("monumentId")     val monumentId: Long = 0,
    @SerializedName("latitude")       val latitude: Double = 0.0,
    @SerializedName("longitude")      val longitude: Double = 0.0,
    @SerializedName("photoUrls")      val photoUrls: List<String> = emptyList(),
    @SerializedName("videoUrl")       val videoUrl: String = "",
    @SerializedName("history")        val history: String = "",
    @SerializedName("prompt")         val prompt: String = "",
    @SerializedName("navigationInfo") val navigationInfo: String = "",
    @SerializedName("visitOrder")     val visitOrder: Int = 0,
    @SerializedName("recommended")    val recommended: Boolean = false
)

// ── Scan Response ─────────────────────────────────────────────────────────────
data class NodeScanResponse(
    @SerializedName("node")           val node: MonumentNode,
    @SerializedName("parentMonument") val parentMonument: Monument? = null,
    val recommendedNext: MonumentNode? = null,
    val allNodes: List<MonumentNode>   = emptyList(),
    val sessionStarted: Boolean        = false
)

// ── Nearby Places ─────────────────────────────────────────────────────────────
data class NearbyPlace(
    @SerializedName("id")         val id: Long = 0,
    @SerializedName("monumentId") val monumentId: Long = 0,
    @SerializedName("name")       val name: String = "",
    @SerializedName("type")       val type: String = "",
    @SerializedName("distanceKm") val distanceKm: Double = 0.0,
    @SerializedName("latitude")   val latitude: Double = 0.0,
    @SerializedName("longitude")  val longitude: Double = 0.0,
    @SerializedName("reason")     val reason: String = ""
) {
    // Computed property must live in the class body, not the constructor
    val description: String get() = reason
}

// ── Session ───────────────────────────────────────────────────────────────────
data class Session(
    @SerializedName("id")          val id: Long = 0,
    @SerializedName("firebaseUid") val firebaseUid: String = "",
    @SerializedName("monumentId")  val monumentId: Long = 0,
    @SerializedName("startTime")   val startTime: String = "",
    @SerializedName("isActive")    val active: Boolean = true
)

// ── Session Response ──────────────────────────────────────────────────────────
data class SessionResponse(
    @SerializedName("session")      val session: Session? = null,
    @SerializedName("restaurants")  val restaurants: List<NearbyPlace> = emptyList(),
    @SerializedName("hotels")       val hotels: List<NearbyPlace> = emptyList(),
    @SerializedName("touristSites") val touristSites: List<NearbyPlace> = emptyList()
) {
    // Computed properties must live in the class body, not the constructor
    val message: String get() = "Trip ended"
    val duration: String get() = ""
    val nodesVisited: Int get() = 0
}

// ── Local trip state (not from backend) ───────────────────────────────────────
data class TripSnapshot(
    val sessionId:       String     = "",
    val monumentId:      Long       = 0L,
    val monumentName:    String     = "",
    val currentNodeId:   Long       = 0L,
    val currentNodeName: String     = "",
    val isTripActive:    Boolean    = false,
    val lastLat:         Double     = 0.0,
    val lastLng:         Double     = 0.0,
    val visitedNodeIds:  List<Long> = emptyList()
)