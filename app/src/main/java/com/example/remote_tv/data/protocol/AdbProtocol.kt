package com.example.remote_tv.data.protocol

import com.example.remote_tv.data.AdbKeyManager
import com.example.remote_tv.data.debug.InAppDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.text.Normalizer
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
        "KEY_SETTINGS"   to 176,
        "KEY_ENTER"      to 66,  "OK"              to 66,
        "KEY_VOL_UP"      to 24, "KEY_VOL_DOWN"    to 25,
        "KEY_MUTE"        to 164,"KEY_PLAY_PAUSE"  to 85,
        "KEY_SEARCH"      to 84, "SEARCH"          to 84,
        "KEY_VOICE"       to 231,"VOICE"           to 231,
        "KEY_PLAY"        to 126,"KEY_PAUSE"       to 127,
        "KEY_STOP"        to 86, "KEY_REWIND"      to 89,
        "KEY_FF"          to 90,
        "UP"             to 19,  "MOVE_UP"         to 19,
        "DOWN"           to 20,  "MOVE_DOWN"       to 20,
        "LEFT"           to 21,  "MOVE_LEFT"       to 21,
        "RIGHT"          to 22,  "MOVE_RIGHT"      to 22,
        "KEY_CH_UP"      to 166, "KEY_CH_DOWN"     to 167,
        "KEY_CHANNEL_UP" to 166, "KEY_CHANNEL_DOWN" to 167,
        "KEY_0" to 7,  "KEY_1" to 8,  "KEY_2" to 9,  "KEY_3" to 10,
        "KEY_4" to 11, "KEY_5" to 12, "KEY_6" to 13, "KEY_7" to 14,
        "KEY_8" to 15, "KEY_9" to 16,
    )

    override suspend fun connect(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val targets = buildConnectTargets(ip)
            InAppDiagnostics.info(TAG, "ADB connect target=$ip:$port candidates=${targets.joinToString()}")

            var s: Socket? = null
            var connectedTarget: String? = null

            targets.forEach { target ->
                if (s != null) return@forEach

                val candidateSocket = Socket()
                try {
                    candidateSocket.connect(InetSocketAddress(target, port), 3000)
                    s = candidateSocket
                    connectedTarget = target
                } catch (candidateError: Exception) {
                    InAppDiagnostics.warn(
                        TAG,
                        "ADB candidate failed $target:$port => ${candidateError::class.simpleName}: ${candidateError.message}"
                    )
                    runCatching { candidateSocket.close() }
                }
            }

            val connectedSocket = s ?: return@withContext false
            s.soTimeout = 4000
            socket = connectedSocket
            input  = DataInputStream(connectedSocket.getInputStream())
            output = DataOutputStream(connectedSocket.getOutputStream())

            InAppDiagnostics.info(TAG, "ADB socket connected via ${connectedTarget ?: ip}")

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

        if (command.startsWith("OPEN_URL:")) {
            val payload = command.removePrefix("OPEN_URL:").trim()
            if (payload.isBlank()) {
                InAppDiagnostics.warn(TAG, "ADB OPEN_URL ignored: empty payload")
                return@withContext false
            }

            val parts = payload.split("|", limit = 2)
            val decodedUrl = runCatching {
                URLDecoder.decode(parts.first(), StandardCharsets.UTF_8.toString())
            }.getOrNull()?.trim().orEmpty()

            val decodedMime = runCatching {
                URLDecoder.decode(parts.getOrNull(1).orEmpty(), StandardCharsets.UTF_8.toString())
            }.getOrDefault("*/*").trim().ifBlank { "*/*" }

            if (decodedUrl.isBlank()) {
                InAppDiagnostics.warn(TAG, "ADB OPEN_URL ignored: invalid URL payload")
                return@withContext false
            }

            return@withContext openUrlOnTv(decodedUrl, decodedMime)
        }

        if (command.startsWith("SEARCH_QUERY:")) {
            val query = command.removePrefix("SEARCH_QUERY:").trim()
            if (query.isBlank()) {
                return@withContext false
            }
            return@withContext sendSearchQuery(query)
        }

        if (command.startsWith("TEXT:")) {
            val rawText = command.substring(5)
            // Chuẩn hóa tiếng Việt sang dạng NFC để TV dễ nhận diện
            val normalized = Normalizer.normalize(rawText, Normalizer.Form.NFC)
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")

            if (normalized.isBlank()) {
                InAppDiagnostics.warn(TAG, "ADB TEXT ignored: blank input")
                return@withContext false
            }

            // Kiểm tra xem có ký tự tiếng Việt hoặc ký tự đặc biệt không
            val hasUnicode = normalized.any { it.code > 127 }

            if (hasUnicode) {
                InAppDiagnostics.info(TAG, "Unicode text detected, using Clipboard path")
                val pasted = sendTextViaClipboard(normalized)
                if (pasted) return@withContext true
                
                // Nếu clipboard thất bại, thử dùng cmd input (hỗ trợ unicode ở một số đời TV mới)
                InAppDiagnostics.warn(TAG, "Clipboard failed, trying cmd input fallback")
                val sentByCmd = sendTextViaCmdInput(normalized)
                if (sentByCmd) return@withContext true
            }

            // Trường hợp tiếng Anh hoặc các fallback unicode đều thất bại
            val injected = sendTextByInputCommand(normalized)
            if (injected) {
                InAppDiagnostics.info(TAG, "ADB text sent via standard input text")
                return@withContext true
            }

            InAppDiagnostics.error(TAG, "All ADB text paths failed for: $normalized")
            return@withContext false
        }

        val keyCode = keyMap[command] ?: run {
            InAppDiagnostics.warn(TAG, "ADB: unknown command $command"); return@withContext false
        }

        return@withContext sendShellCommand("input keyevent $keyCode\n")
            .also { sent ->
                if (sent) {
                    InAppDiagnostics.info(TAG, "ADB command: $command sent")
                }
            }
    }

    private suspend fun sendTextByInputCommand(text: String): Boolean {
        val encoded = encodeForAndroidInputText(text)
        val chunks = encoded.chunked(36)

        chunks.forEachIndexed { index, chunk ->
            val escaped = escapeForShellDoubleQuoted(chunk)
            val sent = sendShellCommand("input text \"$escaped\"\n")
            if (!sent) {
                InAppDiagnostics.error(TAG, "ADB TEXT input failed at chunk ${index + 1}/${chunks.size}")
                return false
            }

            if (index < chunks.lastIndex) {
                delay(35)
            }
        }

        return true
    }

    private suspend fun sendTextViaCmdInput(text: String): Boolean {
        val encoded = encodeForAndroidInputText(text)
        val chunks = encoded.chunked(32)
        chunks.forEachIndexed { index, chunk ->
            val escaped = escapeForShellDoubleQuoted(chunk)
            val sent = sendShellCommand("cmd input text \"$escaped\"\n")
            if (!sent) {
                InAppDiagnostics.warn(TAG, "ADB cmd input failed at chunk ${index + 1}/${chunks.size}")
                return false
            }

            if (index < chunks.lastIndex) {
                delay(25)
            }
        }

        return true
    }

    private fun sendTextViaClipboard(text: String): Boolean {
        val escaped = escapeForShellDoubleQuoted(text)

        val setClipboardCommands = listOf(
            "cmd clipboard set text \"$escaped\"\n",
            "cmd clipboard set \"$escaped\"\n",
            // Older Android builds may only expose clipboard service call variants.
            "service call clipboard 2 i32 0 s16 \"com.android.shell\" s16 \"$escaped\"\n",
            "service call clipboard 1 i32 0 s16 \"com.android.shell\" s16 \"$escaped\"\n",
        )

        val setClipboard = setClipboardCommands.any { cmd -> sendShellCommand(cmd) }
        if (!setClipboard) {
            InAppDiagnostics.warn(TAG, "ADB clipboard set failed on all known commands")
            return false
        }

        val pasteCommands = listOf(
            "input keyevent 279\n",          // KEYCODE_PASTE
            "input keycombination 113 50\n", // CTRL + V
            "input keyevent 50\n",           // KEYCODE_V fallback
        )

        val pasted = pasteCommands.any { cmd -> sendShellCommand(cmd) }
        if (!pasted) {
            InAppDiagnostics.warn(TAG, "ADB clipboard paste failed on all known key paths")
        }
        return pasted
    }

    private fun escapeForShellDoubleQuoted(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
            .replace("`", "\\`")
    }

    private fun encodeForAndroidInputText(value: String): String {
        // Android input text uses %s as the canonical space token.
        return buildString(value.length * 2) {
            value.forEach { ch ->
                when (ch) {
                    ' ' -> append("%s")
                    else -> append(ch)
                }
            }
        }
    }

    private fun buildConnectTargets(ip: String): List<String> {
        val trimmed = ip.trim()
        if (trimmed.isBlank()) return emptyList()

        val base = trimmed.substringBefore('%')
        val isIpv6LinkLocal = base.contains(":") && base.startsWith("fe80", ignoreCase = true)
        if (!isIpv6LinkLocal) {
            return listOf(trimmed)
        }

        if (trimmed.contains("%")) {
            return listOf(trimmed)
        }

        val scoped = listOf("wlan0", "eth0", "ap0", "swlan0").map { scope ->
            "$base%$scope"
        }

        // Keep the original unscoped target as last fallback for devices that resolve scope internally.
        return (scoped + base).distinct()
    }

    private fun sendSearchQuery(query: String): Boolean {
        val escaped = escapeForShellDoubleQuoted(query)

        val searchResult = sendShellCommandDetailed(
            "am start -a android.intent.action.SEARCH --es query \"$escaped\"\n"
        )
        if (searchResult.success) {
            InAppDiagnostics.info(TAG, "ADB search intent success: ACTION_SEARCH")
            return true
        }

        val webSearchResult = sendShellCommandDetailed(
            "am start -a android.intent.action.WEB_SEARCH --es query \"$escaped\"\n"
        )
        if (webSearchResult.success) {
            InAppDiagnostics.info(TAG, "ADB search intent success: ACTION_WEB_SEARCH")
            return true
        }

        InAppDiagnostics.warn(
            TAG,
            "ADB search intent failed for query='${query.take(32)}'"
        )
        return false
    }

    private fun openUrlOnTv(url: String, mimeType: String): Boolean {
        val escapedUrl = escapeForShellDoubleQuoted(url)
        val escapedMime = escapeForShellDoubleQuoted(mimeType)
        val isImageOrHtml = mimeType.startsWith("image/", ignoreCase = true) ||
            mimeType.equals("text/html", ignoreCase = true)

        val attempts = if (isImageOrHtml) {
            listOf(
                "am start -a android.intent.action.VIEW -d \"$escapedUrl\"\n",
                "am start -a android.intent.action.VIEW -d \"$escapedUrl\" -t \"$escapedMime\"\n",
            )
        } else {
            listOf(
                "am start -a android.intent.action.VIEW -d \"$escapedUrl\" -t \"$escapedMime\"\n",
                "am start -a android.intent.action.VIEW -d \"$escapedUrl\"\n",
            )
        }

        attempts.forEachIndexed { index, shell ->
            val result = sendShellCommandDetailed(shell)
            if (result.success) {
                InAppDiagnostics.info(TAG, "ADB OPEN_URL success attempt=${index + 1} mime=$mimeType")
                return true
            }
        }

        InAppDiagnostics.warn(TAG, "ADB OPEN_URL failed url=${url.take(96)} mime=$mimeType")
        return false
    }

    private fun sendShellCommand(shellCmd: String): Boolean {
        return sendShellCommandDetailed(shellCmd).success
    }

    private data class ShellCommandResult(
        val success: Boolean,
        val output: String,
    )

    private fun sendShellCommandDetailed(shellCmd: String): ShellCommandResult {
        return try {
            send(A_WRTE, localId, remoteId, shellCmd.toByteArray())

            var receivedOkay = false
            var guard = 0
            val outputBuilder = StringBuilder()

            while (guard < 16) {
                val msg = read() ?: break
                when (msg.command) {
                    A_OKAY -> {
                        receivedOkay = true
                        break
                    }
                    A_WRTE -> {
                        if (msg.data.isNotEmpty()) {
                            outputBuilder.append(String(msg.data, Charsets.UTF_8))
                        }
                        // Ack shell output frame to keep the stream healthy.
                        send(A_OKAY, localId, remoteId, ByteArray(0))
                    }
                    A_CLSE -> {
                        send(A_CLSE, localId, remoteId, ByteArray(0))
                        return ShellCommandResult(success = false, output = outputBuilder.toString())
                    }
                }
                guard++
            }

            val output = outputBuilder.toString().trim()
            val hasErrorText = looksLikeShellError(output)
            if (hasErrorText) {
                InAppDiagnostics.warn(TAG, "ADB shell output error: ${output.take(160)}")
            }
            ShellCommandResult(success = receivedOkay && !hasErrorText, output = output)
        } catch (e: Exception) {
            InAppDiagnostics.error(TAG, "ADB sendCommand failed: ${e.message}")
            ShellCommandResult(success = false, output = e.message ?: "")
        }
    }

    private fun looksLikeShellError(output: String): Boolean {
        if (output.isBlank()) return false
        val normalized = output.lowercase()
        return normalized.contains("unknown command") ||
            normalized.contains("not found") ||
            normalized.contains("can't find service") ||
            normalized.contains("error:") ||
            normalized.contains("exception") ||
            normalized.contains("usage:")
    }

    override suspend fun launchApp(appId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext false

        val launchTarget = appId.trim()
        if (launchTarget.isBlank()) {
            InAppDiagnostics.warn(TAG, "ADB launch app ignored: blank target")
            return@withContext false
        }

        val shellCommand = when {
            launchTarget.startsWith("am ") ||
                launchTarget.startsWith("cmd ") ||
                launchTarget.startsWith("monkey ") -> "$launchTarget\n"

            else -> "monkey -p $launchTarget -c android.intent.category.LAUNCHER 1\n"
        }

        val result = sendShellCommandDetailed(shellCommand)
        val failedByOutput = looksLikeAppLaunchFailure(result.output)
        val success = result.success && !failedByOutput

        if (success) {
            InAppDiagnostics.info(TAG, "ADB launch app success: $launchTarget")
        } else {
            InAppDiagnostics.warn(
                TAG,
                "ADB launch app failed: $launchTarget output=${result.output.take(140)}"
            )
        }

        success
    }

    private fun looksLikeAppLaunchFailure(output: String): Boolean {
        if (output.isBlank()) return false
        val normalized = output.lowercase()
        return normalized.contains("monkey aborted") ||
            normalized.contains("no activities found to run") ||
            (normalized.contains("activity class") && normalized.contains("does not exist")) ||
            normalized.contains("unable to resolve intent") ||
            normalized.contains("permission denial") ||
            normalized.contains("securityexception")
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
