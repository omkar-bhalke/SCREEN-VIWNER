package com.example.gemini

import android.util.Log
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
    data class Error(val message: String, val isNetworkIssue: Boolean = false) : TestResult()
}

object GeminiApiTester {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun testKey(apiKey: String): TestResult = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            return@withContext TestResult.Error("API key cannot be empty.")
        }
        if (trimmed.length < 8) {
            return@withContext TestResult.Error("API key is too short. Please enter a valid key.")
        }

        // Test with Gemini Flash models (supporting gemini-3.8-flash, gemini-2.5-flash, gemini-flash-latest)
        val candidateModels = listOf("gemini-3.8-flash", "gemini-2.5-flash", "gemini-flash-latest")
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val pingJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "hi") })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 5)
            })
        }
        val requestBody = pingJson.toString().toRequestBody(mediaType)

        var lastError: TestResult.Error? = null

        for (model in candidateModels) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$trimmed"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", trimmed)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseCode = response.code
                val responseBody = response.body?.string()

                if (response.isSuccessful) {
                    return@withContext TestResult.Success
                }

                // If 404, it might mean the specific model alias differs, try next candidate
                if (responseCode == 404) {
                    Log.w("GeminiApiTester", "Model $model returned 404, trying next...")
                    continue
                }

                val errorMsg = when (responseCode) {
                    400, 403 -> "Invalid API key (HTTP $responseCode). Please check your key from Google AI Studio."
                    429 -> "Rate limit reached (HTTP 429). Please check your AI Studio quota."
                    else -> "API test failed (HTTP $responseCode): ${parseErrorMessage(responseBody)}"
                }
                return@withContext TestResult.Error(errorMsg, isNetworkIssue = false)

            } catch (e: IOException) {
                Log.e("GeminiApiTester", "Network exception when testing key with $model", e)
                lastError = TestResult.Error(
                    "Network error: Unable to reach Google servers (${e.localizedMessage ?: "timeout"}). Check your connection.",
                    isNetworkIssue = true
                )
            } catch (e: Exception) {
                Log.e("GeminiApiTester", "General error testing key", e)
                lastError = TestResult.Error("Error validating key: ${e.localizedMessage ?: "Unknown error"}")
            }
        }

        return@withContext lastError ?: TestResult.Error("Unable to validate key. Please check connection.")
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
