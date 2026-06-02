package com.example.humsafar.network

import com.example.humsafar.models.ActiveTripResponse
import com.example.humsafar.models.AmenityResponse
import com.example.humsafar.models.ChatHistoryItem
import com.example.humsafar.models.ChatRequest
import com.example.humsafar.models.ChatResponse
import com.example.humsafar.models.FeedbackCreateRequest
import com.example.humsafar.models.FeedbackResponse
import com.example.humsafar.models.LiveStats
import com.example.humsafar.models.NearbySite
import com.example.humsafar.models.NodeInsights
import com.example.humsafar.models.NodeCommentCreateRequest
import com.example.humsafar.models.NodeCommentResponse
import com.example.humsafar.models.NodeInstantCreateRequest
import com.example.humsafar.models.NodeInstantLikeResponse
import com.example.humsafar.models.NodeInstantResponse
import com.example.humsafar.models.NodePositionResponse
import com.example.humsafar.models.NodeRatingRequest
import com.example.humsafar.models.PhoneUpdateRequest
import com.example.humsafar.models.QrScanResult
import com.example.humsafar.models.RatingResponse
import com.example.humsafar.models.SiteInsights
import com.example.humsafar.models.SiteInsightSnapshot
import com.example.humsafar.models.UserInsights
import com.example.humsafar.models.GemBalance
import com.example.humsafar.models.QuizPrepareRequest
import com.example.humsafar.models.QuizPrepareResponse
import com.example.humsafar.models.QuizStartResponse
import com.example.humsafar.models.QuizAnswerRequest
import com.example.humsafar.models.QuizAnswerResponse
import com.example.humsafar.models.QuizCompleteRequest
import com.example.humsafar.models.QuizCompleteResponse
import com.example.humsafar.models.QuizAbandonRequest
import com.example.humsafar.models.StorePartner
import com.example.humsafar.models.CouponPurchaseRequest
import com.example.humsafar.models.CouponPurchaseResponse
import com.example.humsafar.models.Coupon
import com.example.humsafar.models.BonusOfferRequest
import com.example.humsafar.models.BonusOfferResponse
import com.example.humsafar.models.BonusCompleteRequest
import com.example.humsafar.models.BonusCompleteResponse
import com.example.humsafar.models.BonusSiteStatus
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

    /** Set / update the user's mobile number (used after a Google sign-in). */
    @PATCH("users/{firebase_uid}/phone")
    suspend fun updatePhone(
        @Path("firebase_uid") firebaseUid: String,
        @Body request: PhoneUpdateRequest
    ): Response<UserResponse>

    // ── Live stats (active users + lifetime visitors) ───────────────────────
    @GET("stats/live")
    suspend fun getLiveStats(): Response<LiveStats>

    @POST("stats/heartbeat")
    suspend fun heartbeat(
        @Query("firebase_uid") firebaseUid: String? = null
    ): Response<LiveStats>

    @POST("stats/visit")
    suspend fun recordVisit(
        @Query("firebase_uid") firebaseUid: String? = null
    ): Response<LiveStats>

    // ── Insights (per-site + per-node analytics with on-the-fly ML) ─────────
    @GET("insights/sites/{site_id}")
    suspend fun getSiteInsights(
        @Path("site_id") siteId: Int
    ): Response<SiteInsights>

    @GET("insights/nodes/{node_id}")
    suspend fun getNodeInsights(
        @Path("node_id") nodeId: Int
    ): Response<NodeInsights>

    /** Stored daily insight snapshots for a site (training-ready time series). */
    @GET("insights/sites/{site_id}/history")
    suspend fun getSiteInsightHistory(
        @Path("site_id") siteId: Int,
        @Query("days")   days: Int = 90
    ): Response<List<SiteInsightSnapshot>>

    /** Personal heritage footprint for the signed-in user ("my insights"). */
    @GET("insights/users/{firebase_uid}")
    suspend fun getUserInsights(
        @Path("firebase_uid") firebaseUid: String
    ): Response<UserInsights>

    // ── Gamification: gems wallet ───────────────────────────────────────────
    @GET("gems/{firebase_uid}")
    suspend fun getGems(
        @Path("firebase_uid") firebaseUid: String
    ): Response<GemBalance>

    // ── Gamification: final quiz ────────────────────────────────────────────
    @POST("quiz/prepare")
    suspend fun prepareQuiz(
        @Query("firebase_uid") firebaseUid: String,
        @Query("trip_id")      tripId: Int,
        @Body request: QuizPrepareRequest
    ): Response<QuizPrepareResponse>

    @POST("quiz/start")
    suspend fun startQuiz(
        @Query("firebase_uid") firebaseUid: String,
        @Query("trip_id")      tripId: Int
    ): Response<QuizStartResponse>

    @POST("quiz/answer")
    suspend fun answerQuiz(
        @Body request: QuizAnswerRequest
    ): Response<QuizAnswerResponse>

    @POST("quiz/complete")
    suspend fun completeQuiz(
        @Body request: QuizCompleteRequest
    ): Response<QuizCompleteResponse>

    @POST("quiz/abandon")
    suspend fun abandonQuiz(
        @Body request: QuizAbandonRequest
    ): Response<Unit>

    // ── Gamification: coupon store ──────────────────────────────────────────
    @GET("store/partners")
    suspend fun getStorePartners(
        @Query("site_id")  siteId: Int? = null,
        @Query("kind")     kind: String = "hotel",
        @Query("user_lat") userLat: Double? = null,
        @Query("user_lng") userLng: Double? = null
    ): Response<List<StorePartner>>

    @POST("store/purchase")
    suspend fun purchaseCoupon(
        @Body request: CouponPurchaseRequest
    ): Response<CouponPurchaseResponse>

    @GET("store/coupons/{firebase_uid}")
    suspend fun getMyCoupons(
        @Path("firebase_uid") firebaseUid: String
    ): Response<List<Coupon>>

    // ── Gamification: bonus "Bingo" challenge ───────────────────────────────
    @POST("bonus/offer")
    suspend fun offerBonus(
        @Body request: BonusOfferRequest
    ): Response<BonusOfferResponse>

    @POST("bonus/complete")
    suspend fun completeBonus(
        @Body request: BonusCompleteRequest
    ): Response<BonusCompleteResponse>

    @GET("bonus/status/{firebase_uid}")
    suspend fun getBonusStatus(
        @Path("firebase_uid") firebaseUid: String
    ): Response<List<BonusSiteStatus>>

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

    /** Root comments for a node (most-recent-first). Pass firebase_uid to
     *  populate `is_own` on each comment so the UI can show a delete affordance. */
    @GET("community/comments/node/{node_id}")
    suspend fun getNodeComments(
        @Path("node_id")        nodeId: Int,
        @Query("page")          page: Int = 1,
        @Query("page_size")     pageSize: Int = 20,
        @Query("firebase_uid")  firebaseUid: String? = null
    ): Response<List<NodeCommentResponse>>

    /** Replies of a single root comment, oldest first. */
    @GET("community/comments/{comment_id}/replies")
    suspend fun getCommentReplies(
        @Path("comment_id")     commentId: Int,
        @Query("page")          page: Int = 1,
        @Query("page_size")     pageSize: Int = 50,
        @Query("firebase_uid")  firebaseUid: String? = null
    ): Response<List<NodeCommentResponse>>

    /** Post a comment. Set `parent_comment_id` to make it a reply. */
    @POST("community/comments")
    suspend fun postNodeComment(
        @Body request: NodeCommentCreateRequest
    ): Response<NodeCommentResponse>

    /** Delete one of the caller's own comments. Replies cascade. */
    @DELETE("community/comments/{comment_id}")
    suspend fun deleteNodeComment(
        @Path("comment_id")    commentId: Int,
        @Query("firebase_uid") firebaseUid: String
    ): Response<Unit>

    /** Flag a comment (anyone can flag; backend hides flagged content). */
    @POST("community/comments/{comment_id}/flag")
    suspend fun flagNodeComment(
        @Path("comment_id") commentId: Int
    ): Response<Unit>

    // ── Node Instants (Instagram-style UGC per node) ──────────────────────
    @GET("instants/node/{node_id}")
    suspend fun getNodeInstants(
        @Path("node_id")        nodeId: Int,
        @Query("limit")         limit: Int = 50,
        @Query("firebase_uid")  firebaseUid: String? = null
    ): Response<List<NodeInstantResponse>>

    @POST("instants")
    suspend fun postNodeInstant(
        @Body request: NodeInstantCreateRequest
    ): Response<NodeInstantResponse>

    @POST("instants/{instant_id}/like")
    suspend fun toggleInstantLike(
        @Path("instant_id")     instantId: Int,
        @Query("firebase_uid") firebaseUid: String
    ): Response<NodeInstantLikeResponse>

    @DELETE("instants/{instant_id}")
    suspend fun deleteNodeInstant(
        @Path("instant_id")     instantId: Int,
        @Query("firebase_uid") firebaseUid: String
    ): Response<Unit>

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


