package info.dvkr.screenstream.rtsp.internal

import info.dvkr.screenstream.rtsp.settings.RtspSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

/**
 * One RTSP client session.
 * Handles OPTIONS / DESCRIBE / SETUP / PLAY / TEARDOWN per RFC 2326.
 * After PLAY, RTP video packets are written to the client's RTP port via UDP.
 */
class RtspSession(
    val id: Long,
    private val socket: Socket,
    private val settings: RtspSettings.Data,
    private val onClose: () -> Unit,
    private val getSpsPps: () -> Pair<ByteArray, ByteArray>
) {
    private val reader = BufferedReader(InputStreamReader(socket.inputStream))
    private val writer = PrintWriter(socket.outputStream, true)
    private var rtpSocket: java.net.DatagramSocket? = null
    private var clientRtpPort: Int = 0
    private var clientAddress: String = socket.inetAddress.hostAddress ?: ""
    private var sessionId: String = "SSRC${(Math.random() * 0xFFFFFF).toLong()}"
    private var isPlaying = false

    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                var line: String?
                val requestLines = mutableListOf<String>()

                while (true) {
                    line = reader.readLine() ?: break
                    if (line.isBlank()) {
                        if (requestLines.isNotEmpty()) {
                            handleRequest(requestLines.toList())
                            requestLines.clear()
                        }
                    } else {
                        requestLines.add(line)
                    }
                }
            } catch (_: Exception) {
            } finally {
                close()
            }
        }
    }

    private fun handleRequest(lines: List<String>) {
        val requestLine = lines.firstOrNull() ?: return
        val parts = requestLine.split(" ")
        val method = parts.getOrNull(0) ?: return
        val cseq = lines.firstOrNull { it.startsWith("CSeq:") }
            ?.removePrefix("CSeq:")?.trim() ?: "0"

        when (method) {
            "OPTIONS" -> sendResponse(200, "OK", cseq, listOf(
                "Public: OPTIONS, DESCRIBE, SETUP, TEARDOWN, PLAY"
            ))

            "DESCRIBE" -> {
                val (sps, pps) = getSpsPps()
                val spsB64 = android.util.Base64.encodeToString(sps, android.util.Base64.NO_WRAP)
                val ppsB64 = android.util.Base64.encodeToString(pps, android.util.Base64.NO_WRAP)
                val profileHex = if (sps.size >= 4)
                    "%02X%02X%02X".format(sps[1].toInt() and 0xFF, sps[2].toInt() and 0xFF, sps[3].toInt() and 0xFF)
                else "42001E"

                val sdp = buildString {
                    appendLine("v=0")
                    appendLine("o=- 0 0 IN IP4 ${socket.localAddress.hostAddress}")
                    appendLine("s=ScreenStream")
                    appendLine("t=0 0")
                    appendLine("m=video 0 RTP/AVP 96")
                    appendLine("a=rtpmap:96 H264/90000")
                    appendLine("a=fmtp:96 packetization-mode=1;sprop-parameter-sets=$spsB64,$ppsB64;profile-level-id=$profileHex")
                    appendLine("a=control:streamid=0")
                }
                sendResponse(200, "OK", cseq,
                    listOf("Content-Type: application/sdp", "Content-Length: ${sdp.length}"),
                    sdp
                )
            }

            "SETUP" -> {
                val transport = lines.firstOrNull { it.startsWith("Transport:") } ?: ""
                val portMatch = Regex("client_port=(\\d+)-(\\d+)").find(transport)
                clientRtpPort = portMatch?.groupValues?.get(1)?.toIntOrNull() ?: 5004
                rtpSocket = java.net.DatagramSocket()
                sendResponse(200, "OK", cseq, listOf(
                    "Transport: RTP/AVP;unicast;client_port=$clientRtpPort-${clientRtpPort + 1};server_port=${rtpSocket!!.localPort}-${rtpSocket!!.localPort + 1}",
                    "Session: $sessionId"
                ))
            }

            "PLAY" -> {
                isPlaying = true
                sendResponse(200, "OK", cseq, listOf(
                    "Session: $sessionId",
                    "RTP-Info: url=rtsp://${socket.localAddress.hostAddress}${settings.streamPath}/streamid=0;seq=1;rtptime=0"
                ))
            }

            "TEARDOWN" -> {
                sendResponse(200, "OK", cseq, listOf("Session: $sessionId"))
                close()
            }
        }
    }

    private fun sendResponse(
        code: Int, message: String, cseq: String,
        headers: List<String> = emptyList(),
        body: String = ""
    ) {
        val sb = StringBuilder()
        sb.appendLine("RTSP/1.0 $code $message")
        sb.appendLine("CSeq: $cseq")
        headers.forEach { sb.appendLine(it) }
        if (body.isNotBlank()) sb.appendLine(body)
        sb.appendLine()
        writer.print(sb.toString())
        writer.flush()
    }

    fun sendRtp(packet: ByteArray) {
        if (!isPlaying) return
        try {
            val udpSocket = rtpSocket ?: return
            val dgram = java.net.DatagramPacket(
                packet, packet.size,
                java.net.InetAddress.getByName(clientAddress), clientRtpPort
            )
            udpSocket.send(dgram)
        } catch (_: Exception) { close() }
    }

    fun close() {
        isPlaying = false
        runCatching { rtpSocket?.close() }
        runCatching { socket.close() }
        onClose()
    }
}
