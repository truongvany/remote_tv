package com.example.remote_tv.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException

/**
 * Wake-on-LAN: Gửi Magic Packet qua UDP để đánh thức TV từ trạng thái standby.
 *
 * Magic Packet = 6 byte 0xFF + MAC address lặp lại 16 lần = 102 bytes tổng.
 * Gửi đến địa chỉ broadcast của subnet (vd: 192.168.1.255) hoặc địa chỉ broadcast toàn cục 255.255.255.255.
 */
object WakeOnLanSender {

    private const val TAG = "WakeOnLan"
    private const val WOL_PORT = 9

    /**
     * Gửi Magic Packet đến địa chỉ broadcast của subnet.
     *
     * @param macAddress   Địa chỉ MAC của TV, định dạng: "AA:BB:CC:DD:EE:FF" hoặc "AA-BB-CC-DD-EE-FF"
     * @param broadcastIp  Địa chỉ broadcast (vd: "192.168.1.255"). Mặc định "255.255.255.255".
     * @param port         UDP port, thường là 7 hoặc 9. Mặc định 9.
     * @return true nếu gửi thành công (không đảm bảo TV thức dậy ngay).
     */
    suspend fun send(
        macAddress: String,
        broadcastIp: String = "255.255.255.255",
        port: Int = WOL_PORT,
    ): Boolean = withContext(Dispatchers.IO) {
        val macBytes = parseMac(macAddress) ?: run {
            Log.e(TAG, "Invalid MAC address format: $macAddress")
            return@withContext false
        }

        val packet = buildMagicPacket(macBytes)

        return@withContext try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                val address = InetAddress.getByName(broadcastIp)
                val datagramPacket = DatagramPacket(packet, packet.size, address, port)
                socket.send(datagramPacket)
                Log.d(TAG, "Magic Packet sent to $macAddress via $broadcastIp:$port")
                true
            }
        } catch (e: SocketException) {
            Log.e(TAG, "Socket error sending WoL: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "WoL send failed: ${e.message}")
            false
        }
    }

    /**
     * Tính toán địa chỉ broadcast từ IP và prefix length.
     * Ví dụ: "192.168.1.100/24" → "192.168.1.255"
     */
    fun broadcastFromSubnet(ipWithPrefix: String): String? {
        return try {
            val parts = ipWithPrefix.split("/")
            if (parts.size != 2) return null
            val ip = parts[0].trim()
            val prefix = parts[1].trim().toInt()

            val ipBytes = InetAddress.getByName(ip).address
            val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))

            val ipInt = ((ipBytes[0].toInt() and 0xFF) shl 24) or
                ((ipBytes[1].toInt() and 0xFF) shl 16) or
                ((ipBytes[2].toInt() and 0xFF) shl 8) or
                (ipBytes[3].toInt() and 0xFF)

            val broadcastInt = ipInt or mask.inv()
            val b = broadcastInt
            "${(b shr 24) and 0xFF}.${(b shr 16) and 0xFF}.${(b shr 8) and 0xFF}.${b and 0xFF}"
        } catch (e: Exception) {
            Log.w(TAG, "Could not compute broadcast: ${e.message}")
            null
        }
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    /** Parse chuỗi MAC "AA:BB:CC:DD:EE:FF" hoặc "AA-BB-CC-DD-EE-FF" thành byte array 6 phần tử. */
    private fun parseMac(mac: String): ByteArray? {
        val clean = mac.replace(":", "").replace("-", "").replace(" ", "")
        if (clean.length != 12) return null
        return try {
            ByteArray(6) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** Xây dựng Magic Packet 102 bytes: 6 × 0xFF + MAC × 16. */
    private fun buildMagicPacket(mac: ByteArray): ByteArray {
        val packet = ByteArray(6 + 16 * 6)
        // 6 byte header: 0xFF
        for (i in 0..5) packet[i] = 0xFF.toByte()
        // MAC lặp 16 lần
        for (i in 0..15) {
            System.arraycopy(mac, 0, packet, 6 + i * 6, 6)
        }
        return packet
    }
}
