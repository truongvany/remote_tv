package com.example.remote_tv.data.protocol

import com.example.remote_tv.data.debug.InAppDiagnostics
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.url
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

class SamsungProtocol(private val client: HttpClient) : TVProtocol {
    private val tag = "SamsungProtocol"
    private var session: DefaultClientWebSocketSession? = null

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
    }

    override suspend fun sendCommand(command: String): Boolean {
        val activeSession = session ?: return false

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
        val payload = buildJsonObject {
            put("method", "ms.channel.emit")
            putJsonObject("params") {
                put("event", "ed.apps.launch")
                put("to", "host")
                putJsonObject("data") {
                    put("appId", appId)
                    put("action_type", "NATIVE_LAUNCH")
                }
            }
        }
        return try {
            activeSession.send(Frame.Text(payload.toString()))
            InAppDiagnostics.info(tag, "Samsung launch sent: $appId")
            true
        } catch (e: Exception) {
            InAppDiagnostics.error(tag, "Samsung launch failed: $appId => ${e.message}")
            false
        }
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
}
