package com.example.remote_tv.data.protocol

import com.example.remote_tv.data.AdbKeyManager
import com.example.remote_tv.data.debug.InAppDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.Signature as JSignature

/**
 * ADB over TCP protocol (port 5555).
 *
 * Full auth flow:
 *   Client → CNXN
 *   TV     → AUTH(TOKEN)
 *   Client → AUTH(SIGNATURE, signedToken)
 *   TV     → CNXN  (nếu key đã approve trước)
 *         hoặc AUTH(TOKEN) nếu key chưa biết
 *   Client → AUTH(RSAPUBLICKEY)  → TV hiện dialog "Cho phép ADB?"
 *   User bấm OK
 *   TV     → CNXN → mở shell → kết nối thành công
 */
class AdbProtocol : TVProtocol {

    private val TAG = "AdbProtocol"

    private val A_CNXN = 0x4e584e43
    private val A_OPEN = 0x4e45504f
    private val A_WRTE = 0x45545257
    private val A_CLSE = 0x45534c43
    private val A_OKAY = 0x59414b4f
    private val A_AUTH = 0x48545541

    private val AUTH_TOKEN        = 1
    private val AUTH_SIGNATURE    = 2
    private val AUTH_RSAPUBLICKEY = 3

    private val ADB_VERSION = 0x01000001
    private val MAX_DATA    = 256 * 1024

    private var socket:      Socket?           = null
    private var input:       DataInputStream?  = null
    private var output:      DataOutputStream? = null
    private var remoteId:    Int     = 0
    private var localId:     Int     = 1
    private var isConnected: Boolean = false

    private val keyMap = mapOf(
        "KEY_POWER"      to 26,  "KEY_HOME"       to 3,
        "KEY_BACK"       to 4,   "KEY_MENU"        to 82,
        "KEY_ENTER"      to 66,  "OK"              to 66,
        "KEY_VOL_UP"      to 24, "KEY_VOL_DOWN"    to 25,
        "KEY_MUTE"        to 164,"KEY_PLAY_PAUSE"  to 85,
        "KEY_PLAY"        to 126,"KEY_PAUSE"       to 127,
        "KEY_STOP"        to 86, "KEY_REWIND"      to 89,
        "KEY_FF"          to 90,
        "UP"             to 19,  "MOVE_UP"         to 19,
        "DOWN"           to 20,  "MOVE_DOWN"       to 20,
        "LEFT"           to 21,  "MOVE_LEFT"       to 21,
        "RIGHT"          to 22,  "MOVE_RIGHT"      to 22,
        "KEY_CH_UP"      to 166, "KEY_CH_DOWN"     to 167,
        "KEY_0" to 7,  "KEY_1" to 8,  "KEY_2" to 9,  "KEY_3" to 10,
        "KEY_4" to 11, "KEY_5" to 12, "KEY_6" to 13, "KEY_7" to 14,
        "KEY_8" to 15, "KEY_9" to 16,
    )

    override suspend fun connect(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            InAppDiagnostics.info(TAG, "ADB connect $ip:$port")
            val s = Socket()
            s.connect(InetSocketAddress(ip, port), 3000)
            s.soTimeout = 4000
            socket = s
            input  = DataInputStream(s.getInputStream())
            output = DataOutputStream(s.getOutputStream())

            send(A_CNXN, ADB_VERSION, MAX_DATA, "host::remote_tv\u0000".toByteArray())

            var sentSignature = false
            var sentPubKey = false

            // Repeat nhiều hơn đề phòng TV gửi ping
            repeat(10) {
                val msg = read() ?: return@withContext false
                when (msg.command) {
                    A_CNXN -> {
                        InAppDiagnostics.info(TAG, "ADB CNXN OK: ${String(msg.data)}")
                        return@withContext openShell()
                    }
                    A_AUTH -> {
                        if (msg.arg0 != AUTH_TOKEN) return@withContext false
                        val kp = AdbKeyManager.getKeyPair()
                        if (kp != null && !sentSignature) {
                            val sig = JSignature.getInstance("SHA1withRSA").apply {
                                initSign(kp.private); update(msg.data)
                            }.sign()
                            send(A_AUTH, AUTH_SIGNATURE, 0, sig)
                            sentSignature = true
                            InAppDiagnostics.info(TAG, "ADB: sent AUTH_SIGNATURE")
                        } else if (!sentPubKey) {
                            val pubKey = AdbKeyManager.buildAdbPublicKeyPayload()
                            send(A_AUTH, AUTH_RSAPUBLICKEY, 0, pubKey)
                            sentPubKey = true
                            s.soTimeout = 30_000
                            InAppDiagnostics.info(TAG, "ADB: sent AUTH_RSAPUBLICKEY — bấm Cho phép trên TV!")
                        } else {
                            // Đã gửi pubKey, tiếp tục lặp để chờ phản hồi từ TV
                            InAppDiagnostics.info(TAG, "ADB: waiting for user confirmation...")
                        }
                    }
                    else -> return@withContext false
                }
            }
            false
        } catch (e: java.net.SocketTimeoutException) {
            InAppDiagnostics.warn(TAG, "ADB: timeout — bấm 'Cho phép ADB' trên TV rồi thử lại!")
            cleanup(); false
        } catch (e: Exception) {
            InAppDiagnostics.error(TAG, "ADB connect failed: ${e::class.simpleName}: ${e.message}")
            cleanup(); false
        }
    }

    private fun openShell(): Boolean {
        send(A_OPEN, localId, 0, "shell:\u0000".toByteArray())
        socket?.soTimeout = 4000
        val ok = read() ?: return false
        return if (ok.command == A_OKAY) {
            remoteId = ok.arg0
            isConnected = true
            InAppDiagnostics.info(TAG, "ADB shell opened. remoteId=$remoteId")
            true
        } else {
            InAppDiagnostics.error(TAG, "ADB: shell open failed cmd=${ok.command}"); false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try { if (isConnected) send(A_CLSE, localId, remoteId, ByteArray(0)) } catch (_: Exception) {}
        cleanup()
    }

    override suspend fun sendCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext false
        
        val shellCmd = if (command.startsWith("TEXT:")) {
            // Android TV shell 'input text' dùng %s thay cho khoảng trắng
            val text = command.substring(5).replace(" ", "%s").replace("'", "\\'")
            "input text '$text'\n"
        } else {
            val keyCode = keyMap[command] ?: run {
                InAppDiagnostics.warn(TAG, "ADB: unknown command $command"); return@withContext false
            }
            "input keyevent $keyCode\n"
        }

        return@withContext try {
            send(A_WRTE, localId, remoteId, shellCmd.toByteArray())
            val ack = read()
            if (ack?.command == A_OKAY) send(A_OKAY, localId, remoteId, ByteArray(0))
            InAppDiagnostics.info(TAG, "ADB command: $command sent")
            true
        } catch (e: Exception) {
            InAppDiagnostics.error(TAG, "ADB sendCommand failed: ${e.message}"); false
        }
    }

    override suspend fun launchApp(appId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext false
        return@withContext try {
            send(A_WRTE, localId, remoteId, "monkey -p $appId -c android.intent.category.LAUNCHER 1\n".toByteArray())
            InAppDiagnostics.info(TAG, "ADB launch app: $appId"); true
        } catch (e: Exception) {
            InAppDiagnostics.error(TAG, "ADB launchApp failed: ${e.message}"); false
        }
    }

    // ── Wire protocol helpers ───────────────────────────────────────────────

    private data class Msg(val command: Int, val arg0: Int, val arg1: Int, val data: ByteArray)

    private fun send(command: Int, arg0: Int, arg1: Int, data: ByteArray) {
        val checksum = data.fold(0) { acc, b -> acc + (b.toInt() and 0xFF) }
        val buf = ByteBuffer.allocate(24 + data.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(command); buf.putInt(arg0); buf.putInt(arg1)
        buf.putInt(data.size); buf.putInt(checksum); buf.putInt(command xor -1)
        buf.put(data)
        output?.write(buf.array()); output?.flush()
    }

    private fun read(): Msg? {
        val inp = input ?: return null
        val hdr = ByteArray(24).also { inp.readFully(it) }
        val buf = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN)
        val cmd = buf.int; val a0 = buf.int; val a1 = buf.int
        val len = buf.int; buf.int; buf.int
        val data = if (len > 0) ByteArray(len).also { inp.readFully(it) } else ByteArray(0)
        return Msg(cmd, a0, a1, data)
    }

    private fun cleanup() {
        isConnected = false
        try { input?.close() }  catch (_: Exception) {}
        try { output?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        socket = null; input = null; output = null
    }
}
