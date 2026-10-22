package info.dvkr.screenstream.webrtc.internal

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import info.dvkr.screenstream.webrtc.settings.WebRtcSettings
import info.dvkr.screenstream.webrtc.ui.WebRtcError
import info.dvkr.screenstream.webrtc.ui.WebRtcState
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.webrtc.*

class WebRtcStreamingService(
    private val context: Context,
    private val webRtcSettings: WebRtcSettings,
    private val environment: WebRtcEnvironment
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(WebRtcState())
    val stateFlow: StateFlow<WebRtcState> = _state.asStateFlow()

    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var signalingSocket: Socket? = null
    private val peerConnections = mutableMapOf<String, PeerConnection>()

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    fun start(mediaProjection: MediaProjection, projectionCode: Int, projectionData: Intent) {
        scope.launch {
            try {
                initWebRtc(mediaProjection, projectionCode, projectionData)
                connectSignaling()
                _state.update { it.copy(isStreaming = true, error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = WebRtcError.Other(e)) }
            }
        }
    }

    private fun initWebRtc(
        mediaProjection: MediaProjection,
        projectionCode: Int,
        projectionData: Intent
    ) {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        eglBase = EglBase.create()

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase!!.eglBaseContext, true, true
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase!!.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        val factory = peerConnectionFactory!!
        val settings = webRtcSettings.cachedData()

        // Screen capture via ScreenCapturerAndroid (webrtc-sdk provides this)
        surfaceTextureHelper = SurfaceTextureHelper.create(
            "ScreenCaptureThread", eglBase!!.eglBaseContext
        )

        val capturer = ScreenCapturerAndroid(projectionData, object : MediaProjection.Callback() {
            override fun onStop() {
                stop("MediaProjection stopped")
            }
        })

        videoSource = factory.createVideoSource(capturer.isScreencast)
        capturer.initialize(surfaceTextureHelper, context, videoSource!!.capturerObserver)
        capturer.startCapture(settings.videoWidth, settings.videoHeight, settings.fps)

        localVideoTrack = factory.createVideoTrack("video0", videoSource)

        // Optional mic audio
        if (settings.enableMic) {
            audioSource = factory.createAudioSource(MediaConstraints())
            localAudioTrack = factory.createAudioTrack("audio0", audioSource)
        }
    }

    private fun connectSignaling() {
        scope.launch {
            val nonce = fetchNonce()
            if (nonce == null) {
                _state.update { it.copy(error = WebRtcError.SignalingFailed) }
                return@launch
            }

            val opts = IO.Options.builder()
                .setPath(environment.socketPath)
                .setAuth(mapOf("nonce" to nonce, "package" to environment.packageName))
                .build()

            signalingSocket = IO.socket(environment.signalingServerUrl, opts).apply {
                on(Socket.EVENT_CONNECT) {
                    _state.update { it.copy(signalingConnected = true) }
                }
                on(Socket.EVENT_DISCONNECT) {
                    _state.update { it.copy(signalingConnected = false) }
                }
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    _state.update { it.copy(error = WebRtcError.SignalingFailed) }
                }
                on("clientConnected") { args ->
                    val clientId = (args[0] as JSONObject).getString("id")
                    scope.launch(Dispatchers.Main) { createPeerConnection(clientId) }
                }
                on("clientDisconnected") { args ->
                    val clientId = (args[0] as JSONObject).getString("id")
                    removePeerConnection(clientId)
                }
                on("answer") { args ->
                    val data = args[0] as JSONObject
                    val clientId = data.getString("id")
                    val sdp = data.getString("sdp")
                    peerConnections[clientId]?.setRemoteDescription(
                        SimpleSdpObserver(),
                        SessionDescription(SessionDescription.Type.ANSWER, sdp)
                    )
                }
                on("iceCandidate") { args ->
                    val data = args[0] as JSONObject
                    val clientId = data.getString("id")
                    peerConnections[clientId]?.addIceCandidate(
                        IceCandidate(
                            data.getString("sdpMid"),
                            data.getInt("sdpMLineIndex"),
                            data.getString("candidate")
                        )
                    )
                }
                connect()
            }
        }
    }

    private suspend fun fetchNonce(): String? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val req = Request.Builder()
                .url(environment.signalingServerNonceUrl)
                .post(ByteArray(0).toRequestBody(null))
                .build()
            client.newCall(req).execute().use { resp -> resp.body?.string() }
        } catch (e: Exception) { null }
    }

    private fun createPeerConnection(clientId: String) {
        val factory = peerConnectionFactory ?: return
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val pc = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                val json = JSONObject()
                    .put("id", clientId)
                    .put("sdpMid", candidate.sdpMid)
                    .put("sdpMLineIndex", candidate.sdpMLineIndex)
                    .put("candidate", candidate.sdp)
                signalingSocket?.emit("iceCandidate", json)
            }
            override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
                val connected = state == PeerConnection.PeerConnectionState.CONNECTED
                _state.update { s ->
                    s.copy(clients = s.clients.map { c ->
                        if (c.id == clientId) c.copy(connected = connected) else c
                    })
                }
            }
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        }) ?: return

        // Add tracks (Unified Plan)
        localVideoTrack?.let { pc.addTrack(it, listOf("stream0")) }
        localAudioTrack?.let { pc.addTrack(it, listOf("stream0")) }

        peerConnections[clientId] = pc
        _state.update { s ->
            s.copy(clients = s.clients + WebRtcState.Client(clientId, connected = false))
        }

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
        }
        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                pc.setLocalDescription(SimpleSdpObserver(), sdp)
                signalingSocket?.emit("offer", JSONObject()
                    .put("id", clientId)
                    .put("sdp", sdp.description)
                )
            }
        }, constraints)
    }

    private fun removePeerConnection(clientId: String) {
        peerConnections.remove(clientId)?.close()
        _state.update { s -> s.copy(clients = s.clients.filterNot { it.id == clientId }) }
    }

    fun stop(reason: String) {
        peerConnections.values.forEach { it.close() }
        peerConnections.clear()
        signalingSocket?.disconnect()
        signalingSocket = null
        runCatching { surfaceTextureHelper?.dispose() }
        runCatching { videoSource?.dispose() }
        runCatching { audioSource?.dispose() }
        runCatching { peerConnectionFactory?.dispose() }
        runCatching { eglBase?.release() }
        peerConnectionFactory = null
        eglBase = null
        _state.update { it.copy(isStreaming = false, signalingConnected = false, clients = emptyList()) }
    }

    fun clearError() { _state.update { it.copy(error = null) } }

    fun destroy() { stop("Destroy"); scope.cancel() }

    open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}
