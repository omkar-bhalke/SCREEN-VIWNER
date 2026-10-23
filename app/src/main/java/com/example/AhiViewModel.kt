package com.example

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecordService
import com.example.audio.AudioStreamer
import com.example.data.local.ChatMessageEntity
import com.example.data.local.UserMemoryEntity
import com.example.data.repository.MemoryRepository
import com.example.gemini.LiveSession
import com.example.gemini.SessionResponse
import com.example.model.AhiEmotion
import com.example.model.AhiUiState
import com.example.model.LiveState
import com.example.model.ToolExecution
import com.example.model.UserProfile
import com.example.permission.UnifiedPermissionManager
import com.example.permission.UnifiedReadinessState
import com.example.security.KeystoreManager
import com.example.tools.ToolManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AhiViewModel(application: Application) : AndroidViewModel(application) {

    val keystoreManager = KeystoreManager(application.applicationContext)
    val memoryRepository = MemoryRepository(application.applicationContext)
    val unifiedPermissionManager = UnifiedPermissionManager(application.applicationContext, viewModelScope)

    val memories: StateFlow<List<UserMemoryEntity>> = memoryRepository.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = memoryRepository.userProfile

    val chatMessages: StateFlow<List<ChatMessageEntity>> = memoryRepository.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(
        AhiUiState(
            hasSavedApiKey = keystoreManager.isConfigured(),
            showApiSetupDialog = !keystoreManager.isConfigured()
        )
    )
    val uiState: StateFlow<AhiUiState> = _uiState.asStateFlow()

    private val toolManager = ToolManager(application.applicationContext)
    private val liveSession = LiveSession(toolManager)

    private var audioStreamer: AudioStreamer? = null
    private var audioPlayer: AudioPlayer? = null
    private var toolDismissJob: Job? = null
    private var toastDismissJob: Job? = null

    init {
        initAudioEngine()
        seedInitialMemoryIfEmpty()

        viewModelScope.launch {
            userProfile.collect { profile ->
                _uiState.update {
                    it.copy(
                        userProfile = profile,
                        currentEmotion = profile.currentEmotion
                    )
                }
            }
        }

        viewModelScope.launch {
            unifiedPermissionManager.isMicGranted.collect { granted ->
                _uiState.update { it.copy(hasMicPermission = granted) }
            }
        }

        viewModelScope.launch {
            unifiedPermissionManager.isInternetConnected.collect { connected ->
                _uiState.update { it.copy(isInternetConnected = connected) }
            }
        }

        viewModelScope.launch {
            AudioRecordService.amplitudeFlow.collect { amp ->
                if (_uiState.value.liveState == LiveState.LISTENING) {
                    _uiState.update { it.copy(audioAmplitude = amp.coerceAtLeast(0.06f)) }
                }
            }
        }

        viewModelScope.launch {
            AudioRecordService.errorFlow.collect { err ->
                Log.w("AhiViewModel", "AudioRecordService error: $err")
            }
        }
    }

    private fun seedInitialMemoryIfEmpty() {
        viewModelScope.launch {
            delay(400)
            val current = memoryRepository.getMemoriesSnapshot()
            if (current.isEmpty()) {
                memoryRepository.saveMemory(
                    category = "PROJECT",
                    title = "Android AI Companion",
                    content = "Building AHI with modern Jetpack Compose, real-time voice, and persistent memory."
                )
                memoryRepository.saveMemory(
                    category = "GOAL",
                    title = "Kotlin & AI Mastery",
                    content = "Achieve state-of-the-art mobile intelligence and ultra-fast voice responses."
                )
            }
        }
    }

    private fun initAudioEngine() {
        audioPlayer = AudioPlayer(
            context = getApplication(),
            scope = viewModelScope,
            onSpeechStarted = {
                _uiState.update { it.copy(liveState = LiveState.SPEAKING) }
            },
            onSpeechCompleted = {
                val state = _uiState.value.liveState
                if (state == LiveState.SPEAKING) {
                    if (_uiState.value.voiceSettings.continuousMode && _uiState.value.hasMicPermission) {
                        viewModelScope.launch {
                            delay(250)
                            startListening()
                        }
                    } else {
                        _uiState.update { it.copy(liveState = LiveState.IDLE) }
                    }
                }
            },
            onAmplitudeChanged = { amp ->
                if (_uiState.value.liveState == LiveState.SPEAKING) {
                    _uiState.update { it.copy(audioAmplitude = amp) }
                }
            }
        )

        audioStreamer = AudioStreamer(
            context = getApplication(),
            scope = viewModelScope,
            onPartialResult = { partial ->
                _uiState.update { it.copy(userSpeechLive = partial) }
            },
            onFinalResult = { finalSpeech ->
                processUserInteraction(finalSpeech, isSpoken = true)
            },
            onRmsChanged = { rms ->
                if (_uiState.value.liveState == LiveState.LISTENING) {
                    _uiState.update { it.copy(audioAmplitude = rms) }
                }
            },
            onErrorState = { err ->
                if (_uiState.value.liveState == LiveState.LISTENING) {
                    _uiState.update { it.copy(liveState = LiveState.IDLE, audioAmplitude = 0f) }
                }
            }
        )
    }

    fun saveApiKeyFromSetup(key: String) {
        keystoreManager.saveApiKey(key)
        _uiState.update {
            it.copy(
                hasSavedApiKey = true,
                showApiSetupDialog = false
            )
        }
    }

    fun openApiSetup() {
        _uiState.update { it.copy(showApiSetupDialog = true) }
    }

    fun dismissApiSetup() {
        _uiState.update { it.copy(showApiSetupDialog = false) }
    }

    fun removeApiKey() {
        keystoreManager.clearApiKey()
        _uiState.update {
            it.copy(
                hasSavedApiKey = false,
                showApiSetupDialog = true
            )
        }
    }

    fun setMicPermissionGranted(granted: Boolean) {
        unifiedPermissionManager.onMicPermissionResult(granted)
        _uiState.update { it.copy(hasMicPermission = granted) }
    }

    fun showUnifiedPermissionModal() {
        _uiState.update { it.copy(showUnifiedPermissionModal = true) }
    }

    fun dismissUnifiedPermissionModal() {
        _uiState.update { it.copy(showUnifiedPermissionModal = false) }
    }

    fun retryConnectionAndPermissions() {
        unifiedPermissionManager.refreshState()
    }

    fun onInputTextChange(newText: String) {
        _uiState.update { it.copy(chatInputText = newText) }
    }

    fun toggleVoiceListening() {
        val readiness = unifiedPermissionManager.checkCurrentReadiness()
        if (readiness != UnifiedReadinessState.ALL_READY) {
            _uiState.update { it.copy(showUnifiedPermissionModal = true) }
            return
        }

        when (_uiState.value.liveState) {
            LiveState.LISTENING -> {
                AudioRecordService.stop(getApplication())
                audioStreamer?.stopListening()
                _uiState.update { it.copy(liveState = LiveState.IDLE, audioAmplitude = 0f) }
            }
            LiveState.SPEAKING -> {
                interruptAndListen()
            }
            else -> {
                startListening()
            }
        }
    }

    fun onOrbClicked() {
        toggleVoiceListening()
    }

    fun startListening() {
        val readiness = unifiedPermissionManager.checkCurrentReadiness()
        if (readiness != UnifiedReadinessState.ALL_READY) {
            _uiState.update { it.copy(showUnifiedPermissionModal = true) }
            return
        }
        audioPlayer?.stop()
        _uiState.update {
            it.copy(
                liveState = LiveState.LISTENING,
                audioAmplitude = 0.08f,
                userSpeechLive = ""
            )
        }
        AudioRecordService.start(getApplication())
        audioStreamer?.startListening()
    }

    fun interruptAndListen() {
        audioPlayer?.stop()
        startListening()
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        _uiState.update { it.copy(chatInputText = "") }
        processUserInteraction(trimmed, isSpoken = false)
    }

    private fun processUserInteraction(input: String, isSpoken: Boolean) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        AudioRecordService.stop(getApplication())
        audioStreamer?.stopListening()
        _uiState.update {
            it.copy(
                liveState = LiveState.THINKING,
                userSpeechLive = trimmed,
                audioAmplitude = 0.25f,
                isConversationActive = true
            )
        }

        viewModelScope.launch {
            memoryRepository.recordMessage(
                text = trimmed,
                isUser = true,
                emotion = _uiState.value.currentEmotion.name
            )

            val currentMemories = memories.value
            val currentProfile = userProfile.value
            val savedKey = keystoreManager.getApiKey()

            val (responseText, sessionResponse) = liveSession.processUserSpeech(
                userText = trimmed,
                userProfile = currentProfile,
                memories = currentMemories,
                apiKey = savedKey
            )

            if (sessionResponse is SessionResponse.SaveMemoryAction) {
                memoryRepository.saveMemory(
                    category = sessionResponse.category,
                    title = sessionResponse.title,
                    content = sessionResponse.content
                )
                showMemoryToast("Memory saved: ${sessionResponse.title}")
            }

            memoryRepository.recordMessage(
                text = responseText,
                isUser = false,
                emotion = currentProfile.currentEmotion.name
            )

            _uiState.update {
                it.copy(
                    liveState = LiveState.SPEAKING,
                    ahiSpokenText = responseText
                )
            }

            audioPlayer?.speak(responseText)
        }
    }

    fun addManualMemory(category: String, title: String, content: String) {
        viewModelScope.launch {
            memoryRepository.saveMemory(category, title, content)
            showMemoryToast("Saved to memory: $title")
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryRepository.deleteMemoryById(id)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            memoryRepository.clearChatHistory()
            liveSession.clearHistory()
            _uiState.update {
                it.copy(
                    ahiSpokenText = "",
                    userSpeechLive = "",
                    isConversationActive = false
                )
            }
        }
    }

    fun updateProfile(userName: String, language: String, style: String, emotion: AhiEmotion) {
        memoryRepository.updateProfile(userName, language, style, emotion)
    }

    fun updateVoicePitchAndRate(pitch: Float, rate: Float) {
        audioPlayer?.setPitchAndRate(pitch, rate)
        _uiState.update {
            it.copy(voiceSettings = it.voiceSettings.copy(pitch = pitch, speechRate = rate))
        }
    }

    fun updateVoiceSettings(settings: com.example.model.VoiceSettings) {
        audioPlayer?.setPitchAndRate(settings.pitch, settings.speechRate)
        audioPlayer?.setLanguage(settings.language)
        _uiState.update {
            it.copy(voiceSettings = settings)
        }
    }

    fun testVoicePreview(text: String) {
        audioPlayer?.stop()
        audioPlayer?.speak(text)
    }

    fun setEmotion(emotion: AhiEmotion) {
        memoryRepository.setEmotion(emotion)
    }

    private fun showMemoryToast(msg: String) {
        toastDismissJob?.cancel()
        _uiState.update { it.copy(memorySavedNotice = msg) }
        toastDismissJob = viewModelScope.launch {
            delay(3500)
            _uiState.update { it.copy(memorySavedNotice = null) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer?.release()
        audioStreamer?.release()
        AudioRecordService.stop(getApplication())
        unifiedPermissionManager.release()
    }
}
