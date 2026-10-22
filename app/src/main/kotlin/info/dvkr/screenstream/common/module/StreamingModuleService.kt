package info.dvkr.screenstream.common.module

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import info.dvkr.screenstream.R

abstract class StreamingModuleService : Service() {

    companion object {
        const val CHANNEL_STREAMING = "ScreenStream_Streaming"
        const val CHANNEL_ERROR     = "ScreenStream_Error"
    }

    abstract fun getNotificationIdForeground(): Int
    abstract fun getNotificationIdError(): Int

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(getNotificationIdForeground(), buildForegroundNotification())
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_STREAMING, "Streaming", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ERROR, "Errors", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun buildForegroundNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_STREAMING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ScreenStream")
            .setContentText("Streaming active")
            .setOngoing(true)
            .build()
}
