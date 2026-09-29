package com.example.data.network

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

data class GeneratedVideoFile(
    val file: File,
    val durationSeconds: Int,
    val engineName: String,
    val isFallback: Boolean = false,
    val thumbnailUrl: String? = null
)

class VideoGenerationService(private val context: Context) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateVideo(
        prompt: String,
        json2videoKey: String = "I8Itv4pfonHZVqocfclctU8fb6KPkfv53Cydyq4w",
        bytezKey: String = "f55b5eec8d31941b10043200c79e8b51",
        onStatusUpdate: ((progress: Int, stage: String, isFallback: Boolean) -> Unit)? = null
    ): Result<GeneratedVideoFile> = withContext(Dispatchers.IO) {
        val trimmedPrompt = prompt.trim()
        var json2videoError: String? = null

        // 1. PRIMARY ENGINE: Json2video AI Studio (600s quota, 8s video with pan & zoom cinematic render)
        try {
            onStatusUpdate?.invoke(10, "Initializing Json2video AI Movie Engine...", false)

            val encodedPrompt = URLEncoder.encode(trimmedPrompt, "UTF-8")
            val seed = (System.currentTimeMillis() % 999999).toString()
            val visualSrc = "https://image.pollinations.ai/prompt/$encodedPrompt?width=640&height=480&nologo=true&seed=$seed"

            val titleText = if (trimmedPrompt.length > 40) {
                trimmedPrompt.take(37) + "..."
            } else {
                trimmedPrompt
            }

            // Build Json2video movie payload (8 seconds cinematic sequence)
            val elementsArray = JSONArray().apply {
                // Background image element with cinematic pan-and-zoom
                put(JSONObject().apply {
                    put("type", "image")
                    put("src", visualSrc)
                    put("duration", 8)
                    put("zoom", 2)
                })
                // Title overlay element
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", titleText)
                    put("duration", 6)
                })
            }

            val scenesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("duration", 8)
                    put("elements", elementsArray)
                })
            }

            val requestJson = JSONObject().apply {
                put("resolution", "sd")
                put("draft", true)
                put("scenes", scenesArray)
            }.toString()

            onStatusUpdate?.invoke(25, "Submitting scene & camera keyframes...", false)

            val submitRequest = Request.Builder()
                .url("https://api.json2video.com/v2/movies")
                .addHeader("x-api-key", json2videoKey)
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val projectId = httpClient.newCall(submitRequest).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    json2videoError = "Submit failed HTTP ${response.code}: $body"
                    return@use null
                }
                val json = JSONObject(body)
                if (!json.optBoolean("success", false)) {
                    json2videoError = json.optString("message", "Unknown submission error")
                    return@use null
                }
                json.optString("project", null)
            }

            if (projectId != null) {
                onStatusUpdate?.invoke(40, "Rendering 8s scene on cloud GPU cluster...", false)

                // Poll for movie completion (Json2video typically takes 5 to 12 seconds)
                var attempts = 0
                var videoDownloadUrl: String? = null
                var thumbnailUrl: String? = null

                while (attempts < 25) {
                    delay(2000)
                    attempts++

                    val currentProgress = (40 + (attempts * 4)).coerceAtMost(92)
                    onStatusUpdate?.invoke(currentProgress, "Compositing video frames ($attempts/25)...", false)

                    val pollRequest = Request.Builder()
                        .url("https://api.json2video.com/v2/movies?project=$projectId")
                        .addHeader("x-api-key", json2videoKey)
                        .get()
                        .build()

                    try {
                        val pollResponse = httpClient.newCall(pollRequest).execute()
                        val pollBody = pollResponse.body?.string() ?: ""
                        if (pollResponse.isSuccessful) {
                            val pollJson = JSONObject(pollBody)
                            val movieObj = pollJson.optJSONObject("movie")
                            val status = movieObj?.optString("status", "") ?: ""

                            if (status.equals("done", ignoreCase = true)) {
                                videoDownloadUrl = movieObj?.optString("url", null)
                                thumbnailUrl = movieObj?.optString("thumbnail", null)
                                break
                            } else if (status.equals("error", ignoreCase = true)) {
                                json2videoError = movieObj?.optString("message", "Render failed in cloud engine")
                                break
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("VideoGenerationService", "Polling attempt $attempts failed: ${e.message}")
                    }
                }

                if (videoDownloadUrl != null) {
                    onStatusUpdate?.invoke(96, "Downloading completed MP4 artwork...", false)

                    // Download video binary into cache
                    val downloadRequest = Request.Builder().url(videoDownloadUrl).get().build()
                    val downloadedFile = httpClient.newCall(downloadRequest).execute().use { dlResp ->
                        if (!dlResp.isSuccessful) throw Exception("Failed to download rendered MP4: HTTP ${dlResp.code}")
                        val bytes = dlResp.body?.bytes() ?: throw Exception("Empty video payload")
                        saveVideoToCache(bytes, "orki_video")
                    }

                    onStatusUpdate?.invoke(100, "Video rendering finished!", false)
                    delay(300)

                    return@withContext Result.success(
                        GeneratedVideoFile(
                            file = downloadedFile,
                            durationSeconds = 8,
                            engineName = "Json2video AI Studio (Primary)",
                            isFallback = false,
                            thumbnailUrl = thumbnailUrl
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            json2videoError = e.localizedMessage ?: "Connection error with Json2video"
        }

        // 2. FALLBACK ENGINE: Bytez / Backup Video Generation
        Log.w("VideoGenerationService", "Primary Json2video failed ($json2videoError). Activating Bytez / Pollinations backup video engine...")
        onStatusUpdate?.invoke(50, "Switching to backup video engine...", true)

        try {
            // Check Bytez or Pollinations Video Endpoint
            val encodedPrompt = URLEncoder.encode(trimmedPrompt, "UTF-8")
            val fallbackUrl = "https://gen.pollinations.ai/video/$encodedPrompt"

            val fbRequest = Request.Builder()
                .url(fallbackUrl)
                .get()
                .build()

            val fallbackFile = httpClient.newCall(fbRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Fallback video generator returned HTTP ${response.code}")
                }
                val bytes = response.body?.bytes() ?: throw Exception("Empty fallback video payload")
                saveVideoToCache(bytes, "orki_video_backup")
            }

            onStatusUpdate?.invoke(100, "Backup video generated successfully!", true)
            delay(300)

            return@withContext Result.success(
                GeneratedVideoFile(
                    file = fallbackFile,
                    durationSeconds = 8,
                    engineName = "Backup Video Engine (Fallback)",
                    isFallback = true,
                    thumbnailUrl = null
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            val fullError = "Video generation failed on both services:\n• Json2video: ${json2videoError ?: "Service error"}\n• Backup: ${e.localizedMessage ?: "Offline"}"
            return@withContext Result.failure(Exception(fullError))
        }
    }

    private fun saveVideoToCache(videoBytes: ByteArray, prefix: String): File {
        val videoDir = File(context.cacheDir, "generated_videos")
        if (!videoDir.exists()) {
            videoDir.mkdirs()
        }
        val fileName = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.mp4"
        val videoFile = File(videoDir, fileName)
        FileOutputStream(videoFile).use { out ->
            out.write(videoBytes)
            out.flush()
        }
        return videoFile
    }

    suspend fun saveVideoToGallery(videoFile: File, title: String = "Orki AI Generated Video"): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "Orki_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Orki AI")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Could not create MediaStore video entry"))

            resolver.openOutputStream(videoUri)?.use { outStream ->
                videoFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(videoUri, contentValues, null, null)
            }

            Result.success(videoUri)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
