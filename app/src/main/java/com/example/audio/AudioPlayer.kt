package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

class AudioPlayer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSpeechStarted: () -> Unit,
    private val onSpeechCompleted: () -> Unit,
    private val onAmplitudeChanged: (Float) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var amplitudeJob: Job? = null
    private var currentUtteranceId: String? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w("AudioPlayer", "US English missing, using default locale")
                    engine.language = Locale.getDefault()
                }

                // Try to find a smooth female voice
                val voices = engine.voices
                if (voices != null) {
                    val femaleVoice = voices.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        (name.contains("female") || name.contains("en-us-x-sfg") || name.contains("en-us-x-iom") || name.contains("en-us-x-iol")) &&
                                !voice.isNetworkConnectionRequired
                    } ?: voices.firstOrNull { it.locale.language == Locale.ENGLISH.language && it.name.lowercase().contains("female") }

                    if (femaleVoice != null) {
                        engine.voice = femaleVoice
                    }
                }

                engine.setPitch(1.15f)
                engine.setSpeechRate(1.06f)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            startSimulatingSpeakingWaveform()
                            onSpeechStarted()
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            stopSimulatingSpeakingWaveform()
                            onSpeechCompleted()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            stopSimulatingSpeakingWaveform()
                            onSpeechCompleted()
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        scope.launch(Dispatchers.Main) {
                            stopSimulatingSpeakingWaveform()
                            onSpeechCompleted()
                        }
                    }
                })

                isInitialized = true
                Log.d("AudioPlayer", "TTS initialized successfully")
            }
        } else {
            Log.e("AudioPlayer", "TTS initialization failed with code: $status")
        }
    }

    fun setPitchAndRate(pitch: Float, rate: Float) {
        tts?.setPitch(pitch)
        tts?.setSpeechRate(rate)
    }

    fun setLanguage(languageName: String) {
        val targetLocale = when {
            languageName.contains("Hindi", ignoreCase = true) -> Locale("hi", "IN")
            languageName.contains("Hinglish", ignoreCase = true) -> Locale("en", "IN")
            else -> Locale.US
        }
        tts?.let { engine ->
            val result = engine.setLanguage(targetLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.language = Locale.US
            }
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        stop()

        val utteranceId = "AHI_UTT_${System.currentTimeMillis()}"
        currentUtteranceId = utteranceId

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        if (isInitialized && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } else {
            // TTS initializing fallback
            scope.launch {
                delay(300)
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            }
        }
    }

    fun stop() {
        stopSimulatingSpeakingWaveform()
        tts?.stop()
    }

    private fun startSimulatingSpeakingWaveform() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch(Dispatchers.Default) {
            var phase = 0f
            while (isActive) {
                // Generates natural, lively wave movements corresponding to speech syllables
                val base = (sin(phase) + 1f) / 2f
                val jitter = (sin(phase * 2.7f) + 1f) / 4f
                val amp = (base * 0.6f + jitter * 0.4f).coerceIn(0.15f, 0.95f)
                onAmplitudeChanged(amp)
                phase += 0.28f
                delay(40)
            }
        }
    }

    private fun stopSimulatingSpeakingWaveform() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        onAmplitudeChanged(0f)
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
