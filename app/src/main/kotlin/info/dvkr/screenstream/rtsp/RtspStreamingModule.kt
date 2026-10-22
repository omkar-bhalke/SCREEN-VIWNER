package info.dvkr.screenstream.rtsp

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import info.dvkr.screenstream.common.module.StreamingModule
import info.dvkr.screenstream.rtsp.ui.RtspMainScreenUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.module.Module

class RtspStreamingModule : StreamingModule {

    private val _isStreaming = MutableStateFlow(false)
    private val _hasActiveConsumer = MutableStateFlow(false)

    override fun getId() = StreamingModule.Id.RTSP
    override fun getNameResource() = R.string.rtsp_module_name
    override fun getDescriptionResource() = R.string.rtsp_module_description
    override fun getDetailsResource() = R.string.rtsp_module_details
    override fun getPriority() = 1
    override fun getRequiresLocalNetworkPermission() = true
    override fun isStreaming(): StateFlow<Boolean> = _isStreaming
    override fun getHasActiveConsumer(): StateFlow<Boolean> = _hasActiveConsumer

    override fun getKoinModule(): Module = RtspKoinModule

    override fun startModule(context: Context) {
        context.startForegroundService(
            Intent(context, RtspModuleService::class.java)
                .setAction(RtspModuleService.ACTION_START)
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
        RtspMainScreenUI(windowSizeClass = windowSizeClass, modifier = modifier)
    }
}
