package info.dvkr.screenstream

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import info.dvkr.screenstream.common.module.StreamingModule
import info.dvkr.screenstream.mjpeg.MjpegStreamingModule
import info.dvkr.screenstream.rtsp.RtspStreamingModule
import info.dvkr.screenstream.webrtc.WebRtcStreamingModule
import timber.log.Timber

class ScreenStreamApp : BaseApp() {

    override fun getStreamingModules(): Array<StreamingModule> = arrayOf(
        MjpegStreamingModule(),
        RtspStreamingModule(),
        WebRtcStreamingModule()
    )

    override fun configureReleaseLogger(tree: Timber.Tree) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(true)
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (priority >= Log.WARN) {
                    crashlytics.log("[$tag] $message")
                    if (t != null) crashlytics.recordException(t)
                }
            }
        })
    }
}
