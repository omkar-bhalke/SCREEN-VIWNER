package info.dvkr.screenstream.webrtc.ui

data class WebRtcState(
    val isStreaming: Boolean = false,
    val signalingConnected: Boolean = false,
    val streamId: String = "",
    val viewerUrl: String = "",
    val clients: List<Client> = emptyList(),
    val error: WebRtcError? = null
) {
    data class Client(val id: String, val connected: Boolean)
}

sealed class WebRtcError {
    object SignalingFailed : WebRtcError()
    object CastPermissionsDenied : WebRtcError()
    data class Other(val throwable: Throwable) : WebRtcError()
}

sealed class WebRtcUiEvent {
    object StartStopStream : WebRtcUiEvent()
    object RecoverError : WebRtcUiEvent()
    object CopyViewerUrl : WebRtcUiEvent()
}
