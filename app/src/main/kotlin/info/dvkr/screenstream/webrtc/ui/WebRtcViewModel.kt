package info.dvkr.screenstream.webrtc.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.dvkr.screenstream.webrtc.internal.WebRtcStreamingService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WebRtcViewModel(private val service: WebRtcStreamingService) : ViewModel() {

    val stateFlow: StateFlow<WebRtcState> = service.stateFlow

    fun onEvent(event: WebRtcUiEvent) {
        viewModelScope.launch {
            when (event) {
                WebRtcUiEvent.StartStopStream ->
                    if (stateFlow.value.isStreaming) service.stop("User request")
                WebRtcUiEvent.RecoverError -> service.clearError()
                WebRtcUiEvent.CopyViewerUrl -> { /* clipboard handled in composable */ }
            }
        }
    }
}
