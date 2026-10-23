package com.example.model

enum class LiveState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

data class ToolExecution(
    val toolName: String,
    val title: String,
    val details: String,
    val iconName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class VoiceSettings(
    val pitch: Float = 1.15f,
    val speechRate: Float = 1.05f,
    val continuousMode: Boolean = true,
    val voiceName: String = "Kore (Dynamic Female)",
    val language: String = "English",
    val tone: String = "Charming & Witty"
)

data class AhiUiState(
    val liveState: LiveState = LiveState.IDLE,
    val audioAmplitude: Float = 0f,
    val userSpeechLive: String = "",
    val ahiSpokenText: String = "",
    val activeTool: ToolExecution? = null,
    val hasMicPermission: Boolean = false,
    val voiceSettings: VoiceSettings = VoiceSettings(),
    val connectionLatencyMs: Long = 38L,
    val userProfile: UserProfile = UserProfile(),
    val currentEmotion: AhiEmotion = AhiEmotion.CONFIDENCE,
    val proactiveHint: String? = null,
    val chatInputText: String = "",
    val memorySavedNotice: String? = null,
    val isConversationActive: Boolean = false,
    val showApiSetupDialog: Boolean = false,
    val hasSavedApiKey: Boolean = false,
    val isInternetConnected: Boolean = true,
    val showUnifiedPermissionModal: Boolean = false
)
