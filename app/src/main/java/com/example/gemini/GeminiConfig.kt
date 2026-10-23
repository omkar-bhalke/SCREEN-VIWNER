package com.example.gemini

/**
 * Central Gemini settings.
 *
 * Supports BOTH key formats:
 *  - Standard keys : AIza...
 *  - Auth keys     : AQ.Ab...  (new default in Google AI Studio)
 * Both are sent the same way, in the x-goog-api-key header (never as ?key=).
 */
object GeminiConfig {
    const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    // Newest first. If a model is not available for the key/region (HTTP 404),
    // the next one is tried automatically.
    // Gemini 3.8 Flash: $0.75 input / $3.75 output per 1M tokens (intro price until 31 Dec 2026).
    val MODELS = listOf(
        "gemini-3.8-flash",
        "gemini-3.7-flash",
        "gemini-3.5-flash"
    )

    fun endpoint(model: String) = "$BASE_URL/$model:generateContent"

    /** Removes spaces, new lines and wrapping quotes that often come with copy/paste. */
    fun cleanKey(raw: String?): String =
        raw.orEmpty().trim().trim('"', '\'').filter { !it.isWhitespace() }

    /** Accepts AIza..., AQ.... and any other Google key format. */
    fun isPlausibleKey(key: String): Boolean =
        key.length >= 20 && key.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }
}
