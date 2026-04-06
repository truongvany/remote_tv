package com.example.remote_tv.data.protocol

import com.example.remote_tv.data.debug.InAppDiagnostics
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.send
import java.net.URLEncoder
import java.util.Base64
import kotlinx.coroutines.delay
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.text.Normalizer

class SamsungProtocol(private val client: HttpClient) : TVProtocol {
    private val tag = "SamsungProtocol"
    private var session: DefaultClientWebSocketSession? = null
    private var connectedIp: String? = null
    private var connectedPort: Int? = null

    override suspend fun connect(ip: String, port: Int): Boolean {
        val encodedName = encodedClientName("REMOTE_TV")
        val schemes = schemesForPort(port)

        schemes.forEach { scheme ->
            try {
                val url = "$scheme://$ip:$port/api/v2/channels/samsung.remote.control?name=$encodedName"
                InAppDiagnostics.info(tag, "Open Samsung socket: $url")

                session = client.webSocketSession {
                    url(url)
                    header("Origin", "http://$ip")
                }

                connectedIp = ip
                connectedPort = port

                InAppDiagnostics.info(tag, "Samsung socket connected at $ip:$port via $scheme")
                return true
            } catch (e: Exception) {
                InAppDiagnostics.error(
                    tag,
                    "Samsung connect exception at $ip:$port via $scheme => ${e::class.simpleName}: ${e.message}"
                )
                session = null
            }
        }

        return false
    }

    override suspend fun disconnect() {
        try {
            session?.close()
        } catch (e: Exception) {
            // Ignore
        }
        session = null
        connectedIp = null
        connectedPort = null
    }

    override suspend fun sendCommand(command: String): Boolean {
        val activeSession = session ?: return false

        if (command.startsWith("TEXT:")) {
            val text = command.removePrefix("TEXT:")
            if (text.isBlank()) {
                InAppDiagnostics.warn(tag, "Samsung TEXT ignored: blank payload")
                return false
            }

            val sent = sendInputText(activeSession, text)
            InAppDiagnostics.info(tag, "Samsung text command result success=$sent")
            return sent
        }

        val isVoiceCommand = command == "KEY_BT_VOICE" || command == "KEY_VOICE" || command == "KEY_MIC"
        return if (isVoiceCommand) {
            val pressed = sendRemoteKey(activeSession, "Press", command)
            delay(180)
            val released = sendRemoteKey(activeSession, "Release", command)
            val success = pressed && released
            InAppDiagnostics.info(tag, "Samsung voice command result: $command success=$success")
            success
        } else {
            sendRemoteKey(activeSession, "Click", command)
        }
    }

    override suspend fun launchApp(appId: String): Boolean {
        val activeSession = session ?: return false
        val nativeLaunch = sendLaunchEvent(activeSession, appId, "NATIVE_LAUNCH")
        val deepLinkLaunch = sendLaunchEvent(activeSession, appId, "DEEP_LINK")
        val restLaunch = launchAppViaHttp(appId)

        val success = nativeLaunch || deepLinkLaunch || restLaunch
        if (success) {
            InAppDiagnostics.info(tag, "Samsung launch accepted appId=$appId")
        } else {
            InAppDiagnostics.error(tag, "Samsung launch failed appId=$appId")
        }
        return success
    }

    private fun encodedClientName(name: String): String {
        val base64Name = Base64.getEncoder().encodeToString(name.toByteArray(Charsets.UTF_8))
        return URLEncoder.encode(base64Name, Charsets.UTF_8.name())
    }

    private fun schemesForPort(port: Int): List<String> {
        return when (port) {
            // Port 8001: Samsung TV, WebSocket thuần (không mã hóa)
            8001 -> listOf("ws")
            // Port 8002: Samsung TV mới, WSS với self-signed cert
            8002 -> listOf("wss")
            // Port 8009: Samsung SmartView/Tizen, cũng dùng WSS
            8009 -> listOf("wss", "ws")
            // Mặc định thử ws trước, wss sau
            else -> listOf("ws", "wss")
        }
    }

    private suspend fun sendRemoteKey(
        session: DefaultClientWebSocketSession,
        cmdType: String,
        command: String,
    ): Boolean {
        val payload = buildJsonObject {
            put("method", "ms.remote.control")
            putJsonObject("params") {
                put("Cmd", cmdType)
                put("DataOfCmd", command)
                put("Option", "false")
                put("TypeOfRemote", "SendRemoteKey")
            }
        }

        return try {
            session.send(Frame.Text(payload.toString()))
            InAppDiagnostics.info(tag, "Samsung command sent: $command mode=$cmdType")
            true
        } catch (e: Exception) {
            InAppDiagnostics.error(tag, "Samsung command failed: $command mode=$cmdType => ${e.message}")
            false
        }
    }

    private suspend fun sendInputText(
        session: DefaultClientWebSocketSession,
        text: String,
    ): Boolean {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
            .replace("\n", " ")
            .replace("\r", " ")

        val chunks = normalized.chunked(22)
        chunks.forEachIndexed { index, chunk ->
            val encoded = Base64.getEncoder().encodeToString(chunk.toByteArray(Charsets.UTF_8))
            val payload = buildJsonObject {
                put("method", "ms.remote.control")
                putJsonObject("params") {
                    put("Cmd", encoded)
                    put("DataOfCmd", "base64")
                    put("Option", "false")
                    put("TypeOfRemote", "SendInputString")
                }
            }

            val sent = try {
                session.send(Frame.Text(payload.toString()))
                true
            } catch (e: Exception) {
                InAppDiagnostics.error(tag, "Samsung text chunk failed: ${e.message}")
                false
            }

            if (!sent) {
                return false
            }

            if (index < chunks.lastIndex) {
                delay(65)
            }
        }

        InAppDiagnostics.info(tag, "Samsung text payload sent chunks=${chunks.size}")
        return true
    }

    private suspend fun sendLaunchEvent(
        session: DefaultClientWebSocketSession,
        appId: String,
        actionType: String,
    ): Boolean {
        val payload = buildJsonObject {
            put("method", "ms.channel.emit")
            putJsonObject("params") {
                put("event", "ed.apps.launch")
                put("to", "host")
                putJsonObject("data") {
                    put("appId", appId)
                    put("action_type", actionType)
                }
            }
        }

        return try {
            session.send(Frame.Text(payload.toString()))
            InAppDiagnostics.info(tag, "Samsung launch frame sent: appId=$appId action=$actionType")
            true
        } catch (e: Exception) {
            InAppDiagnostics.warn(tag, "Samsung launch frame failed: action=$actionType reason=${e.message}")
            false
        }
    }

    private suspend fun launchAppViaHttp(appId: String): Boolean {
        val ip = connectedIp ?: return false
        val port = connectedPort ?: return false

        val urls = buildList {
            if (port == 8001) {
                add("http://$ip:8001/api/v2/applications/$appId")
            }
            if (port == 8002 || port == 8009) {
                add("https://$ip:8002/api/v2/applications/$appId")
            }
            add("http://$ip:8001/api/v2/applications/$appId")
            add("https://$ip:8002/api/v2/applications/$appId")
        }.distinct()

        urls.forEach { endpoint ->
            try {
                val response: HttpResponse = client.post(endpoint)
                if (response.status.value in 200..299) {
                    InAppDiagnostics.info(tag, "Samsung launch HTTP success: $endpoint")
                    return true
                }
            } catch (_: Exception) {
                // Try next endpoint.
            }
        }

        InAppDiagnostics.warn(tag, "Samsung launch HTTP fallback failed appId=$appId")
        return false
    }
}
