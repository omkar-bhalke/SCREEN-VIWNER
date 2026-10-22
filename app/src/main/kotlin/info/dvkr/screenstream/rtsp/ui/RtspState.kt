package info.dvkr.screenstream.rtsp.ui

data class RtspState(
    val isStreaming: Boolean = false,
    val serverNetInterfaces: List<NetInterface> = emptyList(),
    val clients: List<Client> = emptyList(),
    val error: Error? = null
) {
    data class NetInterface(val name: String, val address: String, val port: Int, val streamPath: String) {
        fun rtspUrl() = "rtsp://$address:$port$streamPath"
    }
    data class Client(val id: String, val address: String)
    sealed class Error {
        object CastPermissionsDenied : Error()
        data class PortInUse(val port: Int) : Error()
        data class Other(val throwable: Throwable) : Error()
    }
}

sealed class RtspUiEvent {
    object StartStopStream : RtspUiEvent()
    object RecoverError : RtspUiEvent()
}
