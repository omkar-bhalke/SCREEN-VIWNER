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
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun testKey(apiKey: String): TestResult = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            return@withContext TestResult.Error("API key cannot be empty.")
        }
        if (!trimmed.startsWith("AIza")) {
            return@withContext TestResult.Error("Key format invalid. Gemini API keys typically start with 'AIza'.")
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
            val bodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "hello") })
                        })
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = bodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", trimmed)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful) {
                TestResult.Success
            } else {
                val errorMsg = when (response.code) {
                    400, 403 -> "Invalid API key (HTTP ${response.code}). Please verify your key on Google AI Studio."
                    429 -> "Rate limit reached (HTTP 429). Please check your AI Studio quota."
                    else -> "API test failed (HTTP ${response.code}): ${parseErrorMessage(responseBody)}"
                }
                TestResult.Error(errorMsg)
            }
        } catch (e: IOException) {
            TestResult.Error("Network error: Unable to reach Google servers. Please check your internet connection.")
        } catch (e: Exception) {
            TestResult.Error("Error validating key: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun parseErrorMessage(jsonStr: String?): String {
        if (jsonStr.isNullOrBlank()) return "No response from server"
        return try {
            val json = JSONObject(jsonStr)
            val error = json.optJSONObject("error")
            error?.optString("message") ?: "API verification error"
        } catch (e: Exception) {
            "Verification error"
        }
    }
}
