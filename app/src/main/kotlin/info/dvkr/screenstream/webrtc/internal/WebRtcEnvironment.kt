package info.dvkr.screenstream.webrtc.internal

import android.content.Context

/**
 * WebRTC environment — contains all server/config constants extracted from APK:
 *   signalingServerUrl     = "https://screenstream.io"
 *   signalingServerNonceUrl= "https://screenstream.io/app/nonce"
 *   noncePath              = "/app/nonce"
 *   socketPath             = "/app/socket"
 *   cloudProjectNumber     = 68285893024L
 *   packageName            = context.packageName
 *   signalingServerHost    = derived from URL
 */
class WebRtcEnvironment(context: Context) {

    val packageName: String = context.applicationContext.packageName
    val cloudProjectNumber: Long = 68285893024L

    val signalingServerUrl: String = "https://screenstream.io"
    val noncePath: String = "/app/nonce"
    val socketPath: String = "/app/socket"
    val signalingServerNonceUrl: String = "https://screenstream.io/app/nonce"

    // Host extracted at construction via URL parsing (matches APK fn2/en2 URL builder)
    val signalingServerHost: String = run {
        val url = java.net.URL(signalingServerUrl)
        url.host
    }
}
