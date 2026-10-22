package info.dvkr.screenstream.mjpeg.internal

import io.ktor.server.application.ApplicationCall

import info.dvkr.screenstream.mjpeg.settings.MjpegSettings
import info.dvkr.screenstream.mjpeg.ui.MjpegState
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class HttpServerData(private val sendEvent: (MjpegEvent) -> Unit) {

    companion object {
        const val DEFAULT_STREAM_PATH = "/stream"
        const val DEFAULT_JPEG_PATH   = "/stream.mjpeg"
    }

    private val clientIdCounter = AtomicLong(0)
    private val clients = ConcurrentHashMap<Long, MjpegState.Client>()

    private var pinEnabled = false
    private var pin = ""
    private var streamAddress = DEFAULT_STREAM_PATH
    private var blockAddressList = listOf<String>()

    fun configure(settings: MjpegSettings.Data) {
        pinEnabled     = settings.enablePin
        pin            = settings.pin
        streamAddress  = DEFAULT_STREAM_PATH
        blockAddressList = settings.blockAddressList
    }

    fun isClientAllowed(remoteAddress: String, queryPin: String?): Boolean {
        if (remoteAddress in blockAddressList) return false
        if (!pinEnabled) return true
        return queryPin == pin
    }

    fun addClient(address: String, call: io.ktor.server.application.ApplicationCall): Long {
        val id = clientIdCounter.incrementAndGet()
        clients[id] = MjpegState.Client(
            id = id,
            remoteAddress = address,
            state = MjpegState.Client.State.CONNECTED,
            transferBytes = 0L,
            transferSpeed = 0L
        )
        sendEvent(MjpegEvent.ClientConnected(id, address))
        return id
    }

    fun setNextBytes(clientId: Long, address: String, bytes: Int, speed: Long) {
        clients.computeIfPresent(clientId) { _, c ->
            c.copy(transferBytes = c.transferBytes + bytes, transferSpeed = speed)
        }
    }

    fun removeSocket(clientId: Long) {
        clients.remove(clientId)
        sendEvent(MjpegEvent.ClientDisconnected(clientId))
    }

    fun getClientList(): List<MjpegState.Client> = clients.values.toList()

    fun clear() { clients.clear() }

    fun destroy() { clients.clear() }

    fun getStreamAddress() = streamAddress
    fun getJpegFallbackAddress() = DEFAULT_JPEG_PATH
}
