// app/src/main/java/com/example/humsafar/network/VoiceRetrofitClient.kt
// NEW FILE

package com.example.humsafar.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Separate Retrofit instance for the voice pipeline.
 *
 * Why not reuse RetrofitClient:
 *   Voice pipeline = audio upload + STT + LLM + TTS.
 *   Total latency can reach 30–60 s on free-tier backends.
 *   RetrofitClient uses OkHttp defaults (10 s read), which would time out.
 *   Keeping them separate avoids making all /chat calls slow as well.
 *
 * Logging level = HEADERS only:
 *   BODY would dump the raw base64 TTS audio into logcat — useless noise.
 */
object VoiceRetrofitClient {

    private const val BASE_URL = "https://humsafar-backend-59ic.onrender.com/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15,  TimeUnit.SECONDS)
        .writeTimeout(60,   TimeUnit.SECONDS)   // WAV upload
        .readTimeout(120,   TimeUnit.SECONDS)   // STT + LLM + TTS round-trip
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS }
        )
        .build()

    val api: VoiceChatApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VoiceChatApiService::class.java)
    }
}