package info.dvkr.screenstream.mjpeg.ui

import info.dvkr.screenstream.mjpeg.internal.MjpegNetInterface

data class MjpegState(
    val isStreaming: Boolean = false,
    val isBusy: Boolean = false,
    val waitingCastPermission: Boolean = false,
    val requiresPin: Boolean = false,
    val pin: Pin = Pin.NotRequired,
    val serverNetInterfaces: List<MjpegNetInterface> = emptyList(),
    val clients: List<Client> = emptyList(),
    val error: MjpegError? = null
) {
    sealed class Pin {
        object NotRequired : Pin()
        data class Required(
            val pin: String,
            val hiddenOnStart: Boolean
        ) : Pin()
    }

    data class Client(
        val id: Long,
        val remoteAddress: String,
        val state: State,
        val transferBytes: Long,
        val transferSpeed: Long
    ) {
        enum class State { CONNECTED, BLOCKED }
    }
}
