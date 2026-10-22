package info.dvkr.screenstream.mjpeg.internal

import android.media.projection.MediaProjection

sealed class MjpegEvent {
    data class StartProjection(val mediaProjection: MediaProjection) : MjpegEvent()
    object StartServer : MjpegEvent()
    data class StopStream(val reason: String) : MjpegEvent()
    object RecoverError : MjpegEvent()
    object SettingsChanged : MjpegEvent()
    data class ClientConnected(val clientId: Long, val address: String) : MjpegEvent()
    data class ClientDisconnected(val clientId: Long) : MjpegEvent()
    data class SetPin(val pin: String) : MjpegEvent()
    object RandomPin : MjpegEvent()
}
