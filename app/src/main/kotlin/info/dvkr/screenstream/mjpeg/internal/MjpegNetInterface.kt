package info.dvkr.screenstream.mjpeg.internal

data class MjpegNetInterface(
    val name: String,
    val address: String,
    val port: Int,
    val sslEnabled: Boolean = false
) {
    fun getUrl(): String {
        val scheme = if (sslEnabled) "https" else "http"
        val addr = if (address.contains(":")) "[$address]" else address
        return "$scheme://$addr:$port"
    }
}
