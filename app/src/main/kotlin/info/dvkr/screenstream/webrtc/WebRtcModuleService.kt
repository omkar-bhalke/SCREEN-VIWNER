package info.dvkr.screenstream.webrtc

import android.content.Intent
import info.dvkr.screenstream.common.module.StreamingModuleService
import info.dvkr.screenstream.webrtc.internal.WebRtcStreamingService
import org.koin.android.ext.android.inject

class WebRtcModuleService : StreamingModuleService() {

    companion object {
        const val ACTION_START = "info.dvkr.screenstream.webrtc.ACTION_START"
        const val ACTION_STOP  = "info.dvkr.screenstream.webrtc.ACTION_STOP"
        const val NOTIFICATION_ID_FOREGROUND = 300
        const val NOTIFICATION_ID_ERROR = 301
    }

    private val webRtcStreamingService: WebRtcStreamingService by inject()

    override fun getNotificationIdForeground() = NOTIFICATION_ID_FOREGROUND
    override fun getNotificationIdError() = NOTIFICATION_ID_ERROR

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> { /* projection started via activity result */ }
            ACTION_STOP  -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        webRtcStreamingService.destroy()
        super.onDestroy()
    }
}
