package com.example.humsafar.network

import com.example.humsafar.models.Monument
import com.example.humsafar.models.MonumentNode
import com.example.humsafar.models.NearbyPlace
import com.example.humsafar.models.NodeScanResponse
import com.example.humsafar.models.Session
import com.example.humsafar.models.SessionResponse
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

interface SiteApiService {

    @GET("api/monuments")
    suspend fun getAllMonuments(): Response<List<Monument>>

    @GET("api/monuments/{id}")
    suspend fun getMonument(@Path("id") id: Long): Response<Monument>

    @GET("api/monuments/city/{city}")
    suspend fun getByCity(@Path("city") city: String): Response<List<Monument>>

    @GET("api/nodes/monument/{monumentId}")
    suspend fun getNodesForMonument(
        @Path("monumentId") monumentId: Long
    ): Response<List<MonumentNode>>

    @GET("api/nodes/{nodeId}/scan")
    suspend fun scanNode(
        @Path("nodeId") nodeId: Long
    ): Response<NodeScanResponse>

    @GET("api/nearby/{monumentId}")
    suspend fun getNearbyPlaces(
        @Path("monumentId") monumentId: Long
    ): Response<List<NearbyPlace>>

    @POST("api/sessions/start")
    suspend fun startSession(
        @Query("firebaseUid") firebaseUid: String,
        @Query("monumentId") monumentId: Long
    ): Response<Session>

    @POST("api/sessions/end")
    suspend fun endSession(
        @Query("firebaseUid") firebaseUid: String
    ): Response<SessionResponse>
}

// ── Gson deserializer: handles photoUrls as JSON array OR comma-separated string ──

/**
 * Spring Boot stores photoUrls as a plain String column (comma-separated).
 * The API might return it as a JSON string or as an actual JSON array.
 * This deserializer handles both gracefully.
 */
private class PhotoUrlsDeserializer : JsonDeserializer<List<String>> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): List<String> {
        return when {
            json.isJsonArray -> {
                // Already a JSON array — parse normally
                json.asJsonArray.map { it.asString }
            }
            json.isJsonPrimitive -> {
                // Comma-separated string like "url1,url2,url3"
                val raw = json.asString.trim()
                if (raw.isEmpty()) emptyList()
                else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
            else -> emptyList()
        }
    }
}

object SiteClient {
    private const val BASE_URL = "https://site-service-waom.onrender.com/"
    private const val API_KEY  = "HARSH_MONUMENT_SECRET_2026"

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(
            object : TypeToken<List<String>>() {}.type,
            PhotoUrlsDeserializer()
        )
        .create()

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("x-api-key", API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()
            )
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: SiteApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(SiteApiService::class.java)
    }
}