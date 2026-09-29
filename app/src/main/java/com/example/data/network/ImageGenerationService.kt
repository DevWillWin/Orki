package com.example.data.network

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class ImageGenerationService(private val context: Context) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateImage(
        prompt: String,
        workerUrl: String = "https://orki-img-gen.devmightwin.workers.dev",
        apiKey: String = "Orki-Image-7xP6-kQ9m-81vL"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("prompt", prompt.trim())
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

            httpClient.newCall(request).execute().use { response ->
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

                    val diagnosticHint = if (cleanError.contains("Cannot read properties of undefined (reading 'run')")) {
                        "\n\n👉 Fix for Cloudflare Worker:\nYour worker script called `env.AI.run(...)`, but the Workers AI binding is not bound! In your Cloudflare Dashboard > Workers & Pages > orki-img-gen > Settings > Variables & Bindings, add a 'Workers AI' binding named 'AI' (or add `[ai]\\nbinding = \"AI\"` to your wrangler.toml) and redeploy."
                    } else ""

                    return@withContext Result.failure(Exception("$cleanError$diagnosticHint"))
                }

                val imageBytes = response.body?.bytes()
                    ?: return@withContext Result.failure(Exception("Empty image response from server"))

                // Verify valid bitmap format
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ?: return@withContext Result.failure(Exception("Couldn't read the generated image data"))

                // Save to local cache directory for fast instant rendering in UI
                val imageDir = File(context.cacheDir, "generated_images")
                if (!imageDir.exists()) {
                    imageDir.mkdirs()
                }

                val fileName = "orki_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png"
                val imageFile = File(imageDir, fileName)
                FileOutputStream(imageFile).use { out ->
                    out.write(imageBytes)
                    out.flush()
                }

                Result.success(imageFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(e.localizedMessage ?: "Network error during image generation"))
        }
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
