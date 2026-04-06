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
        Log.w(TAG, "ADB command bridge is not implemented yet. command=$command")
        false
    }

    override suspend fun launchApp(appId: String): Boolean = withContext(Dispatchers.IO) {
        Log.w(TAG, "ADB app launch bridge is not implemented yet. appId=$appId")
        false
    }
}
