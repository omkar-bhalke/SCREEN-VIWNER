package info.dvkr.screenstream.rtsp.internal

/**
 * RTP packetizer for H.264 video.
 * Splits AVCC-format NAL units into RTP packets per RFC 6184.
 * Supports single-NAL (< 1400 bytes) and FU-A fragmentation.
 */
object RtpPacketizer {

    private const val RTP_MTU = 1400
    private const val RTP_VERSION = 0x80
    private const val PT_H264: Byte = 96
    private var sequenceNumber: Int = 0
    private var ssrc: Int = (Math.random() * 0xFFFFFFFF).toInt()

    fun packetizeH264(
        avccData: ByteArray,
        presentationTimeUs: Long,
        sps: ByteArray?,
        pps: ByteArray?
    ): List<ByteArray> {
        val packets = mutableListOf<ByteArray>()
        val timestamp = (presentationTimeUs * 90 / 1_000_000).toInt()  // 90kHz clock

        // Extract NAL units (strip 4-byte length prefix in AVCC)
        var offset = 0
        while (offset + 4 <= avccData.size) {
            val nalLen = ((avccData[offset].toInt() and 0xFF) shl 24) or
                         ((avccData[offset+1].toInt() and 0xFF) shl 16) or
                         ((avccData[offset+2].toInt() and 0xFF) shl 8) or
                         (avccData[offset+3].toInt() and 0xFF)
            offset += 4
            if (offset + nalLen > avccData.size) break
            val nal = avccData.copyOfRange(offset, offset + nalLen)
            offset += nalLen

            if (nal.size <= RTP_MTU) {
                // Single NAL unit packet
                packets += buildRtpPacket(nal, timestamp, marker = true)
            } else {
                // FU-A fragmentation
                packets += fuaFragments(nal, timestamp)
            }
        }
        return packets
    }

    private fun fuaFragments(nal: ByteArray, timestamp: Int): List<ByteArray> {
        val packets = mutableListOf<ByteArray>()
        val nalHeader = nal[0]
        val nalType = nalHeader.toInt() and 0x1F
        val fuIndicator = (nalHeader.toInt() and 0xE0) or 28  // FU-A indicator
        var offset = 1  // skip original NAL header

        while (offset < nal.size) {
            val end = minOf(offset + RTP_MTU - 2, nal.size)
            val isFirst = offset == 1
            val isLast = end == nal.size
            val fuHeader = (if (isFirst) 0x80 else 0) or
                           (if (isLast) 0x40 else 0) or
                           nalType

            val payload = ByteArray(2 + (end - offset))
            payload[0] = fuIndicator.toByte()
            payload[1] = fuHeader.toByte()
            nal.copyInto(payload, 2, offset, end)

            packets += buildRtpPacket(payload, timestamp, marker = isLast)
            offset = end
        }
        return packets
    }

    private fun buildRtpPacket(payload: ByteArray, timestamp: Int, marker: Boolean): ByteArray {
        val seq = sequenceNumber++ and 0xFFFF
        val pkt = ByteArray(12 + payload.size)
        pkt[0] = RTP_VERSION.toByte()
        pkt[1] = (PT_H264.toInt() or (if (marker) 0x80 else 0)).toByte()
        pkt[2] = (seq shr 8).toByte()
        pkt[3] = (seq and 0xFF).toByte()
        pkt[4] = (timestamp shr 24).toByte()
        pkt[5] = (timestamp shr 16 and 0xFF).toByte()
        pkt[6] = (timestamp shr 8 and 0xFF).toByte()
        pkt[7] = (timestamp and 0xFF).toByte()
        pkt[8]  = (ssrc shr 24).toByte()
        pkt[9]  = (ssrc shr 16 and 0xFF).toByte()
        pkt[10] = (ssrc shr 8 and 0xFF).toByte()
        pkt[11] = (ssrc and 0xFF).toByte()
        payload.copyInto(pkt, 12)
        return pkt
    }
}
