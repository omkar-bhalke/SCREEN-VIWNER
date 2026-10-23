package com.example.gemini

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class TestResult {
    object Success : TestResult()
    data class Error(val message: String) : TestResult()
}

object GeminiApiTester {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun testKey(apiKey: String): TestResult = withContext(Dispatchers.IO) {
        val key = GeminiConfig.cleanKey(apiKey)
        if (key.isBlank()) {
            return@withContext TestResult.Error("API key cannot be empty.")
        }
        if (!GeminiConfig.isPlausibleKey(key)) {
            return@withContext TestResult.Error("Key looks incomplete or has invalid characters. Please copy the full key again.")
        }

        val bodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "hello") })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 64)
            })
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()

        var lastError = "No Gemini model was reachable with this key."
        try {
            for (model in GeminiConfig.MODELS) {
                val request = Request.Builder()
                    .url(GeminiConfig.endpoint(model))
                    .addHeader("x-goog-api-key", key)
                    .post(bodyJson.toString().toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    when {
                        // 200 = works. 429 = key is valid but quota is used up (still accepted).
                        response.isSuccessful || response.code == 429 -> return@withContext TestResult.Success
                        // Model not available -> try next model
                        response.code == 404 -> {
                            lastError = "Model $model not available (HTTP 404)."
                        }
                        response.code == 401 || response.code == 403 || response.code == 400 -> {
                            return@withContext TestResult.Error(
                                "Key rejected (HTTP ${response.code}): ${parseErrorMessage(responseBody)}"
                            )
                        }
                        else -> {
                            return@withContext TestResult.Error(
                                "Gemini test failed (HTTP ${response.code}): ${parseErrorMessage(responseBody)}"
                            )
                        }
                    }
                }
            }
            TestResult.Error(lastError)
        } catch (e: IOException) {
            TestResult.Error("Network error: unable to reach Google servers. Check your internet connection.")
        } catch (e: Exception) {
            TestResult.Error("Error validating key: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun parseErrorMessage(jsonStr: String?): String {
        if (jsonStr.isNullOrBlank()) return "No response from server"
        return try {
            JSONObject(jsonStr).optJSONObject("error")?.optString("message")
                ?.takeIf { it.isNotBlank() } ?: "Verification error"
        } catch (e: Exception) {
            "Verification error"
        }
    }
}
