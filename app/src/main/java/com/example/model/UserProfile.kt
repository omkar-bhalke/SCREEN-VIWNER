package com.example.model

data class UserProfile(
    val userName: String = "Alex",
    val preferredLanguage: String = "English", // "English", "Hindi", "Hinglish"
    val preferredStyle: String = "Conversational, witty, and supportive",
    val currentEmotion: AhiEmotion = AhiEmotion.CONFIDENCE
)
