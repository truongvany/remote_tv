package com.example.remote_tv.data.connection

import android.util.Log
import com.example.remote_tv.data.model.TVBrand
import com.example.remote_tv.data.model.TVDevice
import com.example.remote_tv.data.protocol.AdbProtocol
import com.example.remote_tv.data.protocol.AndroidTVRemoteProtocol
import com.example.remote_tv.data.protocol.LGProtocol
import com.example.remote_tv.data.protocol.SamsungProtocol
import com.example.remote_tv.data.protocol.TVProtocol
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TVConnectionManager(private val client: HttpClient) {

    private val TAG = "TVConnection"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentDevice = MutableStateFlow<TVDevice?>(null)
    val currentDevice: StateFlow<TVDevice?> = _currentDevice.asStateFlow()

    private var activeProtocol: TVProtocol? = null
    private var lastConnectedDevice: TVDevice? = null

    fun connect(device: TVDevice) {
        lastConnectedDevice = device
        scope.launch {
            activeProtocol?.disconnect()
            activeProtocol = null
            _currentDevice.value = null

            Log.d(TAG, "Connecting to ${device.name} at ${device.ipAddress}:${device.port}")

            val protocol: TVProtocol? = when (device.brand) {
                TVBrand.SAMSUNG -> SamsungProtocol(client)
                TVBrand.LG -> LGProtocol(client)
                TVBrand.ANDROID_TV -> connectAndroidTV(device)
                else -> null
            }

            if (protocol != null) {
                activeProtocol = protocol
                _currentDevice.value = device.copy(isConnected = true)
                Log.d(TAG, "SUCCESS: Connected to ${device.name}")
            } else {
                Log.e(TAG, "FAILED: Could not connect to ${device.ipAddress}")
                _currentDevice.value = null
            }
        }
    }

    private suspend fun connectAndroidTV(device: TVDevice): TVProtocol? {
        // TODO (Real TV): AndroidTVRemoteProtocol cần TLS + certificate pairing
        // Hiện tại dùng raw TCP — hoạt động với emulator qua ADB port-forward
        val remote = AndroidTVRemoteProtocol()
        val remotePort = if (device.port == 8009) 6466 else device.port
        if (remote.connect(device.ipAddress, remotePort)) {
            Log.d(TAG, "Connected via AndroidTVRemoteProtocol at port $remotePort")
            return remote
        }
        // Fallback: ADB port 5555
        Log.w(TAG, "AndroidTVRemote failed, trying ADB at port 5555...")
        val adb = AdbProtocol()
        return if (adb.connect(device.ipAddress, 5555)) adb else null
    }

    fun disconnect() {
        scope.launch {
            activeProtocol?.disconnect()
            activeProtocol = null
            _currentDevice.value = null
        }
    }

    fun scheduleReconnect() {
        val device = lastConnectedDevice ?: return
        scope.launch {
            Log.d(TAG, "Reconnecting to ${device.name} in 5s...")
            delay(5000)
            if (_currentDevice.value == null) {
                Log.d(TAG, "Attempting reconnect to ${device.name}")
                connect(device)
            }
        }
    }

    suspend fun sendCommand(command: String): Boolean {
        val protocol = activeProtocol
        return if (protocol != null) {
            val success = protocol.sendCommand(command)
            if (!success) Log.e(TAG, "Command failed: $command")
            success
        } else {
            Log.e(TAG, "No active connection to send command: $command")
            false
        }
    }

    suspend fun launchApp(appId: String): Boolean {
        return activeProtocol?.launchApp(appId) ?: false
    }
}

