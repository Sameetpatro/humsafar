// app/src/main/java/com/example/humsafar/network/ApiModels.kt
//
// All Retrofit response models. Mirror your backend schemas.py exactly.
// NearbySite now includes latitude + longitude (matches updated NearbySiteResponse).

package com.example.humsafar.network

import com.google.gson.annotations.SerializedName

// ── /sites/nearby ────────────────────────────────────────────────────────────

data class NearbySite(
    @SerializedName("id")              val id: Int              = 0,
    @SerializedName("name")            val name: String         = "",
    @SerializedName("latitude")        val latitude: Double     = 0.0,   // for map markers
    @SerializedName("longitude")       val longitude: Double    = 0.0,   // for map markers
    @SerializedName("distance_meters") val distanceMeters: Double = 0.0,
    @SerializedName("inside_geofence") val insideGeofence: Boolean = false
)

// ── /sites/{site_id} ─────────────────────────────────────────────────────────

data class SiteImage(
    @SerializedName("id")            val id: Int           = 0,
    @SerializedName("image_url")     val imageUrl: String  = "",
    @SerializedName("display_order") val displayOrder: Int = 0
)

data class Node(
    @SerializedName("id")             val id: Int             = 0,
    @SerializedName("name")           val name: String        = "",
    @SerializedName("latitude")       val latitude: Double    = 0.0,
    @SerializedName("longitude")      val longitude: Double   = 0.0,
    @SerializedName("sequence_order") val sequenceOrder: Int  = 0,
    @SerializedName("is_king")        val isKing: Boolean     = false,
    @SerializedName("description")    val description: String? = null,
    @SerializedName("video_url")      val videoUrl: String?   = null
)

data class SiteDetail(
    @SerializedName("id")                      val id: Int                   = 0,
    @SerializedName("name")                    val name: String              = "",
    @SerializedName("latitude")                val latitude: Double          = 0.0,
    @SerializedName("longitude")               val longitude: Double         = 0.0,
    @SerializedName("geofence_radius_meters")  val geofenceRadiusMeters: Int = 0,
    @SerializedName("summary")                 val summary: String?          = null,
    @SerializedName("history")                 val history: String?          = null,
    @SerializedName("fun_facts")               val funFacts: String?         = null,
    @SerializedName("helpline_number")         val helplineNumber: String?   = null,
    @SerializedName("static_map_url")          val staticMapUrl: String?     = null,
    @SerializedName("intro_video_url")         val introVideoUrl: String?    = null,
    @SerializedName("rating")                  val rating: Double            = 0.0,
    @SerializedName("upvotes")                 val upvotes: Int              = 0,
    @SerializedName("images")                  val images: List<SiteImage>   = emptyList(),
    @SerializedName("nodes")                   val nodes: List<Node>         = emptyList()
)

// ── /sites/scan/{qr_value} ───────────────────────────────────────────────────

data class QrScanResult(
    @SerializedName("status")         val status: String        = "",  // "valid" or "invalid"
    @SerializedName("site_id")        val siteId: Int           = 0,   // heritage_sites.id PK
    @SerializedName("node_id")        val nodeId: Int           = 0,   // nodes.id PK
    @SerializedName("sequence_order") val sequenceOrder: Int    = 0,
    @SerializedName("node_name")      val nodeName: String      = ""
)

// ── /chat ─────────────────────────────────────────────────────────────────────

data class ChatMessage(
    @SerializedName("role")    val role: String    = "",
    @SerializedName("content") val content: String = ""
)

data class ChatRequest(
    @SerializedName("site_id") val siteId: Int,           // always from ActiveSiteManager
    @SerializedName("node_id") val nodeId: Int?,          // from ActiveSiteManager, null if no QR
    @SerializedName("message") val message: String,
    @SerializedName("history") val history: List<ChatMessage> = emptyList()
)

data class ChatResponse(
    @SerializedName("reply") val reply: String = ""
)

// ── /trips/start + /trips/end ─────────────────────────────────────────────────

data class TripStartResponse(
    @SerializedName("message") val message: String = "",
    @SerializedName("trip_id") val tripId: Int     = 0
)

data class TripEndResponse(
    @SerializedName("message") val message: String = ""
)