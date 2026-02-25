// app/src/main/java/com/example/humsafar/network/MonumentApiService.kt
//
// ⚠️  LEGACY FILE — kept only to avoid breaking any remaining references.
//
// This file previously targeted an old Spring Boot backend that no longer
// exists. All active API calls now go through HumsafarApiService (FastAPI).
//
// The original imports for QrValidationResponse, DirectionResponse, and
// NodeMetadataResponse have been removed because those models were never
// added to ApiModels.kt and the endpoints they called don't exist on the
// current FastAPI backend.
//
// DO NOT add new features here. Use HumsafarApiService + HumsafarClient.

package com.example.humsafar.network

import com.example.humsafar.models.ChatRequest
import com.example.humsafar.models.ChatResponse
import com.example.humsafar.models.TripStartResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * Legacy Retrofit interface — all methods are stubs that mirror the old API
 * surface. Nothing in the app calls these anymore; they compile cleanly so
 * the rest of the project can build without errors.
 */
interface MonumentApiService {

    @POST("trip/start")
    suspend fun startTrip(@Body body: Map<String, String>): Response<TripStartResponse>

    @POST("trip/end")
    suspend fun endTrip(@Body body: Map<String, String>): Response<Unit>

    @POST("chat")
    suspend fun sendChat(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ChatResponse>
}

object MonumentClient {
    private const val BASE_URL = "https://your-monument-backend.onrender.com/"
    private const val API_KEY  = "HARSH_MONUMENT_SECRET_2026"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("x-api-key", API_KEY)
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(req)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: MonumentApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MonumentApiService::class.java)
    }
}