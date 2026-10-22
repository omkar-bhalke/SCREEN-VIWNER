package info.dvkr.screenstream.mjpeg.internal

import android.content.Context
import info.dvkr.screenstream.mjpeg.settings.MjpegSettings
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * HTTP server for MJPEG streaming.
 * Serves:
 *   GET /           → HTML viewer page (index.html from assets)
 *   GET /favicon.ico → favicon
 *   GET /logo.png   → logo
 *   GET /stream     → MJPEG multipart stream  (content-type: multipart/x-mixed-replace)
 *   GET /stream.mjpeg → alias for /stream
 *
 * PIN protection: optional per MjpegSettings. Clients must supply ?pin=XXXX query param.
 * Client tracking via HttpServerData.
 */
class HttpServer(
    private val context: Context,
    private val mjpegSettings: MjpegSettings,
    private val mjpegSharedFlow: SharedFlow<ByteArray>,
    private val sendEvent: (MjpegEvent) -> Unit
) {
    companion object {
        const val BOUNDARY = "mjpeg_boundary"
        const val PART_HEADER = "--$BOUNDARY\r\nContent-Type: image/jpeg\r\n\r\n"
        const val PART_FOOTER = "\r\n"
    }

    private val serverRef = AtomicReference<ApplicationEngine?>(null)
    val serverData = HttpServerData(sendEvent)

    private val baseIndexHtml: String by lazy {
        context.assets.open("index.html").bufferedReader().readText()
    }
    private val favicon: ByteArray by lazy {
        context.assets.open("favicon.ico").readBytes()
    }
    private val logoSvg: ByteArray by lazy {
        context.assets.open("logo.svg").readBytes()
    }

    suspend fun start(netInterfaces: List<MjpegNetInterface>, scope: CoroutineScope) {
        stop(false)
        val settings = mjpegSettings.cachedData()

        serverData.configure(settings)

        val port = settings.serverPort
        val server = embeddedServer(Netty, port = port) {
            install(io.ktor.server.plugins.defaultheaders.DefaultHeaders)
            routing {
                get("/") { serveIndex(call, settings) }
                get("/favicon.ico") { call.respondBytes(favicon, io.ktor.http.ContentType.Image.XIcon) }
                get("/logo.png") { call.respondBytes(logoSvg) }
                get("/logo.svg") { call.respondBytes(logoSvg) }
                get(serverData.getStreamAddress()) { serveStream(call, scope) }
                get(serverData.getJpegFallbackAddress()) { serveLatestJpeg(call) }
            }
        }
        server.start(wait = false)
        serverRef.set(server)
    }

    private suspend fun serveIndex(call: ApplicationCall, settings: MjpegSettings.Data) {
        val clientAddress = call.request.local.remoteAddress
        if (!serverData.isClientAllowed(clientAddress, call.request.queryParameters["pin"])) {
            call.respond(io.ktor.http.HttpStatusCode.Forbidden, "PIN required")
            return
        }
        val html = baseIndexHtml
            .replace("{{stream_address}}", serverData.getStreamAddress())
            .replace("{{pin_enabled}}", settings.enablePin.toString())
        call.respondText(html, io.ktor.http.ContentType.Text.Html)
    }

    private suspend fun serveStream(call: ApplicationCall, scope: CoroutineScope) {
        val clientAddress = call.request.local.remoteAddress
        val clientId = serverData.addClient(clientAddress, call)
        try {
            call.respond(object : io.ktor.http.content.OutgoingContent.WriteChannelContent() {
                override val contentType = io.ktor.http.ContentType.parse(
                    "multipart/x-mixed-replace; boundary=$BOUNDARY"
                )
                override suspend fun writeTo(channel: io.ktor.utils.io.ByteWriteChannel) {
                    mjpegSharedFlow.collect { jpeg ->
                        channel.writeStringUtf8(PART_HEADER)
                        channel.writeFully(jpeg, 0, jpeg.size)
                        channel.writeStringUtf8(PART_FOOTER)
                        channel.flush()
                        serverData.setNextBytes(clientId, clientAddress, jpeg.size, 0)
                    }
                }
            })
        } finally {
            serverData.removeSocket(clientId)
        }
    }

    private suspend fun serveLatestJpeg(call: ApplicationCall) {
        // Returns the last captured JPEG frame as a single image
        call.response.headers.append("Refresh", "1")
        call.respondBytes(ByteArray(0), io.ktor.http.ContentType.Image.JPEG)
    }

    suspend fun stop(destroy: Boolean) {
        serverRef.getAndSet(null)?.stop(1000, 1000)
        if (destroy) serverData.clear()
    }

    fun destroy() {
        serverRef.getAndSet(null)?.stop(0, 0)
        serverData.destroy()
    }
}
