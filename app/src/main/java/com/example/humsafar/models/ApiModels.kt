// app/src/main/java/com/example/humsafar/models/ApiModels.kt
// FIXED — added description + videoUrl to SiteNode so NodeDetailScreen compiles.
// Backend NodeResponse schema and sites.py router must also be updated (see schemas.py fix).

package com.example.humsafar.models

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────────────────────
// /sites/nearby  →  GET /sites/nearby?lat=&lng=
// ─────────────────────────────────────────────────────────────────────────────
data class NearbySite(
    @SerializedName("id")              val id: Int = 0,
    @SerializedName("name")            val name: String = "",
    @SerializedName("latitude")        val latitude: Double = 0.0,
    @SerializedName("longitude")       val longitude: Double = 0.0,
    @SerializedName("distance_meters") val distanceMeters: Double = 0.0,
    @SerializedName("inside_geofence") val insideGeofence: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// /sites/{site_id}  →  GET /sites/{site_id}
// ─────────────────────────────────────────────────────────────────────────────
data class SiteDetail(
    @SerializedName("id")               val id: Int = 0,
    @SerializedName("name")             val name: String = "",
    @SerializedName("latitude")         val latitude: Double = 0.0,
    @SerializedName("longitude")        val longitude: Double = 0.0,
    @SerializedName("geofence_radius_meters") val geofenceRadiusMeters: Int = 300,
    @SerializedName("summary")          val summary: String? = null,
    @SerializedName("history")          val history: String? = null,
    @SerializedName("fun_facts")        val funFacts: String? = null,
    @SerializedName("helpline_number")  val helplineNumber: String? = null,
    @SerializedName("static_map_url")   val staticMapUrl: String? = null,
    @SerializedName("intro_video_url")  val introVideoUrl: String? = null,
    @SerializedName("rating")           val rating: Double = 4.5,
    @SerializedName("upvotes")          val upvotes: Int = 0,
    @SerializedName("images")           val images: List<SiteImage> = emptyList(),
    @SerializedName("nodes")            val nodes: List<SiteNode> = emptyList()
)

data class SiteImage(
    @SerializedName("id")            val id: Int = 0,
    @SerializedName("image_url")     val imageUrl: String = "",
    @SerializedName("display_order") val displayOrder: Int = 0
)

// Matches backend NodeResponse — used by NodeDetailScreen (images, isKing, etc.)
data class SiteNode(
    @SerializedName("id")              val id: Int = 0,
    @SerializedName("name")            val name: String = "",
    @SerializedName("latitude")        val latitude: Double = 0.0,
    @SerializedName("longitude")       val longitude: Double = 0.0,
    @SerializedName("sequence_order")  val sequenceOrder: Int = 0,
    @SerializedName("is_king")         val isKing: Boolean = false,
    @SerializedName("description")    val description: String? = null,
    @SerializedName("video_url")       val videoUrl: String? = null,
    @SerializedName("image_url")       val imageUrl: String? = null,
    @SerializedName("images")          val images: List<NodeImage> = emptyList()
)

data class NodeImage(
    @SerializedName("id")            val id: Int = 0,
    @SerializedName("image_url")     val imageUrl: String = "",
    @SerializedName("display_order") val displayOrder: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
// /sites/scan/{qr_value}  →  GET /sites/scan/{qr_value}
// ─────────────────────────────────────────────────────────────────────────────
data class QrScanResult(
    @SerializedName("status")         val status: String = "",
    @SerializedName("site_id")        val siteId: Int? = null,
    @SerializedName("node_id")        val nodeId: Int? = null,
    @SerializedName("sequence_order") val sequenceOrder: Int? = null,
    @SerializedName("node_name")      val nodeName: String? = null
) {
    val isValid: Boolean get() = status == "valid"
    val isKingNode: Boolean get() = sequenceOrder == 0
}

// ─────────────────────────────────────────────────────────────────────────────
// /trips/start  →  POST /trips/start?user_id=&qr_value=
// ─────────────────────────────────────────────────────────────────────────────
data class TripStartResponse(
    @SerializedName("message") val message: String = "",
    @SerializedName("trip_id") val tripId: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
// /trips/end  →  POST /trips/end?trip_id=
// ─────────────────────────────────────────────────────────────────────────────
data class TripEndResponse(
    @SerializedName("message") val message: String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
// /chat/  →  POST /chat/
// firebase_uid/trip_id/lang_code are optional; when provided the backend
// persists the user message + assistant reply to user_chat_history. Calls
// without firebase_uid still work — chat must never fail on analytics writes.
// ─────────────────────────────────────────────────────────────────────────────
data class ChatRequest(
    @SerializedName("site_id")      val siteId: Int,
    @SerializedName("node_id")      val nodeId: Int? = null,
    @SerializedName("message")      val message: String,
    @SerializedName("history")      val history: List<ChatHistoryItem> = emptyList(),
    @SerializedName("firebase_uid") val firebaseUid: String? = null,
    @SerializedName("trip_id")      val tripId: Int? = null,
    @SerializedName("lang_code")    val langCode: String? = null
)

data class ChatHistoryItem(
    @SerializedName("role")    val role: String,
    @SerializedName("content") val content: String
)

data class ChatResponse(
    @SerializedName("reply") val reply: String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
// Local trip state (never serialized, lives in TripManager only)
// ─────────────────────────────────────────────────────────────────────────────
data class TripSnapshot(
    val tripId:          Int     = 0,
    val siteId:          Int     = 0,
    val siteName:        String  = "",
    val currentNodeId:   Int     = 0,
    val currentNodeName: String  = "",
    val isTripActive:    Boolean = false,
    val lastLat:         Double  = 0.0,
    val lastLng:         Double  = 0.0,
    val visitedNodeIds:  List<Int> = emptyList()
)

// ─────────────────────────────────────────────────────────────────────────────
// Node detail (assembled client-side from SiteDetail + QrScanResult)
// ─────────────────────────────────────────────────────────────────────────────
data class NodeDetail(
    val id:            Int,
    val name:          String,
    val siteId:        Int,
    val siteName:      String,
    val sequenceOrder: Int,
    val description:   String?,
    val videoUrl:      String?,
    val images:        List<String>,
    val isKing:        Boolean
)

// ─────────────────────────────────────────────────────────────────────────────
// /sites/{site_id}/nodes  →  GET /sites/{site_id}/nodes (exact lat/lng for map)
// ─────────────────────────────────────────────────────────────────────────────
data class NodePositionResponse(
    @SerializedName("id")              val id: Int = 0,
    @SerializedName("name")            val name: String = "",
    @SerializedName("latitude")       val latitude: Double = 0.0,
    @SerializedName("longitude")      val longitude: Double = 0.0,
    @SerializedName("sequence_order") val sequenceOrder: Int = 0,
    @SerializedName("is_king")        val isKing: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// /reviews/submit  →  POST /reviews/submit
// Backend ReviewSubmitBody expects "firebase_uid" — NOT "user_id".
// ─────────────────────────────────────────────────────────────────────────────
data class ReviewSubmitRequest(
    @SerializedName("trip_id")         val tripId: Int,
    @SerializedName("site_id")         val siteId: Int,
    @SerializedName("firebase_uid")    val firebaseUid: String,
    @SerializedName("star_rating")     val starRating: Int,
    @SerializedName("q1")              val q1: Int,
    @SerializedName("q2")              val q2: Int,
    @SerializedName("q3")              val q3: Int,
    @SerializedName("suggestion_text") val suggestionText: String? = null
)

data class ReviewSubmitResponse(
    @SerializedName("message")    val message: String = "",
    @SerializedName("review_id")  val reviewId: Int = 0,
    @SerializedName("new_rating") val newRating: Double = 0.0
)

// ─────────────────────────────────────────────────────────────────────────────
// /reviews/sites/rate  →  POST /reviews/sites/rate
// /reviews/nodes/rate  →  POST /reviews/nodes/rate
// ─────────────────────────────────────────────────────────────────────────────
data class SiteRatingRequest(
    @SerializedName("site_id")      val siteId: Int,
    @SerializedName("firebase_uid") val firebaseUid: String,
    @SerializedName("rating")       val rating: Int    // 1–5
)

data class NodeRatingRequest(
    @SerializedName("node_id")      val nodeId: Int,
    @SerializedName("site_id")      val siteId: Int,
    @SerializedName("firebase_uid") val firebaseUid: String,
    @SerializedName("rating")       val rating: Int    // 1–5
)

data class RatingResponse(
    @SerializedName("message") val message: String = "",
    @SerializedName("new_avg") val newAvg: Double  = 0.0,
    @SerializedName("total")   val total: Int      = 0
)

data class ReviewSummary(
    @SerializedName("avg_star_rating")        val avgStarRating: Double      = 0.0,
    @SerializedName("total_ratings")          val totalRatings: Int          = 0,
    @SerializedName("avg_overall_experience") val avgOverallExperience: Double = 0.0,
    @SerializedName("avg_guide_helpfulness")  val avgGuideHelpfulness: Double  = 0.0,
    @SerializedName("avg_recommend_score")    val avgRecommendScore: Double    = 0.0,
    @SerializedName("total_reviews")          val totalReviews: Int            = 0,
    @SerializedName("recommend_pct")          val recommendPct: Double         = 0.0,
    @SerializedName("satisfaction_label")     val satisfactionLabel: String    = "No data"
)

// ─────────────────────────────────────────────────────────────────────────────
// /users/register  →  POST /users/register
// ─────────────────────────────────────────────────────────────────────────────
data class UserRegisterRequest(
    @SerializedName("firebase_uid")   val firebaseUid: String,
    @SerializedName("display_name")   val displayName: String? = null,
    @SerializedName("email")          val email:        String? = null,
    @SerializedName("phone")          val phone:        String? = null,
    @SerializedName("avatar_url")     val avatarUrl:    String? = null,
    @SerializedName("preferred_lang") val preferredLang: String = "en-IN",
    @SerializedName("is_anonymous")   val isAnonymous:  Boolean = false
)

data class UserResponse(
    @SerializedName("id")             val id: String = "",
    @SerializedName("firebase_uid")   val firebaseUid: String = "",
    @SerializedName("display_name")   val displayName: String? = null,
    @SerializedName("email")          val email: String? = null,
    @SerializedName("phone")          val phone: String? = null,
    @SerializedName("avatar_url")     val avatarUrl: String? = null,
    @SerializedName("preferred_lang") val preferredLang: String = "en-IN",
    @SerializedName("is_anonymous")   val isAnonymous: Boolean = false,
    @SerializedName("created_at")     val createdAt: String = "",
    @SerializedName("last_active_at") val lastActiveAt: String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
// /reviews/users/{firebase_uid}/history  →  GET visit history
// ─────────────────────────────────────────────────────────────────────────────
data class VisitHistoryItem(
    @SerializedName("id")               val id: Int = 0,
    @SerializedName("site_id")          val siteId: Int = 0,
    @SerializedName("site_name")        val siteName: String = "",
    @SerializedName("trip_id")          val tripId: Int? = null,
    @SerializedName("nodes_visited")    val nodesVisited: List<Int> = emptyList(),
    @SerializedName("total_nodes")      val totalNodes: Int = 0,
    @SerializedName("nodes_completed")  val nodesCompleted: Int = 0,
    @SerializedName("completed")        val completed: Boolean = false,
    @SerializedName("visited_at")       val visitedAt: String? = null,    // ISO-8601
    @SerializedName("ended_at")         val endedAt: String? = null,      // ISO-8601
    @SerializedName("duration_mins")    val durationMins: Int? = null,
    @SerializedName("review_submitted") val reviewSubmitted: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// /trips/active/{firebase_uid}  →  GET active trip (for resume)
// ─────────────────────────────────────────────────────────────────────────────
data class ActiveTripResponse(
    @SerializedName("active")     val active: Boolean = false,
    @SerializedName("trip_id")    val tripId: Int? = null,
    @SerializedName("site_id")    val siteId: Int? = null,
    @SerializedName("started_at") val startedAt: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// /community/feedback  →  POST /community/feedback
// ─────────────────────────────────────────────────────────────────────────────
data class FeedbackCreateRequest(
    @SerializedName("firebase_uid") val firebaseUid: String? = null,   // null = anonymous
    @SerializedName("site_id")      val siteId: Int,
    @SerializedName("category")     val category: String = "general",   // general | accessibility | content | bug
    @SerializedName("content")      val content: String
)

data class FeedbackResponse(
    @SerializedName("id")         val id: Int = 0,
    @SerializedName("site_id")    val siteId: Int = 0,
    @SerializedName("category")   val category: String = "general",
    @SerializedName("content")    val content: String = "",
    @SerializedName("status")     val status: String = "open",
    @SerializedName("created_at") val createdAt: String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
// /sites/{site_id}/recommendations  →  GET /sites/{site_id}/recommendations
// ─────────────────────────────────────────────────────────────────────────────
data class RecommendationResponse(
    @SerializedName("id")          val id: Int,
    @SerializedName("site_id")     val siteId: Int,
    @SerializedName("type")        val type: String,      // monument, hotel, restaurant
    @SerializedName("name")        val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("latitude")    val latitude: Double? = null,
    @SerializedName("longitude")   val longitude: Double? = null
)
// ─────────────────────────────────────────────────────────────────────────────
// /amenities/near-node  →  GET /amenities/near-node?node_id=&top_n=
// ─────────────────────────────────────────────────────────────────────────────
data class AmenityResponse(
    @SerializedName("id")               val id:             Int,
    @SerializedName("site_id")          val siteId:         Int,
    @SerializedName("node_id")          val nodeId:         Int?    = null,
    @SerializedName("type")             val type:           String,   // "washroom" | "shop"
    @SerializedName("name")             val name:           String,
    @SerializedName("description")      val description:    String? = null,
    @SerializedName("latitude")         val latitude:       Double,
    @SerializedName("longitude")        val longitude:      Double,
    @SerializedName("price_info")       val priceInfo:      String? = null,
    @SerializedName("timing")           val timing:         String? = null,
    @SerializedName("is_paid")          val isPaid:         Boolean = false,
    @SerializedName("distance_meters")  val distanceMeters: Double? = null
)