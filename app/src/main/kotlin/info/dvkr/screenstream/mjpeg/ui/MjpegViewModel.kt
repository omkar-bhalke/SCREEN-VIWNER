package info.dvkr.screenstream.mjpeg.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.dvkr.screenstream.mjpeg.internal.MjpegEvent
import info.dvkr.screenstream.mjpeg.internal.MjpegStreamingService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MjpegViewModel(
    private val mjpegStreamingService: MjpegStreamingService
) : ViewModel() {

    val stateFlow: StateFlow<MjpegState> = mjpegStreamingService.stateFlow

    fun onEvent(event: MjpegUiEvent) {
        viewModelScope.launch {
            when (event) {
                MjpegUiEvent.StartStopStream ->
                    if (stateFlow.value.isStreaming)
                        mjpegStreamingService.sendEvent(MjpegEvent.StopStream("User request"))
                    else
                        mjpegStreamingService.sendEvent(MjpegEvent.StartServer)
                MjpegUiEvent.RecoverError ->
                    mjpegStreamingService.sendEvent(MjpegEvent.RecoverError)
                is MjpegUiEvent.NewPin ->
                    mjpegStreamingService.sendEvent(MjpegEvent.RandomPin)
                is MjpegUiEvent.SetPin ->
                    mjpegStreamingService.sendEvent(MjpegEvent.SetPin(event.pin))
                is MjpegUiEvent.BlockClient, is MjpegUiEvent.UnblockClient -> { /* TODO */ }
            }
        }
    }
}
