package info.dvkr.screenstream.common.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import info.dvkr.screenstream.R
import info.dvkr.screenstream.SingleActivity
import info.dvkr.screenstream.common.module.StreamingModuleService

class NotificationHelperImpl(private val context: Context) {

    fun buildStreamingNotification(moduleName: String, stopAction: Intent): Notification {
        val openAppPi = PendingIntent.getActivity(
            context, 0,
            Intent(context, SingleActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopPi = PendingIntent.getService(
            context, 1, stopAction,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, StreamingModuleService.CHANNEL_STREAMING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ScreenStream — $moduleName")
            .setContentText("Streaming active")
            .setContentIntent(openAppPi)
            .addAction(R.drawable.ic_stop, "Stop", stopPi)
            .setOngoing(true)
            .build()
    }

    fun postError(channelId: String, notifId: Int, message: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val notif = NotificationCompat.Builder(context, StreamingModuleService.CHANNEL_ERROR)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ScreenStream Error")
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        nm.notify(notifId, notif)
    }
}
