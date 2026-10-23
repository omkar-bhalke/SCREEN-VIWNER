package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AhiDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.UserMemoryEntity
import com.example.model.AhiEmotion
import com.example.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MemoryRepository(context: Context) {
    private val database = AhiDatabase.getDatabase(context)
    private val memoryDao = database.userMemoryDao()
    private val chatDao = database.chatMessageDao()

    private val prefs: SharedPreferences = context.getSharedPreferences("ahi_profile_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(
        UserProfile(
            userName = prefs.getString("user_name", "Alex") ?: "Alex",
            preferredLanguage = prefs.getString("preferred_language", "English") ?: "English",
            preferredStyle = prefs.getString("preferred_style", "Conversational, witty, and supportive") ?: "Conversational, witty, and supportive",
            currentEmotion = AhiEmotion.valueOf(prefs.getString("current_emotion", AhiEmotion.CONFIDENCE.name) ?: AhiEmotion.CONFIDENCE.name)
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    fun getAllMemories(): Flow<List<UserMemoryEntity>> = memoryDao.getAllMemories()

    suspend fun getMemoriesSnapshot(): List<UserMemoryEntity> = memoryDao.getMemoriesList()

    suspend fun saveMemory(category: String, title: String, content: String): Long {
        val entity = UserMemoryEntity(category = category, title = title, content = content)
        return memoryDao.insertMemory(entity)
    }

    suspend fun deleteMemory(memory: UserMemoryEntity) {
        memoryDao.deleteMemory(memory)
    }

    suspend fun deleteMemoryById(id: Long) {
        memoryDao.deleteById(id)
    }

    suspend fun updateMemory(memory: UserMemoryEntity) {
        memoryDao.updateMemory(memory)
    }

    // Chat history
    fun getAllMessages(): Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

    suspend fun getRecentMessages(): List<ChatMessageEntity> = chatDao.getRecentMessages()

    suspend fun recordMessage(text: String, isUser: Boolean, emotion: String): Long {
        val msg = ChatMessageEntity(text = text, isUser = isUser, emotion = emotion)
        return chatDao.insertMessage(msg)
    }

    suspend fun clearChatHistory() {
        chatDao.clearHistory()
    }

    fun updateProfile(userName: String, language: String, style: String, emotion: AhiEmotion) {
        prefs.edit()
            .putString("user_name", userName)
            .putString("preferred_language", language)
            .putString("preferred_style", style)
            .putString("current_emotion", emotion.name)
            .apply()

        _userProfile.value = UserProfile(
            userName = userName,
            preferredLanguage = language,
            preferredStyle = style,
            currentEmotion = emotion
        )
    }

    fun setEmotion(emotion: AhiEmotion) {
        prefs.edit().putString("current_emotion", emotion.name).apply()
        _userProfile.value = _userProfile.value.copy(currentEmotion = emotion)
    }
}
