package com.example.remote_tv.data.protocol

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class SamsungProtocol(private val client: HttpClient) : TVProtocol {
    private var session: DefaultClientWebSocketSession? = null

    override suspend fun connect(ip: String, port: Int): Boolean {
        return try {
            val url = "ws://$ip:$port/api/v2/channels/samsung.remote.control?name=REMOTE_UED"
            session = client.webSocketSession(url)
            true
        } catch (e: Exception) {
            e.printStackTrace()
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
            session?.send(Frame.Text(payload.toString()))
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun launchApp(appId: String): Boolean {
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
            session?.send(Frame.Text(payload.toString()))
            true
        } catch (e: Exception) {
            false
        }
    }
}
