package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ParsedDualResponse(
    val displayText: String,
    val ttsText: String
)

class OrkiApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        const val STREAM_API_URL = "https://aqhedgyiworhbfkrzreb.supabase.co/functions/v1/quick-processor"
        const val TTS_API_URL = "https://aqhedgyiworhbfkrzreb.supabase.co/functions/v1/swift-worker"

        fun parseDualResponse(rawText: String): ParsedDualResponse {
            if (!rawText.contains("---TTS---")) {
                val trimmed = rawText.trim()
                return ParsedDualResponse(trimmed, trimmed)
            }
            val parts = rawText.split("---TTS---")
            val displayText = parts[0].trim()
            val ttsText = parts.drop(1).joinToString("---TTS---").trim()
            return ParsedDualResponse(
                displayText = if (displayText.isNotEmpty()) displayText else rawText.trim(),
                ttsText = if (ttsText.isNotEmpty()) ttsText else displayText
            )
        }

        fun ensureDevanagariForTTS(text: String): String {
            val devanagariRegex = Regex("[\\u0900-\\u097F]")
            if (devanagariRegex.containsMatchIn(text)) return text

            var res = text.lowercase()
                .replace(Regex("\\bkhulumbai\\b"), "खुलुमबाय")
                .replace(Regex("\\bmabwi\\b"), "माबोरै")
                .replace(Regex("\\bmabrwi\\b"), "माबोरै")
                .replace(Regex("\\bmabwrwi\\b"), "माबोरै")
                .replace(Regex("\\bmwjang\\b"), "मोजां")
                .replace(Regex("\\bmwzang\\b"), "मोजां")
                .replace(Regex("\\bjwmwi\\b"), "जोम्वै")
                .replace(Regex("\\bdong\\b"), "दं")
                .replace(Regex("\\bdonga\\b"), "दङ")
                .replace(Regex("\\bnwng\\b"), "नों")
                .replace(Regex("\\bnwngha\\b"), "नोंहा")
                .replace(Regex("\\bang\\b"), "आं")
                .replace(Regex("\\bangha\\b"), "आंहा")
                .replace(Regex("\\bma\\b"), "मा")
                .replace(Regex("\\bkhobor\\b"), "खबर")
                .replace(Regex("\\bthang\\b"), "थां")
                .replace(Regex("\\bthangnw\\b"), "थांनो")
                .replace(Regex("\\bkhalam\\b"), "खालाम")
                .replace(Regex("\\bkhalamnw\\b"), "खालामनो")
                .replace(Regex("\\bswr\\b"), "सोर")
                .replace(Regex("\\bboha\\b"), "बहा")
                .replace(Regex("\\bfai\\b"), "फै")
                .replace(Regex("\\bphai\\b"), "फै")
                .replace(Regex("\\bonkham\\b"), "ओंखाम")
                .replace(Regex("\\bwngkham\\b"), "ओंखाम")
                .replace("kh", "ख").replace("ph", "फ").replace("th", "थ")
                .replace("ng", "ं")
                .replace("wi", "ुइ").replace("ai", "ै").replace("ao", "ौ")
                .replace("jw", "जो").replace("nw", "नो").replace("bw", "बो").replace("w", "ु")
                .replace("b", "ब").replace("d", "द").replace("g", "ग").replace("h", "ह")
                .replace("j", "ज").replace("k", "क").replace("l", "ल").replace("m", "म")
                .replace("n", "न").replace("p", "प").replace("r", "र").replace("s", "स")
                .replace("t", "त").replace("y", "य").replace("z", "ज")
                .replace("a", "ा").replace("i", "ि").replace("u", "ु").replace("e", "े").replace("o", "ो")

            return res
        }

        fun cleanTextForTTS(rawText: String): String {
            val stripped = rawText
                .replace(Regex("[#*_`~>\\[\\]()]"), "")
                .replace(Regex("[\\p{So}\\p{Cn}]"), "")
                .trim()
            return ensureDevanagariForTTS(stripped)
        }
    }

    fun streamChat(
        plan: String,
        model: String,
        script: String,
        userPersona: String,
        contentsJson: JSONArray,
        isLiveMode: Boolean = false
    ): Flow<String> = callbackFlow {
        val payload = JSONObject().apply {
            put("plan", plan)
            put("model", model)
            put("script", script)
            put("userPersona", userPersona)
            put("contents", contentsJson)
            put("isLiveMode", isLiveMode)
            put("generationConfig", JSONObject().apply {
                put("temperature", if (isLiveMode) 0.25 else 0.35)
                put("maxOutputTokens", if (isLiveMode) 180 else if (model == "okafwr-2.1") 4000 else 1500)
            })
        }

        val request = Request.Builder()
            .url(STREAM_API_URL)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        val call = client.newCall(request)

        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                close(Exception("Server returned ${response.code}: $errorBody"))
                return@callbackFlow
            }

            val body = response.body
            if (body == null) {
                close(Exception("Empty response body"))
                return@callbackFlow
            }

            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("data: ")) {
                    val jsonStr = trimmed.substring(6).trim()
                    if (jsonStr.isNotEmpty()) {
                        try {
                            val parsed = JSONObject(jsonStr)
                            val candidates = parsed.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val content = candidates.getJSONObject(0).optJSONObject("content")
                                val parts = content?.optJSONArray("parts")
                                if (parts != null && parts.length() > 0) {
                                    val chunk = parts.getJSONObject(0).optString("text", "")
                                    if (chunk.isNotEmpty()) {
                                        trySend(chunk)
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
                line = reader.readLine()
            }
            close()
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            call.cancel()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun fetchTtsAudioUrl(text: String, voice: String = "female_mainao"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanText = cleanTextForTTS(text)
            if (cleanText.isEmpty()) {
                return@withContext Result.failure(Exception("Text for TTS is empty"))
            }

            // Map voice ID to gender expected by swift-worker and EC2 endpoint
            val gender = if (voice.contains("female", ignoreCase = true)) "female" else "male"

            val payload = JSONObject().apply {
                put("text", cleanText)
                put("gender", gender)
                put("voice", voice)
            }

            val request = Request.Builder()
                .url(TTS_API_URL)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: "HTTP ${response.code}"
                return@withContext Result.failure(Exception("TTS failed: $err"))
            }

            val resBody = response.body?.string() ?: ""
            val json = JSONObject(resBody)
            val audioUrl = json.optString("audioUrl", "")
            if (audioUrl.isNotEmpty()) {
                Result.success(audioUrl)
            } else {
                Result.failure(Exception("No audioUrl returned by TTS server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun warmUpEdgeFunctions() = withContext(Dispatchers.IO) {
        try {
            val payload = "{\"ping\":true,\"warmup\":true}".toRequestBody(jsonMediaType)
            val req1 = Request.Builder().url(STREAM_API_URL).post(payload).build()
            val req2 = Request.Builder().url(TTS_API_URL).post(payload).build()
            client.newCall(req1).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) { response.close() }
            })
            client.newCall(req2).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) { response.close() }
            })
        } catch (_: Exception) {}
    }
}
