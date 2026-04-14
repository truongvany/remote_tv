package com.example.remote_tv.data.protocol

import com.example.remote_tv.data.adb.WirelessAdbConnectionManager
import com.example.remote_tv.data.debug.InAppDiagnostics
import io.github.muntashirakon.adb.AdbPairingRequiredException
import io.github.muntashirakon.adb.AdbStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WirelessAdbProtocol(
    private val manager: WirelessAdbConnectionManager,
) : TVProtocol {

    private val tag = "WirelessAdbProtocol"

    private var connectedHost: String? = null
    private var connectedPort: Int? = null
    private var shellStream: AdbStream? = null

    private val keyMap = mapOf(
        "KEY_POWER" to 26,
        "KEY_HOME" to 3,
        "KEY_BACK" to 4,
        "KEY_MENU" to 82,
        "KEY_SETTINGS" to 176,
        "KEY_ENTER" to 66,
        "OK" to 66,
        "KEY_VOL_UP" to 24,
        "KEY_VOL_DOWN" to 25,
        "KEY_MUTE" to 164,
        "KEY_PLAY_PAUSE" to 85,
        "KEY_SEARCH" to 84,
        "SEARCH" to 84,
        "KEY_VOICE" to 231,
        "VOICE" to 231,
        "KEY_PLAY" to 126,
        "KEY_PAUSE" to 127,
        "KEY_STOP" to 86,
        "UP" to 19,
        "MOVE_UP" to 19,
        "DOWN" to 20,
        "MOVE_DOWN" to 20,
        "LEFT" to 21,
        "MOVE_LEFT" to 21,
        "RIGHT" to 22,
        "MOVE_RIGHT" to 22,
        "KEY_CH_UP" to 166,
        "KEY_CH_DOWN" to 167,
        "KEY_CHANNEL_UP" to 166,
        "KEY_CHANNEL_DOWN" to 167,
    )

    override suspend fun connect(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val connected = manager.connect(ip, port)
            if (!connected) {
                InAppDiagnostics.warn(tag, "Wireless ADB connect returned false for $ip:$port")
                false
            } else {
                connectedHost = ip
                connectedPort = port
                shellStream = manager.openStream("shell:")
                InAppDiagnostics.info(tag, "Wireless ADB connected at $ip:$port")
                true
            }
        } catch (e: AdbPairingRequiredException) {
            InAppDiagnostics.warn(tag, "Wireless ADB requires pairing for $ip:$port")
            false
        } catch (e: Exception) {
            InAppDiagnostics.error(tag, "Wireless ADB connect failed: ${e.message}")
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        runCatching { shellStream?.close() }
        runCatching { manager.disconnect() }
        shellStream = null
        connectedHost = null
        connectedPort = null
    }

    override suspend fun sendCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        if (!manager.isConnected()) {
            return@withContext false
        }

        if (command.startsWith("TEXT:")) {
            val text = command.removePrefix("TEXT:")
            return@withContext sendText(text)
        }

        if (command.startsWith("SEARCH_QUERY:")) {
            val query = command.removePrefix("SEARCH_QUERY:").trim()
            if (query.isBlank()) {
                return@withContext false
            }
            return@withContext sendSearchQuery(query)
        }

        if (command.startsWith("OPEN_URL:")) {
            val payload = command.removePrefix("OPEN_URL:").trim()
            if (payload.isBlank()) {
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
                return@withContext false
            }

            return@withContext openUrl(decodedUrl, decodedMime)
        }

        if (command == "KEY_SEARCH" || command == "SEARCH") {
            return@withContext openSearch()
        }

        val keyCode = keyMap[command] ?: return@withContext false
        runShellCommand("input keyevent $keyCode")
    }

    override suspend fun launchApp(appId: String): Boolean = withContext(Dispatchers.IO) {
        if (!manager.isConnected()) {
            return@withContext false
        }

        val launchTarget = appId.trim()
        if (launchTarget.isBlank()) {
            return@withContext false
        }

        val shellCommand = when {
            launchTarget.startsWith("am ") ||
                launchTarget.startsWith("cmd ") ||
                launchTarget.startsWith("monkey ") -> launchTarget
            else -> "monkey -p $launchTarget -c android.intent.category.LAUNCHER 1"
        }

        runShellCommand(shellCommand)
    }

    private fun sendText(rawText: String): Boolean {
        val normalized = Normalizer.normalize(rawText, Normalizer.Form.NFC)
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ")

        if (normalized.isBlank()) {
            return false
        }

        val hasNonAscii = normalized.any { ch -> ch.code > 127 }
        return if (hasNonAscii) {
            sendSearchQuery(normalized)
        } else {
            val encoded = normalized.replace(" ", "%s")
            runShellCommand("input text \"$encoded\"")
        }
    }

    private fun sendSearchQuery(query: String): Boolean {
        val escaped = escapeForShellDoubleQuoted(query)
        return runShellCommand("am start -a android.intent.action.SEARCH --es query \"$escaped\"") ||
            runShellCommand("am start -a android.intent.action.WEB_SEARCH --es query \"$escaped\"")
    }

    private fun openSearch(): Boolean {
        return runShellCommand("input keyevent 84") ||
            runShellCommand("am start -a android.intent.action.SEARCH") ||
            runShellCommand("am start -a android.intent.action.WEB_SEARCH")
    }

    private fun openUrl(url: String, mimeType: String): Boolean {
        val escapedUrl = escapeForShellDoubleQuoted(url)
        val escapedMime = escapeForShellDoubleQuoted(mimeType)
        return runShellCommand("am start -a android.intent.action.VIEW -d \"$escapedUrl\" -t \"$escapedMime\"") ||
            runShellCommand("am start -a android.intent.action.VIEW -d \"$escapedUrl\"")
    }

    private fun runShellCommand(command: String): Boolean {
        val stream = shellStream ?: return false
        return try {
            val out = stream.openOutputStream()
            out.write((command.trim() + "\n").toByteArray(Charsets.UTF_8))
            out.flush()
            true
        } catch (e: Exception) {
            InAppDiagnostics.warn(
                tag,
                "Wireless ADB shell command failed host=${connectedHost.orEmpty()}:${connectedPort ?: -1} cmd=${command.take(64)} reason=${e.message}"
            )
            false
        }
    }

    private fun escapeForShellDoubleQuoted(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
            .replace("`", "\\`")
    }
}
