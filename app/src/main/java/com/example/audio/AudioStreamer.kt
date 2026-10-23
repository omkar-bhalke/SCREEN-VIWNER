package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class AudioStreamer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onErrorState: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var silenceWatchdogJob: Job? = null

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("AudioStreamer", "Speech recognition not available on this device")
            onErrorState("Speech recognition service unavailable")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        } catch (e: Exception) {
            Log.e("AudioStreamer", "Error creating speech recognizer: ${e.message}")
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("AudioStreamer", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("AudioStreamer", "Beginning of speech detected")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Normalize rmsdB (-2 to 10 dB typically) to 0.0f - 1.0f
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                onRmsChanged(normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("AudioStreamer", "End of speech detected")
                onRmsChanged(0f)
            }

            override fun onError(error: Int) {
                isListening = false
                onRmsChanged(0f)
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client recognition error"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    else -> "Recognition issue (code $error)"
                }
                Log.w("AudioStreamer", "Recognition error: $error ($message)")
                onErrorState(message)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                onRmsChanged(0f)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val speechText = matches?.firstOrNull()?.trim() ?: ""
                if (speechText.isNotBlank()) {
                    onFinalResult(speechText)
                } else {
                    onErrorState("No speech detected")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()?.trim() ?: ""
                if (partial.isNotBlank()) {
                    onPartialResult(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            initRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Enable continuous mode parameters if supported
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            Log.e("AudioStreamer", "Failed to start listening: ${e.message}")
            onErrorState(e.message ?: "Failed to start mic")
        }
    }

    fun stopListening() {
        silenceWatchdogJob?.cancel()
        onRmsChanged(0f)
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w("AudioStreamer", "Error stopping listening: ${e.message}")
            }
            isListening = false
        }
    }

    fun cancel() {
        silenceWatchdogJob?.cancel()
        onRmsChanged(0f)
        isListening = false
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w("AudioStreamer", "Error canceling recognizer: ${e.message}")
        }
    }

    fun release() {
        cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
