// app/src/main/java/com/example/humsafar/network/SiteApiService.kt
//
// ⚠️  LEGACY FILE — kept only to avoid breaking any remaining references.
//
// This file previously targeted the old Spring Boot site service. All active
// calls now go through HumsafarApiService (FastAPI backend on Render).
//
// The original imports for Monument, MonumentNode, NearbyPlace, NodeScanResponse,
// Session, SessionResponse have been removed — those models were never ported
// to ApiModels.kt and the endpoints don't exist on the FastAPI backend.
//
// DO NOT add new features here. Use HumsafarApiService + HumsafarClient.

package com.example.humsafar.network

import com.example.humsafar.models.NearbySite
import com.example.humsafar.models.SiteDetail
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * Legacy Retrofit interface — stubbed out to compile cleanly.
 * All methods delegate the same semantics as HumsafarApiService.
 */
interface SiteApiService {

    @GET("sites/nearby")
    suspend fun getNearbySites(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Response<List<NearbySite>>

    @GET("sites/{id}")
    suspend fun getSiteDetail(
        @Path("id") id: Int
    ): Response<SiteDetail>
}

object SiteClient {
    private const val BASE_URL = "https://humsafar-backend-5u74.onrender.com/"

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: SiteApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SiteApiService::class.java)
    }
}