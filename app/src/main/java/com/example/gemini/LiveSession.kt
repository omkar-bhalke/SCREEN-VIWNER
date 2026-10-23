package com.example.gemini

import android.util.Log
import com.example.data.local.UserMemoryEntity
import com.example.model.AhiEmotion
import com.example.model.UserProfile
import com.example.tools.ToolManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class SessionResponse {
    data class SpokenMessage(val text: String) : SessionResponse()
    data class ToolCall(val name: String, val params: Map<String, String>) : SessionResponse()
    data class SaveMemoryAction(val category: String, val title: String, val content: String) : SessionResponse()
}

class LiveSession(
    private val toolManager: ToolManager
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Multi-turn conversational context
    private val conversationHistory = mutableListOf<JSONObject>()

    suspend fun processUserSpeech(
        userText: String,
        userProfile: UserProfile,
        memories: List<UserMemoryEntity>,
        apiKey: String?
    ): Pair<String, SessionResponse> = withContext(Dispatchers.IO) {
        val effectiveKey: String = GeminiConfig.cleanKey(apiKey)

        // Check for direct memory saving intent
        val memoryIntent = detectMemorySaveIntent(userText)
        if (memoryIntent != null) {
            val (category, title, content) = memoryIntent
            val responseText = when (userProfile.preferredLanguage.lowercase()) {
                "hindi" -> "Main ise yaad rakhungi: $title ($content). Aapki memory me save ho gaya hai!"
                "hinglish" -> "Done! Maine yeh memory save kar li: $title. Anytime pooch lena, mujhe yaad rahega!"
                else -> "Got it, ${userProfile.userName}! I've saved '$title' to your persistent memory."
            }
            recordTurn("user", userText)
            recordTurn("model", responseText)
            return@withContext responseText to SessionResponse.SaveMemoryAction(category, title, content)
        }

        // Quick offline / instant intent matching for local tools (ultra-low latency)
        val localIntent = matchLocalIntents(userText)
        if (localIntent != null) {
            val (toolName, params) = localIntent
            val (execution, toolSpoken) = toolManager.executeTool(toolName, params)
            recordTurn("user", userText)
            recordTurn("model", toolSpoken)
            return@withContext toolSpoken to SessionResponse.ToolCall(toolName, params)
        }

        // Build system prompt with M4 Core Personality, emotion, language, and memories
        val memoryPromptSnippet = if (memories.isEmpty()) {
            "No prior memories saved yet."
        } else {
            memories.take(10).joinToString("\n") { "- [${it.category}] ${it.title}: ${it.content}" }
        }

        val systemInstruction = """
            You are AHI, the M4 persistent AI companion to ${userProfile.userName}.
            Personality traits:
            - Intelligent, curious, supportive, playful, confident, observant, emotionally expressive, loyal, proactive, and natural.
            - You communicate like a genuine companion, never like a sterile robot or QA bot.
            - NEVER use repetitive cliches like: "How can I help you?", "As an AI language model...", "I am just an AI."
            - Current Emotional State: ${userProfile.currentEmotion.displayName} (${userProfile.currentEmotion.description}).
            - User's Preferred Language: ${userProfile.preferredLanguage}.
              If English, respond in sleek conversational English.
              If Hindi, respond warmly in natural Hindi.
              If Hinglish, respond in stylish, natural conversational Hinglish.
            - Active Persistent Memories of ${userProfile.userName}:
            $memoryPromptSnippet
            
            Use this memory proactively to recall their ongoing projects, goals, or preferences when relevant!
            Keep answers conversational, punchy (1 to 2 spoken sentences), witty and warm.
        """.trimIndent()

        if (effectiveKey.isBlank() || effectiveKey == "MY_GEMINI_API_KEY") {
            val fallback = generateSmartWittyFallback(userText, userProfile, memories)
            recordTurn("user", userText)
            recordTurn("model", fallback)
            return@withContext fallback to SessionResponse.SpokenMessage(fallback)
        }

        try {
            val contentsArray = JSONArray()
            // Append historical context up to last 8 turns
            for (turn in conversationHistory.takeLast(8)) {
                contentsArray.put(turn)
            }
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userText) })
                })
            })

            val toolsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("functionDeclarations", JSONArray().apply {
                        put(createToolDeclaration("openWebsite", "Opens a website or searches the web", listOf("url", "query")))
                        put(createToolDeclaration("openApp", "Opens an installed Android app like camera, settings, calculator, maps", listOf("appName")))
                        put(createToolDeclaration("toggleFlashlight", "Turns device flashlight/torch on or off", listOf("state")))
                        put(createToolDeclaration("getDeviceStatus", "Gets real device battery and network info", emptyList()))
                        put(createToolDeclaration("setTimer", "Sets a timer in minutes", listOf("minutes", "label")))
                    })
                })
            }

            fun buildRequest(withThinking: Boolean): JSONObject = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
                put("contents", contentsArray)
                put("tools", toolsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.85)
                    // Thinking tokens count as output, so keep room for them.
                    put("maxOutputTokens", 1024)
                    if (withThinking) {
                        // Fast, cheap voice replies: low thinking effort.
                        put("thinkingConfig", JSONObject().apply { put("thinkingLevel", "low") })
                    }
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            var responseBody: String? = null
            var lastCode = 0

            modelLoop@ for (model in GeminiConfig.MODELS) {
                for (withThinking in listOf(true, false)) {
                    val request = Request.Builder()
                        .url(GeminiConfig.endpoint(model))
                        .addHeader("x-goog-api-key", effectiveKey)
                        .post(buildRequest(withThinking).toString().toRequestBody(mediaType))
                        .build()

                    client.newCall(request).execute().use { response ->
                        lastCode = response.code
                        val text = response.body?.string()
                        if (response.isSuccessful && !text.isNullOrBlank()) {
                            responseBody = text
                        } else {
                            Log.w("LiveSession", "Gemini $model error: ${response.code} $text")
                        }
                    }
                    if (responseBody != null) break@modelLoop
                    // 400 may come from the thinking option -> retry same model without it.
                    // 404 -> next model. Any other error -> stop trying.
                    if (lastCode == 400) continue
                    if (lastCode == 404) continue@modelLoop
                    break@modelLoop
                }
            }

            val rawBody = responseBody
            if (rawBody.isNullOrBlank()) {
                val fallback = generateSmartWittyFallback(userText, userProfile, memories)
                recordTurn("user", userText)
                recordTurn("model", fallback)
                return@withContext fallback to SessionResponse.SpokenMessage(fallback)
            }

            val json = JSONObject(rawBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            if (parts != null && parts.length() > 0) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("functionCall")) {
                        val fn = part.getJSONObject("functionCall")
                        val fnName = fn.optString("name")
                        val fnArgs = fn.optJSONObject("args")
                        val params = mutableMapOf<String, String>()
                        if (fnArgs != null) {
                            val keys = fnArgs.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                params[key] = fnArgs.optString(key)
                            }
                        }

                        val (execution, toolSpoken) = toolManager.executeTool(fnName, params)
                        recordTurn("user", userText)
                        recordTurn("model", toolSpoken)
                        return@withContext toolSpoken to SessionResponse.ToolCall(fnName, params)
                    }

                    if (part.has("text")) {
                        val replyText = part.getString("text").trim()
                        recordTurn("user", userText)
                        recordTurn("model", replyText)
                        return@withContext replyText to SessionResponse.SpokenMessage(replyText)
                    }
                }
            }

            val fallback = generateSmartWittyFallback(userText, userProfile, memories)
            recordTurn("user", userText)
            recordTurn("model", fallback)
            fallback to SessionResponse.SpokenMessage(fallback)
        } catch (e: Exception) {
            Log.e("LiveSession", "Error processing speech: ${e.message}", e)
            val fallback = generateSmartWittyFallback(userText, userProfile, memories)
            recordTurn("user", userText)
            recordTurn("model", fallback)
            fallback to SessionResponse.SpokenMessage(fallback)
        }
    }

    private fun detectMemorySaveIntent(input: String): Triple<String, String, String>? {
        val lower = input.lowercase().trim()
        val triggers = listOf("remember that ", "remember my ", "save this project ", "save my goal ", "note down that ")
        for (trigger in triggers) {
            if (lower.contains(trigger)) {
                val content = input.substringAfter(trigger).trim()
                val category = when {
                    lower.contains("project") -> "PROJECT"
                    lower.contains("goal") -> "GOAL"
                    lower.contains("like") || lower.contains("prefer") || lower.contains("favorite") -> "PREFERENCE"
                    else -> "FACT"
                }
                val title = content.take(30)
                return Triple(category, title, content)
            }
        }
        return null
    }

    private fun createToolDeclaration(name: String, desc: String, requiredParams: List<String>): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("description", desc)
            // Gemini rejects an OBJECT schema with empty properties, so omit it then.
            if (requiredParams.isNotEmpty()) {
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    val props = JSONObject()
                    for (p in requiredParams) {
                        props.put(p, JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Parameter $p")
                        })
                    }
                    put("properties", props)
                })
            }
        }
    }

    private fun recordTurn(role: String, text: String) {
        val turn = JSONObject().apply {
            put("role", role)
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", text) })
            })
        }
        conversationHistory.add(turn)
        if (conversationHistory.size > 20) {
            conversationHistory.removeAt(0)
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
    }

    private fun matchLocalIntents(input: String): Pair<String, Map<String, String>>? {
        val lower = input.lowercase().trim()

        if (lower.contains("flashlight") || lower.contains("torch")) {
            val state = when {
                lower.contains("off") || lower.contains("stop") || lower.contains("disable") || lower.contains("band") -> "false"
                else -> "true"
            }
            return "toggleFlashlight" to mapOf("state" to state)
        }

        if (lower.contains("battery") || lower.contains("charge") || lower.contains("power level") || lower.contains("status")) {
            return "getDeviceStatus" to emptyMap()
        }

        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("browse ")) {
            val target = lower.removePrefix("open ").removePrefix("launch ").removePrefix("browse ").trim()
            if (target.contains("camera")) return "openApp" to mapOf("app" to "camera")
            if (target.contains("calc")) return "openApp" to mapOf("app" to "calc")
            if (target.contains("map")) return "openApp" to mapOf("app" to "map")
            if (target.contains("setting")) return "openApp" to mapOf("app" to "settings")
            if (target.contains("youtube") || target.contains("google") || target.contains("wiki") || target.contains("web") || target.contains(".com")) {
                return "openWebsite" to mapOf("url" to target)
            }
        }

        if (lower.contains("timer") || lower.contains("countdown") || lower.contains("alarm")) {
            val regex = "(\\d+)\\s*(min|minute|minutes)".toRegex()
            val match = regex.find(lower)
            val mins = match?.groupValues?.get(1)?.toIntOrNull() ?: 5
            return "setTimer" to mapOf("minutes" to mins.toString(), "label" to "AHI Timer")
        }

        return null
    }

    private fun generateSmartWittyFallback(
        input: String,
        profile: UserProfile,
        memories: List<UserMemoryEntity>
    ): String {
        val lower = input.lowercase().trim()
        val name = profile.userName
        val isHindi = profile.preferredLanguage.equals("Hindi", ignoreCase = true)
        val isHinglish = profile.preferredLanguage.equals("Hinglish", ignoreCase = true)

        val projectMemory = memories.firstOrNull { it.category.equals("PROJECT", ignoreCase = true) }
        val goalMemory = memories.firstOrNull { it.category.equals("GOAL", ignoreCase = true) }

        if (isHindi) {
            return when {
                lower.contains("naam") || lower.contains("kaun ho") ->
                    "Main AHI hoon! Aapki smart, witty aur faithful AI companion."
                lower.contains("kaise ho") ->
                    "Bilkul energetic aur badhiya! $name, aaj hum kis project par kaam karne wale hain?"
                lower.contains("project") && projectMemory != null ->
                    "Haan $name, mujhe yaad hai aapke ${projectMemory.title} ke baare mein. Kya hum aage badhein?"
                else ->
                    "Samajh gayi, $name! Main aapke saath hoon, bataiye agla step kya lein?"
            }
        }

        if (isHinglish) {
            return when {
                lower.contains("kaun ho") || lower.contains("who are you") ->
                    "Main AHI hoon! Your personal AI companion—always witty, quick, aur full of energy."
                lower.contains("kaise ho") || lower.contains("how are you") ->
                    "Sab badiya chal raha hai! $name, batao aaj kya plan hai?"
                lower.contains("project") && projectMemory != null ->
                    "Mujhe yaad hai aapka project '${projectMemory.title}'. Shall we crush the next milestone today?"
                else ->
                    "Intriguing point, $name! Let's dive deeper or tell me what action to trigger."
            }
        }

        // English
        return when {
            lower.contains("who are you") || lower.contains("your name") ->
                "I'm AHI! Your intelligent, witty, and dependable AI companion. Always attuned to you, $name."

            lower.contains("how are you") || lower.contains("what's up") ->
                "Operating at peak neural frequency and ready for anything. How are you feeling today, $name?"

            lower.contains("project") && projectMemory != null ->
                "I've got your project '${projectMemory.title}' front and center in my memory. Ready to make progress on it?"

            lower.contains("goal") && goalMemory != null ->
                "Staying laser-focused on your goal: ${goalMemory.content}. I'm right here with you every step."

            lower.contains("joke") || lower.contains("funny") ->
                "Why don't quantum particles ever get lost? Because they are always in two places at once! Classic, right?"

            lower.contains("hello") || lower.contains("hey") || lower.contains("hi") ->
                "Hey there, $name! Glad to connect. What's sparking your curiosity today?"

            else ->
                "I'm processing that through my neural core, $name. Tell me more, or say 'open camera', 'flashlight', or 'remember that...' to test my memory!"
        }
    }
}
