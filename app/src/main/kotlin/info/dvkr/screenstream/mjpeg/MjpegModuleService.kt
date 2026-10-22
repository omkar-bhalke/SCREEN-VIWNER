package info.dvkr.screenstream.mjpeg

import android.content.Intent
import info.dvkr.screenstream.common.module.StreamingModuleService
import info.dvkr.screenstream.mjpeg.internal.MjpegStreamingService
import org.koin.android.ext.android.inject

class MjpegModuleService : StreamingModuleService() {

    companion object {
        const val ACTION_START = "info.dvkr.screenstream.mjpeg.ACTION_START"
        const val ACTION_STOP  = "info.dvkr.screenstream.mjpeg.ACTION_STOP"
        const val NOTIFICATION_ID_FOREGROUND = 100
        const val NOTIFICATION_ID_ERROR = 101
    }

    private val mjpegStreamingService: MjpegStreamingService by inject()

    override fun getNotificationIdForeground() = NOTIFICATION_ID_FOREGROUND
    override fun getNotificationIdError() = NOTIFICATION_ID_ERROR

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> mjpegStreamingService.start()
            ACTION_STOP  -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mjpegStreamingService.destroyService()
        super.onDestroy()
    }
}
