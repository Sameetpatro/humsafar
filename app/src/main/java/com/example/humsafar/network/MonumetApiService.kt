// app/src/main/java/com/example/humsafar/network/MonumentApiService.kt
package com.example.humsafar.network

import com.example.humsafar.models.QrValidationResponse
import com.example.humsafar.models.DirectionResponse
import com.example.humsafar.models.TripStartResponse
import com.example.humsafar.models.NodeMetadataResponse
import com.example.humsafar.models.ChatRequest
import com.example.humsafar.models.ChatResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface MonumentApiService {

    @POST("validate-qr")
    suspend fun validateQr(@Body body: Map<String, String>): Response<QrValidationResponse>

    @POST("trip/start")
    suspend fun startTrip(@Body body: Map<String, String>): Response<TripStartResponse>

    @POST("trip/end")
    suspend fun endTrip(@Body body: Map<String, String>): Response<Unit>

    @POST("get-direction")
    suspend fun getDirection(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<DirectionResponse>

    @GET("node/{nodeId}/metadata")
    suspend fun getNodeMetadata(@Path("nodeId") nodeId: String): Response<NodeMetadataResponse>

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