package com.example.remote_tv.data.connection

import android.os.SystemClock
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class TVConnectionManager(private val client: HttpClient) {

    private val TAG = "TVConnection"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentDevice = MutableStateFlow<TVDevice?>(null)
    val currentDevice: StateFlow<TVDevice?> = _currentDevice.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _connectingDeviceKey = MutableStateFlow<String?>(null)
    val connectingDeviceKey: StateFlow<String?> = _connectingDeviceKey.asStateFlow()

    private var activeProtocol: TVProtocol? = null
    private var lastConnectedDevice: TVDevice? = null
    private var connectJob: Job? = null
    private var currentConnectAttemptCount: Int = 0

    fun connect(device: TVDevice) {
        if (_connectingDeviceKey.value != null) {
            InAppDiagnostics.warn(TAG, "[CONNECT_ATTEMPT] Ignored: another connection is in progress")
            return
        }

        val targetKey = device.toConnectionKey()
        lastConnectedDevice = device
        connectJob?.cancel()
        connectJob = scope.launch {
            activeProtocol?.disconnect()
            activeProtocol = null
            _currentDevice.value = null
            _connectionError.value = null
            _connectingDeviceKey.value = targetKey
            currentConnectAttemptCount = 0

            Log.d(TAG, "Connecting to ${device.name} at ${device.ipAddress}:${device.port}")
            InAppDiagnostics.info(
                TAG,
                "Connect request: ${device.name} (${device.brand}) ${device.ipAddress}:${device.port}",
            )

            try {
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
                    InAppDiagnostics.info(TAG, "[CONNECT_SUCCESS] ${device.name} ${device.ipAddress}:${device.port}")
                } else {
                    Log.e(TAG, "FAILED: Could not connect to ${device.ipAddress}")
                    _currentDevice.value = null
                    _connectionError.value = buildConnectionError(device)
                    InAppDiagnostics.error(TAG, "[CONNECT_FAIL] ${device.name} ${device.ipAddress}:${device.port}")
                }
            } finally {
                if (_connectingDeviceKey.value == targetKey) {
                    _connectingDeviceKey.value = null
                }
            }
        }
    }

    private suspend fun connectSamsungTV(device: TVDevice): TVProtocol? {
        val candidatePorts = listOf(device.port, 8002, 8001, 8009).distinct()

        repeat(ConnectionPolicy.MAX_CONNECT_ROUNDS) { roundIndex ->
            val round = roundIndex + 1
            candidatePorts.forEach { port ->
                val protocol = SamsungProtocol(client)
                val connected = attemptConnect(
                    protocolName = "Samsung",
                    ipAddress = device.ipAddress,
                    port = port,
                    round = round,
                    connectAction = { protocol.connect(device.ipAddress, port) }
                )

                if (connected) {
                    Log.d(TAG, "Connected via SamsungProtocol at port $port")
                    return protocol
                }
            }

            if (round < ConnectionPolicy.MAX_CONNECT_ROUNDS) {
                delay(ConnectionPolicy.backoffMs(round))
            }
        }

        return null
    }

    private suspend fun connectLGTV(device: TVDevice): TVProtocol? {
        val candidatePorts = listOf(device.port, 3000, 8008).distinct()

        repeat(ConnectionPolicy.MAX_CONNECT_ROUNDS) { roundIndex ->
            val round = roundIndex + 1
            candidatePorts.forEach { port ->
                val protocol = LGProtocol(client)
                val connected = attemptConnect(
                    protocolName = "LG",
                    ipAddress = device.ipAddress,
                    port = port,
                    round = round,
                    connectAction = { protocol.connect(device.ipAddress, port) }
                )

                if (connected) {
                    Log.d(TAG, "Connected via LGProtocol at port $port")
                    return protocol
                }
            }

            if (round < ConnectionPolicy.MAX_CONNECT_ROUNDS) {
                delay(ConnectionPolicy.backoffMs(round))
            }
        }

        return null
    }

    private suspend fun connectAndroidTV(device: TVDevice): TVProtocol? {
        // TODO (Real TV): AndroidTVRemoteProtocol hoàn chỉnh cần TLS + certificate pairing.
        // Bản hiện tại chỉ thử handshake TCP ở các cổng Android TV phổ biến.
        val candidatePorts = buildList {
            if (device.port == 6466 || device.port == 6467) {
                add(device.port)
            }
            add(6466)
            add(6467)
        }.distinct()

        repeat(ConnectionPolicy.MAX_CONNECT_ROUNDS) { roundIndex ->
            val round = roundIndex + 1
            candidatePorts.forEach { port ->
                val remote = AndroidTVRemoteProtocol()
                val connected = attemptConnect(
                    protocolName = "AndroidTV",
                    ipAddress = device.ipAddress,
                    port = port,
                    round = round,
                    connectAction = { remote.connect(device.ipAddress, port) }
                )

                if (connected) {
                    Log.d(TAG, "Connected via AndroidTVRemoteProtocol at port $port")
                    return remote
                }
            }

            if (round < ConnectionPolicy.MAX_CONNECT_ROUNDS) {
                delay(ConnectionPolicy.backoffMs(round))
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
        } + " Attempts=$currentConnectAttemptCount, timeout=${ConnectionPolicy.CONNECT_TIMEOUT_MS}ms."
    }

    private suspend fun attemptConnect(
        protocolName: String,
        ipAddress: String,
        port: Int,
        round: Int,
        connectAction: suspend () -> Boolean,
    ): Boolean {
        currentConnectAttemptCount += 1
        val startTime = SystemClock.elapsedRealtime()
        InAppDiagnostics.info(
            TAG,
            "[CONNECT_ATTEMPT] $protocolName round=$round target=$ipAddress:$port timeout=${ConnectionPolicy.CONNECT_TIMEOUT_MS}ms"
        )

        val connected = withTimeoutOrNull(ConnectionPolicy.CONNECT_TIMEOUT_MS) {
            connectAction()
        } ?: false

        val elapsed = SystemClock.elapsedRealtime() - startTime
        if (connected) {
            InAppDiagnostics.info(
                TAG,
                "[CONNECT_ATTEMPT] success $protocolName $ipAddress:$port elapsed=${elapsed}ms"
            )
        } else {
            InAppDiagnostics.warn(
                TAG,
                "[CONNECT_ATTEMPT] failed $protocolName $ipAddress:$port elapsed=${elapsed}ms"
            )
        }

        return connected
    }

    fun disconnect() {
        scope.launch {
            activeProtocol?.disconnect()
            activeProtocol = null
            _currentDevice.value = null
            _connectionError.value = null
            _connectingDeviceKey.value = null
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
        InAppDiagnostics.info(TAG, "[COMMAND_SEND] request=$command")
        return if (protocol != null) {
            val success = protocol.sendCommand(command)
            if (!success) {
                Log.e(TAG, "Command failed: $command")
                InAppDiagnostics.error(TAG, "[COMMAND_SEND] failed=$command")
            } else {
                InAppDiagnostics.info(TAG, "[COMMAND_SEND] success=$command")
            }
            success
        } else {
            Log.e(TAG, "No active connection to send command: $command")
            InAppDiagnostics.error(TAG, "[COMMAND_SEND] blocked_no_session=$command")
            false
        }
    }

    private fun TVDevice.toConnectionKey(): String = "$ipAddress:$port"

    suspend fun launchApp(appId: String): Boolean {
        return activeProtocol?.launchApp(appId) ?: false
    }
}

