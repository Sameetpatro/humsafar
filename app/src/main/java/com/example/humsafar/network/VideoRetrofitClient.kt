// app/src/main/java/com/example/humsafar/network/VideoRetrofitClient.kt

package com.example.humsafar.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object VideoRetrofitClient {

    private const val BASE_URL = "https://humsafar-backend-59ic.onrender.com/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        )
        .build()

    val api: VideoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VideoApiService::class.java)
    }

    // Static URL builder helpers
    fun overviewVideoUrl(monumentId: String) =
        "${BASE_URL}static/videos/overview/$monumentId.mp4"

    fun promptVideoUrl(hash: String) =
        "${BASE_URL}static/videos/prompt/$hash.mp4"
}