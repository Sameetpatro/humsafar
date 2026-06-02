package com.example.humsafar.data

import android.content.Context
import android.net.Uri
import com.example.humsafar.BuildConfig
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Uploads instant photos to Cloudinary (unsigned preset). Returns the public
 * HTTPS URL to store in PostgreSQL via POST /instants.
 *
 * Configure in local.properties:
 *   CLOUDINARY_CLOUD_NAME=dw8imuhcz
 *   CLOUDINARY_UPLOAD_PRESET=ds_instants
 */
object InstantStorageUploader {

    private val http = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun uploadPhoto(context: Context, siteId: Int, nodeId: Int, localUri: Uri): String =
        withContext(Dispatchers.IO) {
            val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
            val preset = BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim()
            check(cloudName.isNotEmpty() && preset.isNotEmpty()) {
                "Set CLOUDINARY_CLOUD_NAME and CLOUDINARY_UPLOAD_PRESET in local.properties"
            }

            val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
            val tempFile = File.createTempFile("instant_", ".jpg", context.cacheDir)
            try {
                context.contentResolver.openInputStream(localUri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Could not read selected image")

                val multipart = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("upload_preset", preset)
                    .addFormDataPart("folder", "instants/$siteId/$nodeId")
                    .addFormDataPart(
                        "file",
                        "instant.jpg",
                        tempFile.asRequestBody("image/jpeg".toMediaType())
                    )
                    .build()

                val response = http.newCall(
                    Request.Builder().url(uploadUrl).post(multipart).build()
                ).execute()

                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("Cloudinary upload failed (${response.code}): $body")
                }

                val json = JsonParser.parseString(body).asJsonObject
                json.get("secure_url")?.asString
                    ?: json.get("url")?.asString
                    ?: error("Cloudinary response missing image URL")
            } finally {
                tempFile.delete()
            }
        }
}
