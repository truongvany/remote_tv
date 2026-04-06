package com.example.remote_tv.data.connection

import android.util.Log
import com.example.remote_tv.data.debug.InAppDiagnostics
import com.example.remote_tv.data.model.TVBrand
import com.example.remote_tv.data.model.TVDevice
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

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private var activeProtocol: TVProtocol? = null
    private var lastConnectedDevice: TVDevice? = null

    fun connect(device: TVDevice) {
        lastConnectedDevice = device
        scope.launch {
            activeProtocol?.disconnect()
            activeProtocol = null
            _currentDevice.value = null
            _connectionError.value = null

            Log.d(TAG, "Connecting to ${device.name} at ${device.ipAddress}:${device.port}")
            InAppDiagnostics.info(
                TAG,
                "Connect request: ${device.name} (${device.brand}) ${device.ipAddress}:${device.port}",
            )

            val protocol: TVProtocol? = when (device.brand) {
                TVBrand.SAMSUNG -> connectSamsungTV(device)
                TVBrand.LG -> connectLGTV(device)
                TVBrand.ANDROID_TV -> connectAndroidTV(device)
                TVBrand.UNKNOWN -> connectByPortHeuristics(device)
                else -> null
            }

            if (protocol != null) {
                activeProtocol = protocol
                _currentDevice.value = device.copy(isConnected = true)
                Log.d(TAG, "SUCCESS: Connected to ${device.name}")
                InAppDiagnostics.info(TAG, "Connected: ${device.name} at ${device.ipAddress}:${device.port}")
            } else {
                Log.e(TAG, "FAILED: Could not connect to ${device.ipAddress}")
                _currentDevice.value = null
                _connectionError.value = buildConnectionError(device)
                InAppDiagnostics.error(TAG, "Connect failed: ${device.name} ${device.ipAddress}:${device.port}")
            }
        }
    }

    private suspend fun connectSamsungTV(device: TVDevice): TVProtocol? {
        val protocol = SamsungProtocol(client)
        val candidatePorts = listOf(device.port, 8002, 8001, 8009).distinct()

        candidatePorts.forEach { port ->
            InAppDiagnostics.info(TAG, "Trying Samsung channel ${device.ipAddress}:$port")
            if (protocol.connect(device.ipAddress, port)) {
                Log.d(TAG, "Connected via SamsungProtocol at port $port")
                InAppDiagnostics.info(TAG, "Samsung channel opened on port $port")
                return protocol
            }
        }

        return null
    }

    private suspend fun connectLGTV(device: TVDevice): TVProtocol? {
        val protocol = LGProtocol(client)
        val candidatePorts = listOf(device.port, 3000, 8008).distinct()

        candidatePorts.forEach { port ->
            InAppDiagnostics.info(TAG, "Trying LG channel ${device.ipAddress}:$port")
            if (protocol.connect(device.ipAddress, port)) {
                Log.d(TAG, "Connected via LGProtocol at port $port")
                InAppDiagnostics.info(TAG, "LG channel opened on port $port")
                return protocol
            }
        }

        return null
    }

    private suspend fun connectAndroidTV(device: TVDevice): TVProtocol? {
        // TODO (Real TV): AndroidTVRemoteProtocol hoàn chỉnh cần TLS + certificate pairing.
        // Bản hiện tại chỉ thử handshake TCP ở các cổng Android TV phổ biến.
        val remote = AndroidTVRemoteProtocol()
        val candidatePorts = buildList {
            if (device.port == 6466 || device.port == 6467) {
                add(device.port)
            }
            add(6466)
            add(6467)
        }.distinct()

        candidatePorts.forEach { port ->
            InAppDiagnostics.info(TAG, "Trying AndroidTV remote ${device.ipAddress}:$port")
            if (remote.connect(device.ipAddress, port)) {
                Log.d(TAG, "Connected via AndroidTVRemoteProtocol at port $port")
                InAppDiagnostics.info(TAG, "AndroidTV remote socket connected on port $port")
                return remote
            }
        }

        Log.w(TAG, "AndroidTVRemote connection failed for ${device.ipAddress}")
        InAppDiagnostics.warn(TAG, "AndroidTV remote connect failed for ${device.ipAddress}")
        return null
    }

    private suspend fun connectByPortHeuristics(device: TVDevice): TVProtocol? {
        return when (device.port) {
            8002, 8001 -> connectSamsungTV(device)
            3000, 8008 -> connectLGTV(device)
            6466, 6467 -> connectAndroidTV(device)
            else -> null
        }
    }

    private fun buildConnectionError(device: TVDevice): String {
        return when (device.brand) {
            TVBrand.ANDROID_TV -> {
                if (device.port == 8008 || device.port == 8009) {
                    "This TV was discovered via Google Cast (port ${device.port}). " +
                        "Remote approval popup needs Android TV Remote service on port 6466/6467 and TLS pairing, which is not fully implemented yet."
                } else {
                    "Could not establish Android TV remote session at ${device.ipAddress}. Ensure TV and phone are on same Wi-Fi and remote pairing is enabled on TV."
                }
            }
            TVBrand.SAMSUNG -> "Could not open Samsung remote channel. Make sure Remote Access is allowed on the TV."
            TVBrand.LG -> "LG pairing API is not fully implemented yet in this build."
            else -> "No compatible control protocol for ${device.ipAddress}:${device.port}."
        }
    }

    fun disconnect() {
        scope.launch {
            activeProtocol?.disconnect()
            activeProtocol = null
            _currentDevice.value = null
            _connectionError.value = null
            InAppDiagnostics.info(TAG, "Disconnected active protocol")
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
        InAppDiagnostics.info(TAG, "Send command: $command")
        return if (protocol != null) {
            val success = protocol.sendCommand(command)
            if (!success) {
                Log.e(TAG, "Command failed: $command")
                InAppDiagnostics.error(TAG, "Command failed: $command")
            } else {
                InAppDiagnostics.info(TAG, "Command sent: $command")
            }
            success
        } else {
            Log.e(TAG, "No active connection to send command: $command")
            InAppDiagnostics.error(TAG, "Command blocked (no active connection): $command")
            false
        }
    }

    suspend fun launchApp(appId: String): Boolean {
        return activeProtocol?.launchApp(appId) ?: false
    }
}

