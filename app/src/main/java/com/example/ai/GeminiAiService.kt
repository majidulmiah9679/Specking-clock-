package com.example.ai

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object GeminiAiService {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Synthesizes studio-grade spoken audio using gemini-3.8-flash-tts
     */
    suspend fun generateAiSpeech(
        text: String,
        voiceName: String = "Kore"
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured in Secrets panel."))
        }

        try {
            val url = "${BASE_URL}gemini-3.8-flash-tts:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", text) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val genConfig = JSONObject().apply {
                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                    val speechConfig = JSONObject().apply {
                        val voiceConfig = JSONObject().apply {
                            val prebuiltVoiceConfig = JSONObject().apply {
                                put("voiceName", voiceName)
                            }
                            put("prebuiltVoiceConfig", prebuiltVoiceConfig)
                        }
                        put("voiceConfig", voiceConfig)
                    }
                    put("speechConfig", speechConfig)
                }
                put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gemini TTS Error ${response.code}: $responseBody"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val inlineData = firstPart?.optJSONObject("inlineData")
            val base64Data = inlineData?.optString("data")

            if (!base4OrEmpty(base64Data)) {
                val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                Result.success(audioBytes)
            } else {
                Result.failure(Exception("No audio data returned from Gemini TTS"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a custom chime or short musical alert clip using lyria-3-clip-preview
     */
    suspend fun generateLyriaChime(
        prompt: String = "A clean harmonic 3-second gentle grandfather clock chime bell"
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        try {
            val url = "${BASE_URL}lyria-3-clip-preview:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val genConfig = JSONObject().apply {
                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                }
                put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Lyria Music Error ${response.code}: $responseBody"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val inlineData = firstPart?.optJSONObject("inlineData")
            val base64Data = inlineData?.optString("data")

            if (!base4OrEmpty(base64Data)) {
                val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                Result.success(audioBytes)
            } else {
                Result.failure(Exception("No audio data returned from Lyria model"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun playAudio(context: Context, audioBytes: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val tempFile = File.createTempFile("gemini_audio_", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            val mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            // Player exception
        }
    }

    private fun base4OrEmpty(str: String?): Boolean = str.isNullOrEmpty()
}
