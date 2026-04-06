package com.example.remote_tv.data.protocol

import android.util.Log
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdbProtocol : TVProtocol {
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private val TAG = "AdbProtocol"

    override suspend fun connect(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to ADB at $ip:$port...")
            socket = Socket()
            socket?.connect(InetSocketAddress(ip, port), 3000)
            outputStream = socket?.getOutputStream()
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) { }
        socket = null
        outputStream = null
    }

    override suspend fun sendCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        val keyCode = when (command.uppercase()) {
            "UP", "MOVE_UP" -> 19
            "DOWN", "MOVE_DOWN" -> 20
            "LEFT", "MOVE_LEFT" -> 21
            "RIGHT", "MOVE_RIGHT" -> 22
            "OK", "KEY_ENTER", "ENTER" -> 66
            "KEY_BACK", "BACK" -> 4
            "KEY_HOME", "HOME" -> 3
            "KEY_MENU", "MENU" -> 82
            "KEY_SEARCH", "SEARCH" -> 84
            "KEY_VOICE", "VOICE" -> 231 // KEYCODE_VOICE_ASSIST
            "KEY_VOL_UP" -> 24
            "KEY_VOL_DOWN" -> 25
            "KEY_MUTE" -> 164
            "KEY_POWER" -> 26
            else -> return@withContext false
        }

        return@withContext try {
            // Trong thực tế, lệnh ADB sẽ là "input keyevent <keycode>"
            // Ở đây ta giả định stream đã sẵn sàng nhận lệnh shell hoặc qua ADB protocol
            Log.d(TAG, "ADB Sending KeyCode: $keyCode for command: $command")
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun launchApp(appId: String): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "ADB Launching App: $appId via 'monkey -p $appId 1'")
        true
    }
}
