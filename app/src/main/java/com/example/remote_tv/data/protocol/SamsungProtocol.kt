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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class SamsungProtocol(private val client: HttpClient) : TVProtocol {
    private val tag = "SamsungProtocol"
    private var session: DefaultClientWebSocketSession? = null

    override suspend fun connect(ip: String, port: Int): Boolean {
        return try {
            val scheme = if (port == 8002) "wss" else "ws"
            val encodedName = encodedClientName("REMOTE_TV")
            val url = "$scheme://$ip:$port/api/v2/channels/samsung.remote.control?name=$encodedName"
            InAppDiagnostics.info(tag, "Open Samsung socket: $url")

            session = client.webSocketSession {
                url(url)
                header("Origin", "http://$ip")
            }

            InAppDiagnostics.info(tag, "Samsung socket connected at $ip:$port")
            true
        } catch (e: Exception) {
            InAppDiagnostics.error(tag, "Samsung connect exception at $ip:$port => ${e::class.simpleName}: ${e.message}")
            false
        }
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
        val payload = buildJsonObject {
            put("method", "ms.remote.control")
            putJsonObject("params") {
                put("Cmd", "Click")
                put("DataOfCmd", command)
                put("Option", "false")
                put("TypeOfRemote", "SendRemoteKey")
            }
        }
        return try {
            activeSession.send(Frame.Text(payload.toString()))
            InAppDiagnostics.info(tag, "Samsung command sent: $command")
            true
        } catch (e: Exception) {
            InAppDiagnostics.error(tag, "Samsung command failed: $command => ${e.message}")
            false
        }
    }

    override suspend fun launchApp(appId: String): Boolean {
        val activeSession = session ?: return false
        val payload = buildJsonObject {
            put("method", "ms.channel.emit")
            putJsonObject("params") {
                put("event", "ed.apps.launch")
                putJsonObject("data") {
                    put("appId", appId)
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
}
