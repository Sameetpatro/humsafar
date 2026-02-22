// app/src/main/java/com/example/humsafar/network/VideoApiService.kt
// Calls the VIDEO MICROSERVICE directly — no proxy through main backend

package com.example.humsafar.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// ── Request / Response models ──────────────────────────────────────────────

data class GenerateVideoRequest(
    @SerializedName("bot_text")      val botText:      String,
    @SerializedName("site_name")     val siteName:     String,
    @SerializedName("site_id")       val siteId:       String,
    @SerializedName("language_code") val languageCode: String = "en-IN"
)

data class GenerateVideoResponse(
    @SerializedName("job_id") val jobId:  String,
    @SerializedName("status") val status: String
)

data class VideoStatusResponse(
    @SerializedName("job_id")    val jobId:    String,
    @SerializedName("status")    val status:   String,   // "generating" | "ready" | "failed"
    @SerializedName("progress")  val progress: Int    = 0,
    @SerializedName("video_url") val videoUrl: String? = null,
    @SerializedName("message")   val message:  String  = ""
)

// ── Retrofit interface ─────────────────────────────────────────────────────

interface VideoApiService {
    @POST("generate")
    suspend fun generateVideo(@Body request: GenerateVideoRequest): GenerateVideoResponse

    @GET("status/{jobId}")
    suspend fun getStatus(@Path("jobId") jobId: String): VideoStatusResponse
}

// ── Client — named VideoServiceClient to avoid conflict with VoiceRetrofitClient ──

object VideoServiceClient {

    // !! REPLACE with your actual Render video service URL !!
    private const val BASE_URL = "https://humsafar-video-service.onrender.com/"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        )
        .build()

    val api: VideoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VideoApiService::class.java)
    }
}