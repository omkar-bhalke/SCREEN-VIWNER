package info.dvkr.screenstream.rtsp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.dvkr.screenstream.rtsp.internal.RtspStreamingService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RtspViewModel(private val service: RtspStreamingService) : ViewModel() {

    val stateFlow: StateFlow<RtspState> = service.stateFlow

    fun onEvent(event: RtspUiEvent) {
        viewModelScope.launch {
            when (event) {
                RtspUiEvent.StartStopStream ->
                    if (stateFlow.value.isStreaming) service.stop("User request")
                RtspUiEvent.RecoverError -> service.clearError()
            }
        }
    }
}
