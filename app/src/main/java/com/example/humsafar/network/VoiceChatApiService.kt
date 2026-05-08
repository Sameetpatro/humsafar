// app/src/main/java/com/example/humsafar/network/VoiceChatApiService.kt

package com.example.humsafar.network

import com.example.humsafar.models.VoiceChatResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VoiceChatApiService {

    /**
     * Multipart voice exchange. site_id + (optional) node_id drive prompt context.
     * firebase_uid + trip_id are optional — when both are present and the user
     * is registered, the backend persists the user/assistant exchange to
     * user_chat_history. Calls without them still work fine.
     */
    @Multipart
    @POST("voice-chat")
    suspend fun sendVoiceMessage(
        @Part                       audio:        MultipartBody.Part,
        @Part("site_name")          siteName:     RequestBody,
        @Part("site_id")            siteId:       RequestBody,
        @Part("language")           language:     RequestBody,
        @Part("lang_name")          langName:     RequestBody,
        @Part("node_id")            nodeId:       RequestBody,
        @Part("firebase_uid")       firebaseUid:  RequestBody,
        @Part("trip_id")            tripId:       RequestBody
    ): VoiceChatResponse
}
