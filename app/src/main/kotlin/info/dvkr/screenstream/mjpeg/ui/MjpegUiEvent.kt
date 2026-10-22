package info.dvkr.screenstream.mjpeg.ui

sealed class MjpegUiEvent {
    object StartStopStream : MjpegUiEvent()
    object RecoverError : MjpegUiEvent()
    data class BlockClient(val clientId: Long) : MjpegUiEvent()
    data class UnblockClient(val clientId: Long) : MjpegUiEvent()
    object NewPin : MjpegUiEvent()
    data class SetPin(val pin: String) : MjpegUiEvent()
}
