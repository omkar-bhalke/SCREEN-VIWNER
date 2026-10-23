package com.example.audio

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Android Service hosting the real-time AudioRecord microphone pipeline.
 * Captures 16kHz 16-bit Mono PCM audio chunks for the AHI voice stream
 * and calculates real-time RMS amplitude for UI audio waveforms and ribbon orb.
 */
class AudioRecordService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): AudioRecordService = this@AudioRecordService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_START -> startRecordingInternal()
            ACTION_STOP -> stopRecordingInternal()
        }
        return START_NOT_STICKY
    }

    private fun startRecordingInternal() {
        if (_isRecordingFlow.value) {
            Log.d(TAG, "AudioRecordService is already recording")
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Cannot start AudioRecord: RECORD_AUDIO permission not granted")
            serviceScope.launch {
                _errorFlow.emit("RECORD_AUDIO permission required")
            }
            stopSelf()
            return
        }

        // Enter Foreground service state to ensure unbroken streaming
        try {
            val notification = buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Foreground notification start error: ${e.message}")
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferSize = (minBufferSize * 2).coerceAtLeast(CHUNK_SIZE_SHORTS * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to DEFAULT/MIC source
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                serviceScope.launch {
                    _errorFlow.emit("AudioRecord initialization failed")
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            audioRecord?.startRecording()
            _isRecordingFlow.value = true

            recordingJob = serviceScope.launch {
                val shortBuffer = ShortArray(CHUNK_SIZE_SHORTS)
                val byteBuffer = ByteBuffer.allocate(CHUNK_SIZE_SHORTS * 2).order(ByteOrder.LITTLE_ENDIAN)

                while (isActive && _isRecordingFlow.value) {
                    val readResult = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: -1
                    if (readResult > 0) {
                        // 1. Calculate RMS / Amplitude
                        var sum = 0.0
                        for (i in 0 until readResult) {
                            val sample = shortBuffer[i].toDouble()
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / readResult)
                        // Normalized amplitude 0.0f .. 1.0f
                        val normalized = (rms / 28000.0).coerceIn(0.0, 1.0).toFloat()
                        _amplitudeFlow.value = normalized

                        // 2. Convert ShortArray to ByteArray for downstream PCM voice streaming
                        byteBuffer.clear()
                        for (i in 0 until readResult) {
                            byteBuffer.putShort(shortBuffer[i])
                        }
                        val chunkBytes = byteBuffer.array().copyOf(readResult * 2)
                        _audioChunkFlow.emit(chunkBytes)
                    } else if (readResult == AudioRecord.ERROR_INVALID_OPERATION || readResult == AudioRecord.ERROR_BAD_VALUE) {
                        Log.e(TAG, "AudioRecord read error: $readResult")
                        break
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception starting AudioRecord: ${e.message}", e)
            serviceScope.launch {
                _errorFlow.emit(e.message ?: "Audio capture error")
            }
            stopRecordingInternal()
        }
    }

    private fun stopRecordingInternal() {
        _isRecordingFlow.value = false
        _amplitudeFlow.value = 0f
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping audioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping foreground: ${e.message}")
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AHI Voice Stream Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Streams real-time microphone audio for AHI voice assistant"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AHI Live Voice")
            .setContentText("Microphone streaming active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecordingInternal()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "AudioRecordService"
        private const val CHANNEL_ID = "ahi_audio_record_channel"
        private const val NOTIFICATION_ID = 4040

        const val SAMPLE_RATE = 16000
        const val CHUNK_SIZE_SHORTS = 1024 // 64ms chunks at 16kHz

        const val ACTION_START = "com.example.audio.action.START_RECORDING"
        const val ACTION_STOP = "com.example.audio.action.STOP_RECORDING"

        private val _audioChunkFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
        val audioChunkFlow: SharedFlow<ByteArray> = _audioChunkFlow.asSharedFlow()

        private val _amplitudeFlow = MutableStateFlow(0f)
        val amplitudeFlow: StateFlow<Float> = _amplitudeFlow.asStateFlow()

        private val _isRecordingFlow = MutableStateFlow(false)
        val isRecordingFlow: StateFlow<Boolean> = _isRecordingFlow.asStateFlow()

        private val _errorFlow = MutableSharedFlow<String>(extraBufferCapacity = 8)
        val errorFlow: SharedFlow<String> = _errorFlow.asSharedFlow()

        fun start(context: Context) {
            val intent = Intent(context, AudioRecordService::class.java).apply {
                action = ACTION_START
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.w(TAG, "startForegroundService exception: ${e.message}, falling back to startService")
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AudioRecordService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
