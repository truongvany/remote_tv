package com.example.remote_tv.data.protocol

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import com.example.remote_tv.data.debug.InAppDiagnostics

/**
 * Android TV Remote Control Protocol (port 6466 / _androidtvremote2._tcp)
 *
 * Giao tiếp với Android TV Remote Service bằng cách gửi key-event qua TCP.
 * Tham chiếu: https://github.com/tronikos/androidtvremote2
 *
 * Mỗi message là: [length: 2 bytes big-endian][payload protobuf]
 * Key-event protobuf đơn giản: field 1 (key_code varint), field 2 (direction varint: 1=DOWN, 2=UP)
 */
class AndroidTVRemoteProtocol : TVProtocol {

    private val TAG = "AndroidTVRemote"
    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: InputStream? = null

    companion object {
        // Phím mã (KEYCODE) theo chuẩn Android
        private val KEY_MAP = mapOf(
            "UP" to 19,
            "MOVE_UP" to 19,
            "DOWN" to 20,
            "MOVE_DOWN" to 20,
            "LEFT" to 21,
            "MOVE_LEFT" to 21,
            "RIGHT" to 22,
            "MOVE_RIGHT" to 22,
            "OK" to 66,
            "ENTER" to 66,
            "KEY_ENTER" to 66,
            "KEY_BACK" to 4,
            "KEY_HOME" to 3,
            "KEY_VOL_UP" to 24,
            "KEY_VOL_DOWN" to 25,
            "KEY_MUTE" to 164,
            "SEARCH" to 84,
            "KEY_SEARCH" to 84,
            "VOICE" to 231,
            "KEY_VOICE" to 231,
            "KEY_POWER" to 26,
            "KEY_MENU" to 82,
            "KEY_SETTINGS" to 176,
            "KEY_CH_UP" to 166,
            "KEY_CH_DOWN" to 167,
            "KEY_CHANNEL_UP" to 166,
            "KEY_CHANNEL_DOWN" to 167,
            "KEY_PLAY_PAUSE" to 85,
        )
    }

    override suspend fun connect(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            InAppDiagnostics.info(TAG, "Connecting to Android TV Remote at $ip:$port")
            
            val s = if (port == 6466) {
                // Sử dụng TLS cho port 6466 (v2 Remote Service)
                InAppDiagnostics.info(TAG, "Using TLS for secure connection...")
                val sslContext = AndroidTVCertificateManager.getSslContext()
                val factory = sslContext.socketFactory
                factory.createSocket() as SSLSocket
            } else {
                Socket()
            }

            s.connect(InetSocketAddress(ip, port), 3000)
            
            if (s is SSLSocket) {
                s.startHandshake()
                InAppDiagnostics.info(TAG, "TLS Handshake successful!")
            }

            socket = s
            outputStream = DataOutputStream(s.getOutputStream())
            inputStream = s.getInputStream()
            Log.d(TAG, "Connected to Android TV Remote!")
            true
        } catch (e: Exception) {
            InAppDiagnostics.error(TAG, "Failed to connect: ${e.message}")
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
        outputStream = null
        inputStream = null
    }

    override suspend fun sendCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        val keyCode = KEY_MAP[command]
        if (keyCode == null) {
            Log.w(TAG, "Unknown command: $command")
            return@withContext false
        }
        val out = outputStream ?: return@withContext false
        return@withContext try {
            // Gửi KEY_DOWN rồi KEY_UP
            sendKeyEvent(out, keyCode, 1) // 1 = DOWN
            sendKeyEvent(out, keyCode, 2) // 2 = UP
            Log.d(TAG, "Sent key $command (keyCode=$keyCode)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "sendCommand error: ${e.message}")
            false
        }
    }

    /**
     * Tạo protobuf đơn giản cho RemoteKeyEvent:
     *   message RemoteKeyEvent { int32 key_code = 1; int32 direction = 2; }
     * Mỗi field: tag = (field_num << 3) | wire_type (varint=0)
     */
    private fun sendKeyEvent(out: DataOutputStream, keyCode: Int, direction: Int) {
        // field 1 (key_code): tag = (1 << 3) | 0 = 0x08
        // field 2 (direction): tag = (2 << 3) | 0 = 0x10
        val payload = byteArrayOf(
            0x08.toByte(), keyCode.toByte(),
            0x10.toByte(), direction.toByte()
        )
        // Wrapper: field 4 (remote_key_inject) inside RemoteMessage
        // tag = (4 << 3) | 2 = 0x22  (wire type 2 = length-delimited)
        val inner = byteArrayOf(0x22.toByte(), payload.size.toByte()) + payload

        // Đóng gói message với 2-byte big-endian length prefix
        out.writeShort(inner.size)
        out.write(inner)
        out.flush()
    }

    override suspend fun launchApp(appId: String): Boolean = withContext(Dispatchers.IO) {
        val ip = socket?.inetAddress?.hostAddress ?: return@withContext false
        val target = appId.trim()
        if (target.isBlank()) return@withContext false

        val adb = AdbProtocol()
        return@withContext try {
            val connected = adb.connect(ip, 5555)
            if (!connected) {
                InAppDiagnostics.warn(TAG, "launchApp ADB connect failed: $ip:5555")
                return@withContext false
            }

            val success = adb.launchApp(target)
            if (success) {
                InAppDiagnostics.info(TAG, "launchApp via ADB success: $target")
            } else {
                InAppDiagnostics.warn(TAG, "launchApp via ADB failed: $target")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "launchApp ADB error: ${e.message}")
            false
        } finally {
            runCatching { adb.disconnect() }
        }
    }
}

