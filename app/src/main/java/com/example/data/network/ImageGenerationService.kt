package com.example.data.network

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

data class GeneratedImageFile(
    val file: File,
    val engineName: String,
    val isFallback: Boolean = false
)

class ImageGenerationService(private val context: Context) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateImage(
        prompt: String,
        workerUrl: String = "https://orki-img-gen.devmightwin.workers.dev",
        apiKey: String = "Orki-Image-7xP6-kQ9m-81vL",
        pollinationsKey: String = "sk_hFioWQ0q0wuc9SIncnI0Rm5mxTnrJS7o",
        onStatusUpdate: ((stage: String, isFallback: Boolean) -> Unit)? = null
    ): Result<GeneratedImageFile> = withContext(Dispatchers.IO) {
        val trimmedPrompt = prompt.trim()
        var cloudflareError: String? = null

        // 1. Try Primary Engine: Cloudflare Worker
        try {
            onStatusUpdate?.invoke("Connecting to Orki AI Worker (Cloudflare)...", false)

            val json = JSONObject()
                .put("prompt", trimmedPrompt)
                .toString()

            val normalizedUrl = if (workerUrl.startsWith("http://") || workerUrl.startsWith("https://")) {
                workerUrl
            } else {
                "https://$workerUrl"
            }

            val request = Request.Builder()
                .url(normalizedUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val cfResult: File? = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                    val cleanError = try {
                        val errorJson = JSONObject(errorBody)
                        val mainMsg = errorJson.optString("error", "")
                        val details = errorJson.optString("details", "")
                        when {
                            mainMsg.isNotBlank() && details.isNotBlank() -> "$mainMsg: $details"
                            mainMsg.isNotBlank() -> mainMsg
                            details.isNotBlank() -> details
                            else -> errorBody
                        }
                    } catch (_: Exception) {
                        errorBody
                    }
                    cloudflareError = "HTTP ${response.code} ($cleanError)"
                    return@use null
                }

                val imageBytes = response.body?.bytes()
                if (imageBytes == null || imageBytes.isEmpty()) {
                    cloudflareError = "Empty response body from Cloudflare Worker"
                    return@use null
                }

                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap == null) {
                    cloudflareError = "Failed to decode image bytes from Cloudflare Worker"
                    return@use null
                }

                saveToCache(imageBytes, "orki_cf")
            }

            if (cfResult != null) {
                return@withContext Result.success(
                    GeneratedImageFile(
                        file = cfResult,
                        engineName = "Orki AI (Cloudflare)",
                        isFallback = false
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cloudflareError = e.localizedMessage ?: "Cloudflare network connection error"
        }

        // 2. Primary Engine Failed or Limit Reached -> Automatic Fallback to Pollinations AI
        Log.w("ImageGenerationService", "Primary Cloudflare Worker failed: $cloudflareError. Switching to Pollinations AI fallback...")
        onStatusUpdate?.invoke("Cloudflare limit reached. Switching to Pollinations AI backup...", true)

        try {
            val encodedPrompt = URLEncoder.encode(trimmedPrompt, "UTF-8")
            val pollinationsUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=1024&height=1024&nologo=true&seed=${System.currentTimeMillis() % 100000}"

            val pollRequestBuilder = Request.Builder()
                .url(pollinationsUrl)

            if (pollinationsKey.isNotBlank()) {
                pollRequestBuilder.addHeader("Authorization", "Bearer $pollinationsKey")
            }

            val fallbackFile: File = httpClient.newCall(pollRequestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    val pollError = response.body?.string() ?: "HTTP ${response.code}"
                    throw Exception("Fallback Pollinations AI failed: HTTP ${response.code} ($pollError). Primary Cloudflare error: $cloudflareError")
                }

                val imageBytes = response.body?.bytes()
                    ?: throw Exception("Empty image response from fallback Pollinations AI")

                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ?: throw Exception("Could not decode image bytes from Pollinations AI")

                saveToCache(imageBytes, "orki_pollinations")
            }

            return@withContext Result.success(
                GeneratedImageFile(
                    file = fallbackFile,
                    engineName = "Pollinations AI (Backup)",
                    isFallback = true
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            val finalMsg = "Image generation failed on both services:\n• Cloudflare Worker: ${cloudflareError ?: "Failed/Quota exceeded"}\n• Pollinations AI: ${e.localizedMessage ?: "Network error"}"
            return@withContext Result.failure(Exception(finalMsg))
        }
    }

    private fun saveToCache(imageBytes: ByteArray, prefix: String): File {
        val imageDir = File(context.cacheDir, "generated_images")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }
        val fileName = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png"
        val imageFile = File(imageDir, fileName)
        FileOutputStream(imageFile).use { out ->
            out.write(imageBytes)
            out.flush()
        }
        return imageFile
    }

    suspend fun saveImageToGallery(imageFile: File, title: String = "Orki AI Generated Image"): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "Orki_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Orki AI")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Could not create MediaStore entry"))

            resolver.openOutputStream(imageUri)?.use { outStream ->
                imageFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            Result.success(imageUri)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
