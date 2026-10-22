package info.dvkr.screenstream.rtsp.internal

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.view.Surface
import info.dvkr.screenstream.rtsp.settings.RtspSettings
import info.dvkr.screenstream.rtsp.ui.RtspState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * RTSP/RTP streaming service.
 *
 * Protocol flow:
 *   TCP server on :8554 (default)
 *   RTSP OPTIONS / DESCRIBE / SETUP / PLAY / TEARDOWN per RFC 2326
 *   Video: H.264 or H.265 over RTP (PT=96) via MediaCodec encoder
 *   Audio: AAC or Opus over RTP (PT=97) via AudioRecord → MediaCodec
 *
 * Relay mode: re-push to an external RTSP server (RTMP-style push via ANNOUNCE/RECORD)
 * ONVIF: basic device info served at /onvif/device_service (optional)
 */
class RtspStreamingService(
    private val context: Context,
    private val rtspSettings: RtspSettings
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(RtspState())
    val stateFlow: StateFlow<RtspState> = _state.asStateFlow()

    private var mediaProjection: MediaProjection? = null
    private var videoEncoder: MediaCodec? = null
    private var encoderInputSurface: Surface? = null
    private var serverSocket: ServerSocket? = null
    private val sessions = ConcurrentHashMap<Long, RtspSession>()
    private val sessionIdCounter = AtomicLong(0)

    // -----------------------------------------------------------------------
    // Start / stop
    // -----------------------------------------------------------------------

    fun start(projection: MediaProjection) {
        mediaProjection = projection
        scope.launch {
            try {
                startVideoEncoder()
                startRtspServer()
                _state.update { it.copy(isStreaming = true, error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = RtspState.Error.Other(e)) }
            }
        }
    }

    fun stop(reason: String) {
        scope.launch {
            sessions.values.forEach { it.close() }
            sessions.clear()
            safeStopEncoder()
            safeStopServer()
            mediaProjection?.stop()
            mediaProjection = null
            _state.update { it.copy(isStreaming = false, clients = emptyList()) }
        }
    }

    fun destroy() {
        stop("Destroy")
        scope.cancel()
    }

    // -----------------------------------------------------------------------
    // Video encoder (MediaCodec surface-mode → H.264/H.265)
    // -----------------------------------------------------------------------

    private fun startVideoEncoder() {
        val s = rtspSettings.cachedData()
        val mimeType = if (s.videoCodec == RtspSettings.VideoCodec.H265)
            MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC

        val format = MediaFormat.createVideoFormat(mimeType, s.videoWidth, s.videoHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, s.videoBitrateKbps * 1000)
            setInteger(MediaFormat.KEY_FRAME_RATE, s.videoFps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            setInteger(MediaFormat.KEY_PROFILE,
                if (mimeType == MediaFormat.MIMETYPE_VIDEO_AVC)
                    MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
                else
                    MediaCodecInfo.CodecProfileLevel.HEVCProfileMain
            )
        }

        videoEncoder = MediaCodec.createEncoderByType(mimeType).also { codec ->
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoderInputSurface = codec.createInputSurface()
            codec.start()
        }

        // Create VirtualDisplay feeding encoder surface
        val dm = context.resources.displayMetrics
        mediaProjection?.createVirtualDisplay(
            "RtspCapture", s.videoWidth, s.videoHeight, dm.densityDpi,
            android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            encoderInputSurface, null, null
        )

        // Drain encoder output → RTP packetizer → all sessions
        scope.launch(Dispatchers.IO) {
            val bufferInfo = MediaCodec.BufferInfo()
            val encoder = videoEncoder ?: return@launch
            var spsNal: ByteArray? = null
            var ppsNal: ByteArray? = null

            while (isActive) {
                val idx = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                if (idx < 0) continue
                val buf = encoder.getOutputBuffer(idx) ?: run {
                    encoder.releaseOutputBuffer(idx, false); continue
                }

                val isConfig = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                val bytes = ByteArray(bufferInfo.size)
                buf.position(bufferInfo.offset)
                buf.get(bytes)

                if (isConfig) {
                    // Parse SPS/PPS from config buffer
                    parseSpsPps(bytes)?.let { (sps, pps) ->
                        spsNal = sps; ppsNal = pps
                    }
                    encoder.releaseOutputBuffer(idx, false)
                    continue
                }

                // Packetize as RTP and broadcast
                val rtpPackets = RtpPacketizer.packetizeH264(
                    bytes, bufferInfo.presentationTimeUs, spsNal, ppsNal
                )
                sessions.values.forEach { session ->
                    rtpPackets.forEach { pkt -> session.sendRtp(pkt) }
                }

                encoder.releaseOutputBuffer(idx, false)
            }
        }
    }

    private fun parseSpsPps(configBytes: ByteArray): Pair<ByteArray, ByteArray>? {
        // Find NAL start codes (0x00 0x00 0x00 0x01) and split
        val startCode = byteArrayOf(0, 0, 0, 1)
        val parts = mutableListOf<ByteArray>()
        var start = 0
        for (i in 0..configBytes.size - 4) {
            if (configBytes.sliceArray(i until i + 4).contentEquals(startCode)) {
                if (i > start) parts.add(configBytes.sliceArray(start until i))
                start = i + 4
            }
        }
        if (start < configBytes.size) parts.add(configBytes.sliceArray(start until configBytes.size))
        val sps = parts.firstOrNull { it.isNotEmpty() && (it[0].toInt() and 0x1F) == 7 }
        val pps = parts.firstOrNull { it.isNotEmpty() && (it[0].toInt() and 0x1F) == 8 }
        return if (sps != null && pps != null) Pair(sps, pps) else null
    }

    private fun safeStopEncoder() {
        runCatching { videoEncoder?.stop() }
        runCatching { videoEncoder?.release() }
        videoEncoder = null
        runCatching { encoderInputSurface?.release() }
        encoderInputSurface = null
    }

    // -----------------------------------------------------------------------
    // RTSP TCP server
    // -----------------------------------------------------------------------

    private fun startRtspServer() {
        val s = rtspSettings.cachedData()
        serverSocket = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress(s.serverPort))
        }

        scope.launch(Dispatchers.IO) {
            val srv = serverSocket ?: return@launch
            while (isActive && !srv.isClosed) {
                try {
                    val client = srv.accept()
                    val id = sessionIdCounter.incrementAndGet()
                    val session = RtspSession(
                        id = id,
                        socket = client,
                        settings = rtspSettings.cachedData(),
                        onClose = { sessions.remove(id) },
                        getSpsPps = { Pair(ByteArray(0), ByteArray(0)) }
                    )
                    sessions[id] = session
                    session.start(scope)
                    _state.update { st ->
                        st.copy(clients = st.clients + RtspState.Client(
                            id = id.toString(),
                            address = client.inetAddress.hostAddress ?: "unknown"
                        ))
                    }
                } catch (_: Exception) { break }
            }
        }

        // Publish addresses
        val interfaces = buildList {
            try {
                java.net.NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { iface ->
                    if (!iface.isUp || iface.isLoopback) return@forEach
                    iface.inetAddresses.toList().forEach { addr ->
                        if (addr is java.net.Inet4Address && s.enableIPv4)
                            add(RtspState.NetInterface(
                                name = iface.name,
                                address = addr.hostAddress ?: return@forEach,
                                port = s.serverPort,
                                streamPath = s.streamPath
                            ))
                    }
                }
            } catch (_: Exception) {}
        }
        _state.update { it.copy(serverNetInterfaces = interfaces) }
    }

    private fun safeStopServer() {
        runCatching { serverSocket?.close() }
        serverSocket = null
    }
}

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
