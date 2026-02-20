// app/src/main/java/com/example/humsafar/network/VoiceChatApiService.kt
// NEW FILE

package com.example.humsafar.network

import com.example.humsafar.models.VoiceChatResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit interface for POST /voice-chat.
 *
 * Why multipart/form-data (not JSON):
 *   Binary audio + metadata in one request.
 *   FastAPI's File + Form params map directly to this.
 *   No base64 encoding of audio on the Android side → ~33% smaller payload.
 */
interface VoiceChatApiService {

    @Multipart
    @POST("voice-chat")
    suspend fun sendVoiceMessage(
        @Part                  audio:    MultipartBody.Part,
        @Part("site_name")     siteName: RequestBody,
        @Part("site_id")       siteId:   RequestBody,
        @Part("language")      language: RequestBody,   // BCP-47, e.g. "en-IN"
        @Part("lang_name")     langName: RequestBody    // "ENGLISH" | "HINDI" | "HINGLISH"
    ): VoiceChatResponse
}