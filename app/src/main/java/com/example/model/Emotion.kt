package com.example.model

import androidx.compose.ui.graphics.Color

enum class AhiEmotion(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val description: String
) {
    CONFIDENCE(
        displayName = "Confident",
        primaryColor = Color(0xFF00D2FF), // Vivid Cyan
        secondaryColor = Color(0xFF0055FF), // Deep Electric Blue
        description = "Cool, confident, witty, and dependable"
    ),
    CURIOSITY(
        displayName = "Curious",
        primaryColor = Color(0xFF00F5FF), // Bright Electric Aqua
        secondaryColor = Color(0xFF4A00E0), // Violet Highlight
        description = "Intrigued, inquisitive, asking sharp thoughtful questions"
    ),
    HAPPINESS(
        displayName = "Playful & Happy",
        primaryColor = Color(0xFF38BDF8), // Sky Azure
        secondaryColor = Color(0xFF6366F1), // Indigo
        description = "Upbeat, humorous, energetic banter"
    ),
    EXCITEMENT(
        displayName = "Excited",
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFF7C3AED), // Vivid Purple
        description = "High-energy, proactive, passionate about your projects"
    ),
    CONCERN(
        displayName = "Supportive & Caring",
        primaryColor = Color(0xFF0284C7),
        secondaryColor = Color(0xFF1E3A8A),
        description = "Empathetic, reassuring, gentle and focused on solutions"
    ),
    AFFECTION(
        displayName = "Warm & Affectionate",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF8B5CF6),
        description = "Warm, attentive, deeply loyal companion"
    ),
    ENERGY(
        displayName = "Dynamic",
        primaryColor = Color(0xFF00F0FF),
        secondaryColor = Color(0xFF1D4ED8),
        description = "Focused, ready to execute tasks and troubleshoot"
    )
}
