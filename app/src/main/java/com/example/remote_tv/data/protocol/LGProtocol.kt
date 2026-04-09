package com.example.remote_tv.data.protocol

import android.util.Log
import com.example.remote_tv.data.debug.InAppDiagnostics
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.UUID

/**
 * LG WebOS TV Protocol — Điều khiển qua WebSocket port 3000.
 *
 * Flow kết nối:
 * 1. WS connect ws://ip:3000
 * 2. Gửi register message → nhận client-key từ TV (lần đầu TV hiện dialog xác nhận)
 * 3. Dùng client-key cho các lần sau (pairing được nhớ)
 *
 * Tham chiếu: https://webostv.developer.lge.com/develop/app-dev/web-api-list/
 */
class LGProtocol(private val client: HttpClient) : TVProtocol {

    private val TAG = "LGProtocol"
    private var session: DefaultClientWebSocketSession? = null
    private var clientKey: String? = null
    private val json = Json { ignoreUnknownKeys = true }

    // ----------------------------------------------------------------
    // LG WebOS SSAP URIs
    // ----------------------------------------------------------------
    companion object {
        // Remote key URIs
        const val URI_BUTTON = "ssap://com.webos.service.ime/sendKeyEvent"
        const val URI_ENTER = "ssap://com.webos.service.ime/sendEnterKey"
        const val URI_DELETE = "ssap://com.webos.service.ime/deleteCharacters"
        const val URI_TYPE = "ssap://com.webos.service.ime/insertText"
        const val URI_VOLUME_UP = "ssap://audio/volumeUp"
        const val URI_VOLUME_DOWN = "ssap://audio/volumeDown"
        const val URI_MUTE = "ssap://audio/setMute"
        const val URI_CHANNEL_UP = "ssap://tv/channelUp"
        const val URI_CHANNEL_DOWN = "ssap://tv/channelDown"
        const val URI_POWER_OFF = "ssap://system/turnOff"
        const val URI_LAUNCH_APP = "ssap://com.webos.applicationManager/launch"
        const val URI_LIST_APPS = "ssap://com.webos.applicationManager/listApps"
        const val URI_HOME = "ssap://home/showDashboard"
        const val URI_TOAST = "ssap://system.notifications/createToast"
        const val URI_MEDIA_PLAY = "ssap://media.controls/play"
        const val URI_MEDIA_PAUSE = "ssap://media.controls/pause"
        const val URI_MEDIA_STOP = "ssap://media.controls/stop"
        const val URI_MEDIA_REWIND = "ssap://media.controls/rewind"
        const val URI_MEDIA_FORWARD = "ssap://media.controls/fastForward"

        // Key names cho URI_BUTTON
        private val KEY_MAP = mapOf(
            "KEY_BACK" to "BACK",
            "KEY_HOME" to "HOME",
            "KEY_UP" to "UP",
            "KEY_DOWN" to "DOWN",
            "KEY_LEFT" to "LEFT",
            "KEY_RIGHT" to "RIGHT",
            "UP" to "UP",
            "DOWN" to "DOWN",
            "LEFT" to "LEFT",
            "RIGHT" to "RIGHT",
            "OK" to "ENTER",
            "KEY_ENTER" to "ENTER",
            "KEY_MUTE" to "MUTE",
            "KEY_VOL_UP" to "VOLUMEUP",
            "KEY_VOL_DOWN" to "VOLUMEDOWN",
            "KEY_MENU" to "MENU",
            "KEY_INFO" to "INFO",
            "KEY_POWER" to "POWER",
            "KEY_PLAY" to "PLAY",
            "KEY_PAUSE" to "PAUSE",
            "KEY_STOP" to "STOP",
            "KEY_PLAY_PAUSE" to "PLAY",
            "KEY_CHANNEL_UP" to "CHANNELUP",
            "KEY_CHANNEL_DOWN" to "CHANNELDOWN",
        )
    }

    override suspend fun connect(ip: String, port: Int): Boolean {
        val targetPort = if (port == 3000) 3000 else 3000 // LG luôn dùng 3000
        return try {
            InAppDiagnostics.info(TAG, "LG connect → ws://$ip:$targetPort")
            val ws = client.webSocketSession("ws://$ip:$targetPort")
            session = ws
            val registered = performRegistration(ws)
            if (registered) {
                InAppDiagnostics.info(TAG, "LG registered successfully. clientKey=${clientKey?.take(8)}...")
            } else {
                InAppDiagnostics.warn(TAG, "LG registration incomplete (may need TV confirmation)")
            }
            registered
        } catch (e: Exception) {
            InAppDiagnostics.error(TAG, "LG connect failed: ${e.message}")
            session = null
            false
        }
    }

    override suspend fun disconnect() {
        try { session?.close() } catch (_: Exception) {}
        session = null
    }

    override suspend fun sendCommand(command: String): Boolean {
        if (command.startsWith("TEXT:")) {
            val text = command.removePrefix("TEXT:")
            return sendLgRequest(URI_TYPE, buildJsonObject { put("text", text); put("replace", false) })
        }

        // Xử lý Volume/Mute qua SSAP direct
        return when (command) {
            "KEY_VOL_UP" -> sendLgRequest(URI_VOLUME_UP)
            "KEY_VOL_DOWN" -> sendLgRequest(URI_VOLUME_DOWN)
            "KEY_MUTE" -> sendLgRequest(URI_MUTE, buildJsonObject { put("mute", true) })
            "KEY_CHANNEL_UP" -> sendLgRequest(URI_CHANNEL_UP)
            "KEY_CHANNEL_DOWN" -> sendLgRequest(URI_CHANNEL_DOWN)
            "KEY_POWER" -> sendLgRequest(URI_POWER_OFF)
            "KEY_HOME" -> sendLgRequest(URI_HOME)
            "KEY_ENTER", "OK" -> sendLgRequest(URI_ENTER)
            "KEY_PLAY" -> sendLgRequest(URI_MEDIA_PLAY)
            "KEY_PAUSE" -> sendLgRequest(URI_MEDIA_PAUSE)
            "KEY_STOP" -> sendLgRequest(URI_MEDIA_STOP)
            "KEY_PLAY_PAUSE" -> sendLgRequest(URI_MEDIA_PLAY)
            else -> {
                // Fallback: gửi button event
                val lgKey = KEY_MAP[command] ?: command
                sendLgRequest(URI_BUTTON, buildJsonObject { put("keyCode", lgKey) })
            }
        }
    }

    override suspend fun launchApp(appId: String): Boolean {
        return sendLgRequest(URI_LAUNCH_APP, buildJsonObject { put("id", appId) })
    }

    // ----------------------------------------------------------------
    // Registration (Pairing)
    // ----------------------------------------------------------------

    private suspend fun performRegistration(ws: DefaultClientWebSocketSession): Boolean {
        val registerPayload = buildJsonObject {
            put("type", "register")
            put("id", UUID.randomUUID().toString())
            putJsonObject("payload") {
                put("forcePairing", false)
                put("pairingType", "PROMPT")
                clientKey?.let { put("client-key", it) }
                putJsonObject("manifest") {
                    put("manifestVersion", 1)
                    putJsonObject("appVersion") { put("major", 2); put("minor", 0) }
                    put("signed", buildJsonObject {
                        put("created", "20140509")
                        put("appId", "com.example.remote_tv")
                        put("vendorId", "com.example")
                        put("localizedAppNames", buildJsonObject {
                            put("", "Remote TV")
                        })
                        put("localizedVendorNames", buildJsonObject {
                            put("", "Example")
                        })
                        put("permissions", kotlinx.serialization.json.buildJsonArray {
                            add(kotlinx.serialization.json.JsonPrimitive("READ_INSTALLED_APPS"))
                            add(kotlinx.serialization.json.JsonPrimitive("READ_LGE_TV_INPUT_EVENTS"))
                            add(kotlinx.serialization.json.JsonPrimitive("READ_TV_CURRENT_TIME"))
                        })
                    })
                    put("permissions", kotlinx.serialization.json.buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive("LAUNCH"))
                        add(kotlinx.serialization.json.JsonPrimitive("LAUNCH_WEBAPP"))
                        add(kotlinx.serialization.json.JsonPrimitive("APP_TO_APP"))
                        add(kotlinx.serialization.json.JsonPrimitive("CLOSE"))
                        add(kotlinx.serialization.json.JsonPrimitive("TEST_OPEN"))
                        add(kotlinx.serialization.json.JsonPrimitive("TEST_PROTECTED"))
                        add(kotlinx.serialization.json.JsonPrimitive("CONTROL_TV_SCREEN"))
                        add(kotlinx.serialization.json.JsonPrimitive("CONTROL_VOLUME"))
                        add(kotlinx.serialization.json.JsonPrimitive("CONTROL_INPUT_TEXT"))
                        add(kotlinx.serialization.json.JsonPrimitive("READ_INSTALLED_APPS"))
                    })
                }
            }
        }

        ws.send(Frame.Text(registerPayload.toString()))
        InAppDiagnostics.info(TAG, "LG register sent, waiting for TV response...")

        // Chờ tối đa 30s (người dùng cần bấm OK trên TV dialog)
        return try {
            withTimeoutOrNull(30_000L) {
                for (frame in ws.incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        val msgType = parseMessageType(text)
                        val msgPayload = parsePayload(text)

                        when (msgType) {
                            "registered" -> {
                                clientKey = msgPayload?.get("client-key")?.jsonPrimitive?.content
                                InAppDiagnostics.info(TAG, "LG registered! clientKey=${clientKey?.take(8)}...")
                                return@withTimeoutOrNull true
                            }
                            "error" -> {
                                InAppDiagnostics.error(TAG, "LG registration error: $text")
                                return@withTimeoutOrNull false
                            }
                            "response" -> {
                                // Có thể cần confirm thêm
                                InAppDiagnostics.info(TAG, "LG registration response: $text")
                            }
                        }
                    }
                }
                false
            } ?: false
        } catch (e: TimeoutCancellationException) {
            InAppDiagnostics.warn(TAG, "LG registration timed out (user may not have confirmed)")
            false
        }
    }

    // ----------------------------------------------------------------
    // Send SSAP request and await response
    // ----------------------------------------------------------------

    private suspend fun sendLgRequest(uri: String, payload: JsonObject? = null): Boolean {
        val ws = session ?: return false
        val msgId = UUID.randomUUID().toString().take(8)

        val msg = buildJsonObject {
            put("type", "request")
            put("id", msgId)
            put("uri", uri)
            payload?.let { put("payload", it) }
        }

        return try {
            ws.send(Frame.Text(msg.toString()))
            InAppDiagnostics.info(TAG, "LG request sent: uri=$uri id=$msgId")

            // Chờ response tối đa 3s
            val success = withTimeoutOrNull(3_000L) {
                for (frame in ws.incoming) {
                    if (frame !is Frame.Text) continue
                    val text = frame.readText()
                    val responseId = parseId(text)
                    if (responseId == msgId) {
                        val returnValue = parseReturnValue(text)
                        InAppDiagnostics.info(TAG, "LG response: id=$msgId returnValue=$returnValue")
                        return@withTimeoutOrNull returnValue
                    }
                }
                false
            } ?: false

            success
        } catch (e: Exception) {
            InAppDiagnostics.error(TAG, "LG request failed uri=$uri: ${e.message}")
            false
        }
    }

    // ----------------------------------------------------------------
    // JSON parsing helpers
    // ----------------------------------------------------------------

    private fun parseMessageType(text: String): String? {
        return try {
            json.parseToJsonElement(text).jsonObject["type"]?.jsonPrimitive?.content
        } catch (_: Exception) { null }
    }

    private fun parsePayload(text: String): JsonObject? {
        return try {
            json.parseToJsonElement(text).jsonObject["payload"]?.jsonObject
        } catch (_: Exception) { null }
    }

    private fun parseId(text: String): String? {
        return try {
            json.parseToJsonElement(text).jsonObject["id"]?.jsonPrimitive?.content
        } catch (_: Exception) { null }
    }

    private fun parseReturnValue(text: String): Boolean {
        return try {
            val root = json.parseToJsonElement(text).jsonObject
            val payload = root["payload"]?.jsonObject
            val returnValue = payload?.get("returnValue")?.jsonPrimitive?.content
            returnValue == "true" || returnValue == null // null means no error field
        } catch (_: Exception) { true }
    }
}
