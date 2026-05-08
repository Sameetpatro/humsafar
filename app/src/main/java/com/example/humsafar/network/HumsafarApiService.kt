package com.example.humsafar.network

import com.example.humsafar.models.ActiveTripResponse
import com.example.humsafar.models.AmenityResponse
import com.example.humsafar.models.ChatHistoryItem
import com.example.humsafar.models.ChatRequest
import com.example.humsafar.models.ChatResponse
import com.example.humsafar.models.FeedbackCreateRequest
import com.example.humsafar.models.FeedbackResponse
import com.example.humsafar.models.NearbySite
import com.example.humsafar.models.NodePositionResponse
import com.example.humsafar.models.NodeRatingRequest
import com.example.humsafar.models.QrScanResult
import com.example.humsafar.models.RatingResponse
import com.example.humsafar.models.RecommendationResponse
import com.example.humsafar.models.ReviewSubmitRequest
import com.example.humsafar.models.ReviewSubmitResponse
import com.example.humsafar.models.ReviewSummary
import com.example.humsafar.models.SiteRatingRequest
import com.example.humsafar.models.TripEndResponse
import com.example.humsafar.models.TripStartResponse
import com.example.humsafar.models.UserRegisterRequest
import com.example.humsafar.models.UserResponse
import com.example.humsafar.models.VisitHistoryItem
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface HumsafarApiService {

    // ── Users ─────────────────────────────────────────────────────────────
    // Backend pattern: every Firebase user must be registered via this endpoint
    // before any /trips, /reviews, or chat-history-bearing call will succeed.
    // Idempotent — safe to call on every app launch.
    @POST("users/register")
    suspend fun registerUser(
        @Body request: UserRegisterRequest
    ): Response<UserResponse>

    @GET("users/{firebase_uid}")
    suspend fun getUser(
        @Path("firebase_uid") firebaseUid: String
    ): Response<UserResponse>

    // ── Sites ─────────────────────────────────────────────────────────────

    @GET("sites/nearby")
    suspend fun getNearbySites(
        @Query("lat")          lat: Double,
        @Query("lng")          lng: Double,
        @Query("max_range_km") maxRangeKm: Double = 100.0
    ): Response<List<NearbySite>>

    @GET("sites/{site_id}")
    suspend fun getSiteDetail(
        @Path("site_id") siteId: Int
    ): Response<com.example.humsafar.network.SiteDetail>

    /** Node positions only (exact lat/lng from backend nodes table) for Directions map */
    @GET("sites/{site_id}/nodes")
    suspend fun getSiteNodes(
        @Path("site_id") siteId: Int
    ): Response<List<NodePositionResponse>>

    @GET("sites/scan/{qr_value}")
    suspend fun scanQr(
        @Path("qr_value") qrValue: String
    ): Response<QrScanResult>

    @GET("sites/{site_id}/recommendations")
    suspend fun getRecommendations(
        @Path("site_id") siteId: Int,
        @Query("type") type: String? = null
    ): Response<List<RecommendationResponse>>

    // ── Trips ─────────────────────────────────────────────────────────────

    /**
     * Start a trip by scanning the King QR code.
     * Backend resolves firebase_uid → users.id (UUID); call /users/register first.
     */
    @POST("trips/start")
    suspend fun startTrip(
        @Query("firebase_uid") firebaseUid: String,
        @Query("qr_value")     qrValue: String,
        @Query("entry_lat")    entryLat: Double? = null,
        @Query("entry_lng")    entryLng: Double? = null
    ): Response<TripStartResponse>

    @POST("trips/end")
    suspend fun endTrip(
        @Query("trip_id")       tripId: Int,
        @Query("visited_nodes") visitedNodes: String? = null,
        @Query("entry_lat")     entryLat: Double? = null,
        @Query("entry_lng")     entryLng: Double? = null
    ): Response<TripEndResponse>

    /** Returns the user's currently active trip (used to resume on app reopen). */
    @GET("trips/active/{firebase_uid}")
    suspend fun getActiveTrip(
        @Path("firebase_uid") firebaseUid: String
    ): Response<ActiveTripResponse>

    // ── Reviews ─────────────────────────────────────────────────────────────

    @POST("reviews/submit")
    suspend fun submitReview(
        @Body request: ReviewSubmitRequest
    ): Response<ReviewSubmitResponse>

    @POST("reviews/sites/rate")
    suspend fun rateSite(
        @Body request: SiteRatingRequest
    ): Response<RatingResponse>

    @POST("reviews/nodes/rate")
    suspend fun rateNode(
        @Body request: NodeRatingRequest
    ): Response<RatingResponse>

    @GET("reviews/sites/{site_id}/summary")
    suspend fun getSiteReviewSummary(
        @Path("site_id") siteId: Int
    ): Response<ReviewSummary>

    /** Visit history for the user (drives the History screen). */
    @GET("reviews/users/{firebase_uid}/history")
    suspend fun getUserVisitHistory(
        @Path("firebase_uid") firebaseUid: String
    ): Response<List<VisitHistoryItem>>

    // ── Community ──────────────────────────────────────────────────────────

    /** Submit feedback / bug report. firebase_uid is optional (null = anonymous). */
    @POST("community/feedback")
    suspend fun submitFeedback(
        @Body request: FeedbackCreateRequest
    ): Response<FeedbackResponse>

    // ── Chat ──────────────────────────────────────────────────────────────
    // Use "chat/" WITH trailing slash so OkHttp never needs to redirect.
    // The custom interceptor in HumsafarClient also handles 307 correctly
    // in case FastAPI ever redirects.
    @POST("chat/")
    suspend fun sendChat(
        @Body request: ChatRequest
    ): Response<ChatResponse>

    // ── Amenities ──────────────────────────────────────────────────────────────

    /** Top-N nearest washrooms + shops relative to a scanned node */
    @GET("amenities/near-node")
    suspend fun getAmenitiesNearNode(
        @Query("node_id") nodeId: Int,
        @Query("top_n")   topN:   Int = 2
    ): Response<List<AmenityResponse>>

    /** Full detail for one amenity (AmenityDetailScreen) */
    @GET("amenities/{amenity_id}")
    suspend fun getAmenityDetail(
        @Path("amenity_id") amenityId: Int
    ): Response<AmenityResponse>

    /** All amenities for a site (optional type filter: "washroom" or "shop") */
    @GET("amenities/site/{site_id}")
    suspend fun getSiteAmenities(
        @Path("site_id") siteId: Int,
        @Query("type")   type:   String? = null
    ): Response<List<AmenityResponse>>
}

// ─────────────────────────────────────────────────────────────────────────────
// Singleton client
// ─────────────────────────────────────────────────────────────────────────────

object HumsafarClient {

    private const val BASE_URL = "https://humsafar-backend-5u74.onrender.com/"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60,    TimeUnit.SECONDS)
        .writeTimeout(30,   TimeUnit.SECONDS)
        // ── FIX: resubmit POST body on 307/308 redirects ──────────────────
        // By default OkHttp drops the body when following a redirect.
        // This interceptor manually follows 307/308 with the original request.
        .addInterceptor { chain ->
            val originalRequest: Request = chain.request()
            val response = chain.proceed(originalRequest)

            if (response.code == 307 || response.code == 308) {
                val newUrl = response.header("Location")
                if (newUrl != null) {
                    response.close()
                    val redirectedRequest = originalRequest.newBuilder()
                        .url(newUrl)
                        .build()
                    return@addInterceptor chain.proceed(redirectedRequest)
                }
            }
            response
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .followRedirects(false)          // we handle redirects manually above
        .followSslRedirects(false)
        .build()
    val api: HumsafarApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HumsafarApiService::class.java)
    }
}


