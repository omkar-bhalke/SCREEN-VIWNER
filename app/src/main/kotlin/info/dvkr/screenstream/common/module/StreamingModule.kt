package info.dvkr.screenstream.common.module

import android.content.Context
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.module.Module

interface StreamingModule {

    enum class Id { MJPEG, RTSP, WEBRTC }

    enum class WindowWidthSizeClass { COMPACT, MEDIUM, EXPANDED }

    fun getId(): Id
    fun getNameResource(): Int
    fun getDescriptionResource(): Int
    fun getDetailsResource(): Int
    fun getPriority(): Int
    fun getRequiresLocalNetworkPermission(): Boolean
    fun isStreaming(): StateFlow<Boolean>
    fun getHasActiveConsumer(): StateFlow<Boolean>

    fun getKoinModule(): Module
    fun startModule(context: Context)
    suspend fun stopModule(reason: String?)
    fun stopStream(reason: String)
    fun recoverError()

    @Composable
    fun StreamUIContent(
        windowSizeClass: WindowWidthSizeClass,
        modifier: androidx.compose.ui.Modifier,
        onBackPressed: () -> Unit
    )
}
