package info.dvkr.screenstream.mjpeg

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import info.dvkr.screenstream.R
import info.dvkr.screenstream.common.module.StreamingModule
import info.dvkr.screenstream.mjpeg.ui.MjpegMainScreenUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.module.Module

class MjpegStreamingModule : StreamingModule {

    private val _isStreaming = MutableStateFlow(false)
    private val _hasActiveConsumer = MutableStateFlow(false)

    override fun getId() = StreamingModule.Id.MJPEG
    override fun getNameResource() = R.string.mjpeg_module_name
    override fun getDescriptionResource() = R.string.mjpeg_module_description
    override fun getDetailsResource() = R.string.mjpeg_module_details
    override fun getPriority() = 0
    override fun getRequiresLocalNetworkPermission() = true
    override fun isStreaming(): StateFlow<Boolean> = _isStreaming
    override fun getHasActiveConsumer(): StateFlow<Boolean> = _hasActiveConsumer

    override fun getKoinModule(): Module = MjpegKoinModule

    override fun startModule(context: Context) {
        context.startForegroundService(
            Intent(context, MjpegModuleService::class.java)
                .setAction(MjpegModuleService.ACTION_START)
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
        MjpegMainScreenUI(windowSizeClass = windowSizeClass, modifier = modifier)
    }
}
