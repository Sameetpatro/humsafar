// app/src/main/java/com/example/humsafar/network/ApiService.kt
//
// Retrofit interface. Every endpoint that needs a site uses the DB primary key.
// site_id comes from ActiveSiteManager — never hardcoded.

package com.example.humsafar.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Site discovery ────────────────────────────────────────────────────────

    /**
     * Backend runs haversine SQL on heritage_sites table.
     * Returns sites within max_range_km, sorted by distance.
     * inside_geofence=true when distance <= geofence_radius_meters.
     */
    @GET("sites/nearby")
    suspend fun getNearbySites(
        @Query("lat")          lat: Double,
        @Query("lng")          lng: Double,
        @Query("max_range_km") maxRangeKm: Double = 10.0
    ): Response<List<NearbySite>>

    /**
     * Full site detail — nodes, images, history, etc.
     * Call with site_id from ActiveSiteManager (= heritage_sites.id PK).
     */
    @GET("sites/{site_id}")
    suspend fun getSiteDetail(
        @Path("site_id") siteId: Int
    ): Response<SiteDetail>

    /**
     * QR code scan.
     * Returns site_id + node_id — both are DB primary keys.
     * Store them in ActiveSiteManager after scanning.
     */
    @GET("sites/scan/{qr_value}")
    suspend fun scanQr(
        @Path("qr_value") qrValue: String
    ): Response<QrScanResult>

    // ── Chat ──────────────────────────────────────────────────────────────────

    /**
     * site_id drives which prompt row is loaded from the prompts table.
     * node_id (optional) further narrows to a node-specific prompt.
     * Both come from ActiveSiteManager.
     */
    @POST("chat/")
    suspend fun chat(
        @Body request: ChatRequest
    ): Response<ChatResponse>

    // ── Voice ─────────────────────────────────────────────────────────────────

    /**
     * Multipart voice. site_id and node_id same rules as chat.
     * Backend uses these to fetch the same 3-tier prompt context.
     */
    @Multipart
    @POST("voice-chat")
    suspend fun voiceChat(
        @Part                       audio:     MultipartBody.Part,
        @Part("site_name")          siteName:  RequestBody,
        @Part("site_id")            siteId:    RequestBody,  // ActiveSiteManager.activeSiteId
        @Part("language")           language:  RequestBody,
        @Part("lang_name")          langName:  RequestBody,
        @Part("node_id")            nodeId:    RequestBody   // "" if no QR scanned
    ): Response<VoiceChatResponse>

    // ── Trips ─────────────────────────────────────────────────────────────────

    /**
     * Trip start. qr_value must be a king node QR.
     * Backend resolves site_id from the node — we don't pass it separately.
     */
    @POST("trips/start")
    suspend fun startTrip(
        @Query("user_id")  userId:   String,
        @Query("qr_value") qrValue:  String
    ): Response<TripStartResponse>

    @POST("trips/end")
    suspend fun endTrip(
        @Query("trip_id") tripId: Int
    ): Response<TripEndResponse>
}

// VoiceChatResponse — mirrors backend VoiceChatResponse schema
data class VoiceChatResponse(
    @com.google.gson.annotations.SerializedName("user_text")    val userText: String    = "",
    @com.google.gson.annotations.SerializedName("bot_text")     val botText: String     = "",
    @com.google.gson.annotations.SerializedName("audio_base64") val audioBase64: String = "",
    @com.google.gson.annotations.SerializedName("audio_format") val audioFormat: String = "wav"
)