package com.example.humsafar.network

import com.example.humsafar.models.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface HumsafarApiService {

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
    ): Response<SiteDetail>

    @GET("sites/scan/{qr_value}")
    suspend fun scanQr(
        @Path("qr_value") qrValue: String
    ): Response<QrScanResult>

    // ── Trips ─────────────────────────────────────────────────────────────

    @POST("trips/start")
    suspend fun startTrip(
        @Query("user_id")  userId: String,
        @Query("qr_value") qrValue: String
    ): Response<TripStartResponse>

    @POST("trips/end")
    suspend fun endTrip(
        @Query("trip_id") tripId: Int
    ): Response<TripEndResponse>

    // ── Chat ──────────────────────────────────────────────────────────────
    // Use "chat/" WITH trailing slash so OkHttp never needs to redirect.
    // The custom interceptor in HumsafarClient also handles 307 correctly
    // in case FastAPI ever redirects.
    @POST("chat/")
    suspend fun sendChat(
        @Body request: ChatRequest
    ): Response<ChatResponse>
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