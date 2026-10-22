package info.dvkr.screenstream.webrtc

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import info.dvkr.screenstream.R
import info.dvkr.screenstream.common.module.StreamingModule
import info.dvkr.screenstream.webrtc.ui.WebRtcMainScreenUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.module.Module

class WebRtcStreamingModule : StreamingModule {

    private val _isStreaming = MutableStateFlow(false)
    private val _hasActiveConsumer = MutableStateFlow(false)

    override fun getId() = StreamingModule.Id.WEBRTC
    override fun getNameResource() = R.string.webrtc_module_name
    override fun getDescriptionResource() = R.string.webrtc_module_description
    override fun getDetailsResource() = R.string.webrtc_module_details
    override fun getPriority() = 2
    override fun getRequiresLocalNetworkPermission() = false
    override fun isStreaming(): StateFlow<Boolean> = _isStreaming
    override fun getHasActiveConsumer(): StateFlow<Boolean> = _hasActiveConsumer

    override fun getKoinModule(): Module = WebRtcKoinModule

    override fun startModule(context: Context) {
        context.startForegroundService(
            Intent(context, WebRtcModuleService::class.java)
                .setAction(WebRtcModuleService.ACTION_START)
        )
    }

    override suspend fun stopModule(reason: String?) {}
    override fun stopStream(reason: String) {}
    override fun recoverError() {}

    @Composable
    override fun StreamUIContent(
        windowSizeClass: StreamingModule.WindowWidthSizeClass,
        modifier: Modifier,
        onBackPressed: () -> Unit
    ) {
        WebRtcMainScreenUI(windowSizeClass = windowSizeClass, modifier = modifier)
    }
}
