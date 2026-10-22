package info.dvkr.screenstream.mjpeg.internal

import android.content.Context
import android.graphics.Bitmap
import android.media.projection.MediaProjection
import android.os.PowerManager
import info.dvkr.screenstream.mjpeg.settings.MjpegSettings
import info.dvkr.screenstream.mjpeg.ui.MjpegError
import info.dvkr.screenstream.mjpeg.ui.MjpegState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.io.ByteArrayOutputStream

class MjpegStreamingService(
    private val context: Context,
    private val mjpegSettings: MjpegSettings
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _mutableState = MutableStateFlow(MjpegState())
    val stateFlow: StateFlow<MjpegState> = _mutableState.asStateFlow()

    private val bitmapStateFlow = MutableStateFlow<Bitmap?>(null)
    private val mjpegSharedFlow = MutableSharedFlow<ByteArray>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var bitmapCapture: BitmapCapture? = null
    private var mediaProjection: MediaProjection? = null
    private var wakeLock: PowerManager.WakeLock? = null

    val httpServer: HttpServer by lazy {
        HttpServer(context = context, mjpegSettings = mjpegSettings,
            mjpegSharedFlow = mjpegSharedFlow, sendEvent = ::sendEvent)
    }

    init {
        coroutineScope.launch {
            bitmapStateFlow.filterNotNull().collect { bitmap ->
                val settings = mjpegSettings.cachedData()
                val output = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, settings.jpegQuality, output)
                mjpegSharedFlow.emit(output.toByteArray())
            }
        }
    }

    fun start() {
        coroutineScope.launch {
            try {
                val interfaces = NetworkHelper(context).getNetInterfaces(0, 0, true, false)
                httpServer.start(interfaces, coroutineScope)
                _mutableState.update { it.copy(serverNetInterfaces = interfaces, waitingCastPermission = true) }
            } catch (e: Exception) {
                _mutableState.update { it.copy(error = MjpegError.from(e)) }
            }
        }
    }

    fun sendEvent(event: MjpegEvent, delay: Long = 0L) {
        coroutineScope.launch {
            if (delay > 0) delay(delay)
            processEvent(event)
        }
    }

    private suspend fun processEvent(event: MjpegEvent) {
        when (event) {
            is MjpegEvent.StartProjection -> handleStartProjection(event)
            is MjpegEvent.StartServer -> {
                val interfaces = NetworkHelper(context).getNetInterfaces(0, 0, true, false)
                httpServer.start(interfaces, coroutineScope)
                _mutableState.update { it.copy(serverNetInterfaces = interfaces, waitingCastPermission = false) }
            }
            is MjpegEvent.StopStream -> handleStopStream(event.reason)
            is MjpegEvent.RecoverError -> _mutableState.update { it.copy(error = null) }
            is MjpegEvent.SettingsChanged -> httpServer.serverData.configure(mjpegSettings.cachedData())
            is MjpegEvent.ClientConnected ->
                _mutableState.update { it.copy(clients = httpServer.serverData.getClientList()) }
            is MjpegEvent.ClientDisconnected ->
                _mutableState.update { it.copy(clients = httpServer.serverData.getClientList()) }
            is MjpegEvent.SetPin -> {
                /* update settings */ }
            MjpegEvent.RandomPin -> { /* generate random pin */ }
        }
    }

    private fun handleStartProjection(event: MjpegEvent.StartProjection) {
        mediaProjection = event.mediaProjection
        val dm = context.resources.displayMetrics
        bitmapCapture = BitmapCapture(
            context = context, mjpegSettings = mjpegSettings,
            mediaProjection = mediaProjection!!,
            bitmapStateFlow = bitmapStateFlow,
            onError = { e -> _mutableState.update { it.copy(error = MjpegError.from(e)) } }
        )
        val started = bitmapCapture!!.start(android.util.Size(dm.widthPixels, dm.heightPixels))
        if (started) {
            acquireWakeLock()
            _mutableState.update { it.copy(isStreaming = true, error = null, waitingCastPermission = false) }
        }
    }

    private fun handleStopStream(reason: String) {
        bitmapCapture?.destroy(); bitmapCapture = null
        mediaProjection?.stop(); mediaProjection = null
        releaseWakeLock()
        _mutableState.update { it.copy(isStreaming = false) }
    }

    private fun acquireWakeLock() {
        if (mjpegSettings.cachedData().keepAwake != true) return
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ScreenStream::MjpegWakeLock")
        wakeLock?.acquire(10 * 60 * 1000L)
    }
    private fun releaseWakeLock() {
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
    }

    fun destroyService() {
        handleStopStream("Destroy")
        coroutineScope.launch { httpServer.destroy() }
        coroutineScope.cancel()
    }
}
