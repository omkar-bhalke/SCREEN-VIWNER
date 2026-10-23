package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_memories")
data class UserMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // "GOAL", "PROJECT", "PREFERENCE", "FACT"
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val emotion: String = "CONFIDENCE"
)
