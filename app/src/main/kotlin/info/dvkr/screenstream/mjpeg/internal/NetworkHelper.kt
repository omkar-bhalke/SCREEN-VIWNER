package info.dvkr.screenstream.mjpeg.internal

import android.content.Context
import android.net.wifi.WifiManager
import java.net.NetworkInterface

class NetworkHelper(private val context: Context) {

    fun getNetInterfaces(
        ipv4Filter: Int,
        ipv6Filter: Int,
        enableIPv4: Boolean,
        enableIPv6: Boolean
    ): List<MjpegNetInterface> {
        val result = mutableListOf<MjpegNetInterface>()
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val port = 8080 // read from settings in real impl

        try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { iface ->
                if (!iface.isUp || iface.isLoopback) return@forEach
                iface.inetAddresses.toList().forEach { addr ->
                    when {
                        addr is java.net.Inet4Address && enableIPv4 ->
                            result += MjpegNetInterface(iface.name, addr.hostAddress ?: return@forEach, port)
                        addr is java.net.Inet6Address && enableIPv6 ->
                            result += MjpegNetInterface(iface.name, addr.hostAddress?.split("%")?.first() ?: return@forEach, port)
                    }
                }
            }
        } catch (e: Exception) {
            // network unavailable
        }
        return result
    }
}
