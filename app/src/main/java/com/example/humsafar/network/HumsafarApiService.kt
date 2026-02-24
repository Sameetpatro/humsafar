// app/src/main/java/com/example/humsafar/network/HumsafarApiService.kt
// SINGLE Retrofit interface replacing RetrofitClient + SiteClient + MonumentClient.
// All calls go to https://humsafar-backend-5u74.onrender.com

package com.example.humsafar.network

import com.example.humsafar.models.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface HumsafarApiService {


    // ── Sites ─────────────────────────────────────────────────────────────

    /** Returns sites within max_range_km of the given coordinate. */
    @GET("sites/nearby")
    suspend fun getNearbySites(
        @Query("lat")          lat: Double,
        @Query("lng")          lng: Double,
        @Query("max_range_km") maxRangeKm: Double = 100.0
    ): Response<List<NearbySite>>

    /** Full site detail including images + node list. */
    @GET("sites/{site_id}")
    suspend fun getSiteDetail(
        @Path("site_id") siteId: Int
    ): Response<SiteDetail>

    /** Scan a QR code value — returns node + site info. */
    @GET("sites/scan/{qr_value}")
    suspend fun scanQr(
        @Path("qr_value") qrValue: String
    ): Response<QrScanResult>

    // ── Trips ─────────────────────────────────────────────────────────────

    /** Start a trip by scanning a King node QR. */
    @POST("trips/start")
    suspend fun startTrip(
        @Query("user_id")   userId: String,
        @Query("qr_value")  qrValue: String
    ): Response<TripStartResponse>

    /** End an active trip. */
    @POST("trips/end")
    suspend fun endTrip(
        @Query("trip_id") tripId: Int
    ): Response<TripEndResponse>

    // ── Chat ──────────────────────────────────────────────────────────────

    /** Text chat with the heritage AI. */
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
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
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