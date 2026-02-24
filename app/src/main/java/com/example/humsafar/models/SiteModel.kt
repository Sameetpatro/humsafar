package com.example.humsafar.models

import com.google.gson.annotations.SerializedName

// ── Monument ──────────────────────────────────────────────────────────────────
data class Monument(
    @SerializedName("id")          val id: Long = 0,
    @SerializedName("name")        val name: String = "",
    @SerializedName("city")        val city: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("latitude")    val latitude: Double = 0.0,
    @SerializedName("longitude")   val longitude: Double = 0.0,
    @SerializedName("photoUrls")   val photoUrls: List<String> = emptyList(),
    @SerializedName("videoUrl")    val videoUrl: String = ""
)

// ── Node ──────────────────────────────────────────────────────────────────────
data class MonumentNode(
    @SerializedName("id")             val id: Long = 0,
    @SerializedName("name")           val name: String = "",
    @SerializedName("nodeType")       val nodeType: String = "NORMAL", // "KING" | "NORMAL"
    @SerializedName("monumentId")     val monumentId: Long = 0,
    @SerializedName("latitude")       val latitude: Double = 0.0,
    @SerializedName("longitude")      val longitude: Double = 0.0,
    @SerializedName("photoUrls")      val photoUrls: List<String> = emptyList(),
    @SerializedName("videoUrl")       val videoUrl: String = "",
    @SerializedName("history")        val history: String = "",
    @SerializedName("prompt")         val prompt: String = "",
    @SerializedName("navigationInfo") val navigationInfo: String = "",
    @SerializedName("visitOrder")     val visitOrder: Int = 0,        // static sequence
    @SerializedName("recommended")    val recommended: Boolean = false
)

// ── Scan Response ─────────────────────────────────────────────────────────────
data class NodeScanResponse(
    @SerializedName("node")              val node: MonumentNode,
    @SerializedName("recommendedNext")   val recommendedNext: MonumentNode?,
    @SerializedName("allNodes")          val allNodes: List<MonumentNode> = emptyList(),
    @SerializedName("sessionStarted")    val sessionStarted: Boolean = false
)

// ── Nearby Places ─────────────────────────────────────────────────────────────
data class NearbyPlace(
    @SerializedName("id")          val id: Long = 0,
    @SerializedName("monumentId")  val monumentId: Long = 0,
    @SerializedName("name")        val name: String = "",
    @SerializedName("type")        val type: String = "",   // "WASHROOM"|"CANTEEN"|"SNACKS"|"EXIT"
    @SerializedName("latitude")    val latitude: Double = 0.0,
    @SerializedName("longitude")   val longitude: Double = 0.0,
    @SerializedName("description") val description: String = ""
)

// ── Session ───────────────────────────────────────────────────────────────────
data class Session(
    @SerializedName("id")          val id: Long = 0,
    @SerializedName("firebaseUid") val firebaseUid: String = "",
    @SerializedName("monumentId")  val monumentId: Long = 0,
    @SerializedName("startTime")   val startTime: String = "",
    @SerializedName("active")      val active: Boolean = true
)

data class SessionResponse(
    @SerializedName("message")   val message: String = "",
    @SerializedName("duration")  val duration: String = "",
    @SerializedName("nodesVisited") val nodesVisited: Int = 0
)

// ── Local trip state (not from backend) ───────────────────────────────────────
data class TripSnapshot(
    val sessionId:       String       = "",
    val monumentId:      Long         = 0L,
    val monumentName:    String       = "",
    val currentNodeId:   Long         = 0L,
    val currentNodeName: String       = "",
    val isTripActive:    Boolean      = false,
    val lastLat:         Double       = 0.0,
    val lastLng:         Double       = 0.0,
    val visitedNodeIds:  List<Long>   = emptyList()
)