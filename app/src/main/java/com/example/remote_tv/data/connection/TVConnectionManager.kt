package com.example.remote_tv.data.connection

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.example.remote_tv.data.adb.WirelessAdbConnectionManager
import com.example.remote_tv.data.debug.InAppDiagnostics
import com.example.remote_tv.data.model.AppLaunchResult
import com.example.remote_tv.data.model.TVBrand
import com.example.remote_tv.data.model.TVDevice
import com.example.remote_tv.data.model.PlaybackState
import com.example.remote_tv.data.protocol.AdbProtocol
import com.example.remote_tv.data.protocol.AndroidTVRemoteProtocol
import com.example.remote_tv.data.protocol.LGProtocol
import com.example.remote_tv.data.protocol.SamsungProtocol
import com.example.remote_tv.data.protocol.TVProtocol
import com.example.remote_tv.data.protocol.WirelessAdbProtocol
import io.ktor.client.HttpClient
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class TVConnectionManager(context: Context, private val client: HttpClient) {

    private val TAG = "TVConnection"
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentDevice = MutableStateFlow<TVDevice?>(null)
    val currentDevice: StateFlow<TVDevice?> = _currentDevice.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _connectingDeviceKey = MutableStateFlow<String?>(null)
    val connectingDeviceKey: StateFlow<String?> = _connectingDeviceKey.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var activeProtocol: TVProtocol? = null
    private var lastConnectedDevice: TVDevice? = null
    private var connectJob: Job? = null
    private var currentConnectAttemptCount: Int = 0
    private var lastAdbTargetPort: Int? = null
    private val wirelessAdbManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        WirelessAdbConnectionManager.getInstance(appContext)
    }

    private fun getWirelessAdbManagerOrNull(): WirelessAdbConnectionManager? {
        return try {
            wirelessAdbManager
        } catch (t: Throwable) {
            InAppDiagnostics.error(TAG, "[ADB_PAIR] manager_init_error=${t.message}")
            null
        }
    }

    fun connect(device: TVDevice) {
        if (_connectingDeviceKey.value != null) {
            InAppDiagnostics.warn(TAG, "[CONNECT_ATTEMPT] Cancel previous pending connect and start new target")
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
            _playbackState.value = PlaybackState.IDLE
            currentConnectAttemptCount = 0
            lastAdbTargetPort = null

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
                    _playbackState.value = PlaybackState.IDLE
                    Log.d(TAG, "SUCCESS: Connected to ${device.name}")
                    InAppDiagnostics.info(TAG, "[CONNECT_SUCCESS] ${device.name} ${device.ipAddress}:${device.port}")
                } else {
                    Log.e(TAG, "FAILED: Could not connect to ${device.ipAddress}")
                    _currentDevice.value = null
                    _playbackState.value = PlaybackState.IDLE
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
        // Bước 1: Thử Samsung WebSocket Remote API (Tizen TV ports)
        val standardPorts = listOf(8001, 8002)
        val candidatePorts = if (device.port in standardPorts) {
            standardPorts
        } else {
            (standardPorts + device.port).distinct()
        }

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

        // Bước 2: Samsung WebSocket thất bại
        // Fallback sang ADB (port 5555) — cần timeout lớn hơn do TV có thể hiện dialog xác nhận
        InAppDiagnostics.warn(TAG, "Samsung WebSocket failed — trying ADB fallback at ${device.ipAddress}:5555")
        return connectViaAdb(device)
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
        val candidatePorts = adbCandidatePortsFor(device)
        // remote2 TLS pairing (6466/6467) is not complete in this project yet.
        // To avoid false-positive "CONNECTED" state, use ADB as the only control-ready path for Android TV.
        InAppDiagnostics.warn(
            TAG,
            "AndroidTV remote2 pairing is not fully implemented. Using ADB control path at ${device.ipAddress}:${candidatePorts.joinToString("/")}"
        )
        return connectViaAdb(device, preferredPorts = candidatePorts)
    }

    private suspend fun connectByPortHeuristics(device: TVDevice): TVProtocol? {
        return when (device.port) {
            8002, 8001 -> connectSamsungTV(device)
            3000 -> connectLGTV(device)
            8008 -> connectAndroidTV(device)
            8009 -> connectAndroidOrSamsungFromCastPort(device)
            6466, 6467 -> connectAndroidTV(device)
            5555 -> connectViaAdb(device)  // ADB over TCP
            else -> null
        }
    }

    private suspend fun connectAndroidOrSamsungFromCastPort(device: TVDevice): TVProtocol? {
        val androidPath = connectAndroidTV(device)
        if (androidPath != null) {
            return androidPath
        }

        InAppDiagnostics.warn(
            TAG,
            "Port 8009 is ambiguous (Cast/Samsung). Android path failed, trying Samsung fallback."
        )
        return connectSamsungTV(device.copy(brand = TVBrand.SAMSUNG))
    }

    private suspend fun connectViaAdb(
        device: TVDevice,
        preferredPorts: List<Int> = adbCandidatePortsFor(device),
    ): TVProtocol? {
        val candidatePorts = preferredPorts.distinct().filter { it in 1..65535 }
        if (candidatePorts.isEmpty()) {
            return null
        }

        candidatePorts.forEach { adbPort ->
            lastAdbTargetPort = adbPort

            if (shouldRunAdbPortPrecheck(device.ipAddress)) {
                val adbPortOpen = isTcpPortOpen(device.ipAddress, adbPort, timeoutMs = 650)
                if (!adbPortOpen) {
                    InAppDiagnostics.warn(
                        TAG,
                        "[ADB_CONNECT] precheck failed: ${device.ipAddress}:$adbPort is closed/unreachable"
                    )
                    return@forEach
                }
            }

            val adb = AdbProtocol()
            currentConnectAttemptCount++
            val startTime = android.os.SystemClock.elapsedRealtime()
            InAppDiagnostics.info(
                TAG,
                "[ADB_CONNECT] target=${device.ipAddress}:$adbPort timeout=35000ms (bao gồm thời gian chờ user chấp nhận trên TV)"
            )
            // Timeout 35s: 3s TCP connect + 4s initial auth + 30s chờ user bấm OK trên TV
            val connected = withTimeoutOrNull(35_000L) {
                adb.connect(device.ipAddress, adbPort)
            } ?: false
            val elapsed = android.os.SystemClock.elapsedRealtime() - startTime
            if (connected) {
                InAppDiagnostics.info(TAG, "[ADB_CONNECT] success target=${device.ipAddress}:$adbPort elapsed=${elapsed}ms")
                return adb
            }

            InAppDiagnostics.warn(TAG, "[ADB_CONNECT] failed target=${device.ipAddress}:$adbPort elapsed=${elapsed}ms")
            runCatching { adb.disconnect() }
        }

        return null
    }

    private fun adbCandidatePortsFor(device: TVDevice): List<Int> {
        val ports = mutableListOf<Int>()
        val requestedPort = device.port
        val looksLikeAdbConnectPort = requestedPort == 5555 || requestedPort in 30000..65535

        if (looksLikeAdbConnectPort) {
            ports += requestedPort
        }

        if (5555 !in ports) {
            ports += 5555
        }

        return ports
    }

    private fun shouldRunAdbPortPrecheck(ipAddress: String): Boolean {
        val normalized = ipAddress.trim().substringBefore('%')
        return normalized.contains('.')
    }

    private fun isTcpPortOpen(ipAddress: String, port: Int, timeoutMs: Int): Boolean {
        return runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ipAddress, port), timeoutMs)
            }
            true
        }.getOrElse { false }
    }

    private fun buildConnectionError(device: TVDevice): String {
        return when (device.brand) {
            TVBrand.ANDROID_TV -> {
                if (device.ipAddress.startsWith("fe80", ignoreCase = true)) {
                    "TV đang trả địa chỉ IPv6 link-local (${device.ipAddress}) nên kết nối có thể thất bại nếu thiếu scope mạng. " +
                        "Hãy refresh để chọn thiết bị có IPv4 (thường dạng 10.x/192.168.x), hoặc đảm bảo điện thoại và TV cùng Wi-Fi rồi thử lại."
                } else
                if (device.port == 8008 || device.port == 8009) {
                    "TV này được phát hiện qua Google Cast (port ${device.port}). " +
                        "Google Cast chỉ dùng để phát hiện thiết bị, không phải kênh điều khiển remote. " +
                        "Hãy bật ADB Debugging trên TV: Settings → Hệ thống → Thông tin → bấm Build Number 7 lần → Developer Options → USB Debugging: ON → Network Debugging: ON."
                } else if (device.port == 6466 || device.port == 6467) {
                    "Đã phát hiện Android TV Remote service (port ${device.port}) nhưng luồng TLS pairing remote2 chưa hoàn chỉnh trong phiên bản này. " +
                        "Vui lòng bật ADB Debugging + Network Debugging để app điều khiển qua ADB. " +
                        "Nếu TV chỉ có Wireless Debugging dạng pair code, hãy ghép đôi trước rồi nhập đúng cổng kết nối (có thể không phải 5555)."
                } else {
                    val adbPortHint = lastAdbTargetPort ?: 5555
                    "Không kết nối được Android TV tại ${device.ipAddress}. Đảm bảo TV và điện thoại cùng WiFi, sau đó bật USB Debugging + Network Debugging trên TV để điều khiển qua ADB. " +
                        "Cổng vừa thử: $adbPortHint (nhiều TV dùng cổng Wireless Debugging khác 5555)."
                }
            }
            TVBrand.SAMSUNG -> {
                "Không mở được kênh Samsung Remote (port 8001/8002) và ADB (port 5555) đều thất bại. " +
                "Nếu TV chạy Android TV: Settings → Hệ thống → Thông tin → bấm Build Number 7 lần → Developer Options → USB Debugging + Network Debugging: ON."
            }
            TVBrand.LG -> "LG pairing API chưa hoàn chỉnh trong phiên bản này."
            else -> "Không có protocol phù hợp cho ${device.ipAddress}:${device.port}."
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
            _playbackState.value = PlaybackState.IDLE
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

    suspend fun pairAndConnectAndroidTv(
        device: TVDevice,
        pairPort: Int,
        pairingCode: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val sanitizedCode = pairingCode.trim().filter { it.isDigit() }
        if (sanitizedCode.length < 6) {
            _connectionError.value = "Mã pair không hợp lệ. Hãy nhập mã 6 số trên TV."
            return@withContext false
        }

        val targetKey = device.toConnectionKey()
        connectJob?.cancel()

        activeProtocol?.disconnect()
        activeProtocol = null
        _currentDevice.value = null
        _connectionError.value = null
        _connectingDeviceKey.value = targetKey
        _playbackState.value = PlaybackState.IDLE
        currentConnectAttemptCount = 0
        lastAdbTargetPort = device.port

        InAppDiagnostics.info(
            TAG,
            "[ADB_PAIR] start target=${device.ipAddress}:${device.port} pairPort=$pairPort",
        )

        val manager = getWirelessAdbManagerOrNull()
        if (manager == null) {
            _connectionError.value = "Không thể khởi tạo ADB pair engine trên thiết bị này."
            return@withContext false
        }

        return@withContext try {
            val paired = withTimeoutOrNull(25_000L) {
                manager.pair(device.ipAddress, pairPort, sanitizedCode)
            } ?: false

            if (!paired) {
                _connectionError.value = "Ghép đôi ADB thất bại. Kiểm tra lại Pair Port và mã pair trên TV."
                InAppDiagnostics.warn(TAG, "[ADB_PAIR] failed target=${device.ipAddress}:$pairPort")
                false
            } else {
                InAppDiagnostics.info(TAG, "[ADB_PAIR] success target=${device.ipAddress}:$pairPort")

                val protocol = WirelessAdbProtocol(manager)
                val connected = withTimeoutOrNull(20_000L) {
                    protocol.connect(device.ipAddress, device.port)
                } ?: false

                if (!connected) {
                    _connectionError.value =
                        "Pair thành công nhưng kết nối ADB thất bại ở cổng ${device.port}. Hãy kiểm tra Connect Port trên TV."
                    InAppDiagnostics.warn(
                        TAG,
                        "[ADB_PAIR] connect_after_pair_failed target=${device.ipAddress}:${device.port}",
                    )
                    false
                } else {
                    activeProtocol = protocol
                    _currentDevice.value = device.copy(isConnected = true)
                    _playbackState.value = PlaybackState.IDLE
                    lastConnectedDevice = device
                    InAppDiagnostics.info(
                        TAG,
                        "[ADB_PAIR] connect_after_pair_success target=${device.ipAddress}:${device.port}",
                    )
                    true
                }
            }
        } catch (e: Exception) {
            _connectionError.value = "Ghép đôi thất bại: ${e.message ?: "unknown"}"
            InAppDiagnostics.error(TAG, "[ADB_PAIR] error=${e.message}")
            false
        } finally {
            if (_connectingDeviceKey.value == targetKey) {
                _connectingDeviceKey.value = null
            }
        }
    }

    suspend fun sendCommand(command: String): Boolean {
        val protocol = activeProtocol
        InAppDiagnostics.info(TAG, "[COMMAND_SEND] request=$command")

        if (command.startsWith("TEXT:")) {
            InAppDiagnostics.info(TAG, "[TEXT_SEND] request=${command.removePrefix("TEXT:")}")
        }

        return if (protocol != null) {
            val candidates = resolveCommandCandidates(command, protocol)
            var success = false

            for (candidate in candidates) {
                val sent = protocol.sendCommand(candidate)
                InAppDiagnostics.info(TAG, "[COMMAND_SEND] try=$candidate result=$sent")
                if (sent) {
                    success = true
                    break
                }
            }

            if (!success) {
                Log.e(TAG, "Command failed: $command")
                InAppDiagnostics.error(TAG, "[COMMAND_SEND] failed=$command")
                if (command.startsWith("TEXT:")) {
                    InAppDiagnostics.error(TAG, "[TEXT_SEND] failed protocol=${protocol.javaClass.simpleName}")
                }
            } else {
                InAppDiagnostics.info(TAG, "[COMMAND_SEND] success=$command")
                if (command.startsWith("TEXT:")) {
                    InAppDiagnostics.info(TAG, "[TEXT_SEND] success protocol=${protocol.javaClass.simpleName}")
                }
                updatePlaybackStateFromCommand(command)
            }

            success
        } else {
            Log.e(TAG, "No active connection to send command: $command")
            InAppDiagnostics.error(TAG, "[COMMAND_SEND] blocked_no_session=$command")
            false
        }
    }

    private fun TVDevice.toConnectionKey(): String = "$ipAddress:$port"

    suspend fun launchApp(appId: String): AppLaunchResult {
        val protocol = activeProtocol
        val device = _currentDevice.value

        if (protocol == null || device == null) {
            InAppDiagnostics.error(TAG, "[APP_LAUNCH] blocked_no_session appId=$appId")
            return AppLaunchResult.noSession(appId)
        }

        val mappedAppId = mapAppIdForDevice(appId, device)
        InAppDiagnostics.info(TAG, "[APP_LAUNCH] request=$appId mapped=$mappedAppId brand=${device.brand}")
        val directSuccess = protocol.launchApp(mappedAppId)

        if (directSuccess) {
            InAppDiagnostics.info(TAG, "[APP_LAUNCH] success appId=$mappedAppId")
            return AppLaunchResult.success(appId, mappedAppId)
        }

        if (device.brand == TVBrand.ANDROID_TV && protocol !is AdbProtocol && protocol !is WirelessAdbProtocol) {
            val adbSuccess = launchAndroidTvViaAdb(device, appId)
            if (adbSuccess) {
                InAppDiagnostics.info(TAG, "[APP_LAUNCH] success via_adb appId=$appId")
                return AppLaunchResult.success(appId, appId)
            }
        }

        val searchFallbackSuccess = launchBySearchFallback(appId)
        if (searchFallbackSuccess) {
            InAppDiagnostics.info(TAG, "[APP_LAUNCH] success via_search_fallback appId=$appId")
            return AppLaunchResult.success(appId, appId)
        }

        val unsupported = protocol is AndroidTVRemoteProtocol || protocol is LGProtocol
        if (unsupported) {
            InAppDiagnostics.error(TAG, "[APP_LAUNCH] unsupported appId=$mappedAppId protocol=${protocol.javaClass.simpleName}")
            return AppLaunchResult.unsupported(
                requestedAppId = appId,
                resolvedAppId = mappedAppId,
                message = "Current protocol does not support app launch yet",
            )
        }

        InAppDiagnostics.error(TAG, "[APP_LAUNCH] failed appId=$mappedAppId brand=${device.brand}")
        return AppLaunchResult.failed(
            requestedAppId = appId,
            resolvedAppId = mappedAppId,
            message = "TV rejected app launch request",
        )
    }

    private suspend fun launchAndroidTvViaAdb(device: TVDevice, appId: String): Boolean {
        val candidatePorts = adbCandidatePortsFor(device)
        candidatePorts.forEach { adbPort ->
            val adb = AdbProtocol()
            try {
                val connected = withTimeoutOrNull(10_000L) {
                    adb.connect(device.ipAddress, adbPort)
                } ?: false

                if (!connected) {
                    InAppDiagnostics.warn(TAG, "[APP_LAUNCH] adb_fallback_connect_failed target=${device.ipAddress}:$adbPort")
                    return@forEach
                }

                val launched = adb.launchApp(appId)
                if (launched) {
                    InAppDiagnostics.info(TAG, "[APP_LAUNCH] adb_fallback_success target=${device.ipAddress}:$adbPort")
                    return true
                }
            } catch (e: Exception) {
                InAppDiagnostics.error(TAG, "[APP_LAUNCH] adb_fallback_error target=${device.ipAddress}:$adbPort error=${e.message}")
            } finally {
                runCatching { adb.disconnect() }
            }
        }

        return false
    }

    private suspend fun launchBySearchFallback(appId: String): Boolean {
        val appQuery = appSearchQuery(appId) ?: return false

        InAppDiagnostics.warn(TAG, "[APP_LAUNCH] fallback_search start query=$appQuery")

        // Try opening search and typing app name when direct app launch is unavailable.
        sendCommand("KEY_HOME")
        delay(140)

        val openSearch = sendCommand("KEY_SEARCH")
        if (!openSearch) {
            InAppDiagnostics.warn(TAG, "[APP_LAUNCH] fallback_search failed open_search")
            return false
        }

        delay(120)
        val sendText = sendCommand("TEXT:$appQuery")
        delay(120)
        val submit = sendCommand("KEY_ENTER")

        val success = sendText && submit
        if (!success) {
            InAppDiagnostics.warn(TAG, "[APP_LAUNCH] fallback_search failed sendText=$sendText submit=$submit")
        }
        return success
    }

    private fun appSearchQuery(appId: String): String? {
        val normalizedAppId = appId.trim()
        return when (normalizedAppId) {
            "com.netflix.ninja" -> "Netflix"
            "com.google.android.youtube.tv", "com.google.android.youtube.tvunplugged" -> "YouTube"
            "com.disney.disneyplus" -> "Disney Plus"
            "com.amazon.amazonvideo.livingroom" -> "Prime Video"
            "com.spotify.tv.android" -> "Spotify"
            "com.android.vending" -> "Play Store"
            "com.android.tv.settings", "am start -a android.settings.SETTINGS" -> "Settings"
            else -> normalizedAppId.toSearchFallbackQuery()
        }
    }

    private fun String.toSearchFallbackQuery(): String? {
        val normalized = trim()
        if (normalized.isBlank()) return null

        if (normalized.startsWith("am start -a android.settings.SETTINGS", ignoreCase = true)) {
            return "Settings"
        }

        if (!normalized.contains('.')) {
            return normalized.takeIf { it.length >= 2 }
        }

        val candidate = normalized
            .substringAfterLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .replaceFirstChar { ch -> ch.uppercase() }

        return candidate.takeIf { it.length >= 2 }
    }

    private fun resolveCommandCandidates(command: String, protocol: TVProtocol): List<String> {
        if (protocol is SamsungProtocol) {
            return when (command) {
                "KEY_VOICE" -> listOf("KEY_BT_VOICE", "KEY_VOICE", "KEY_MIC")
                "KEY_SEARCH" -> listOf("KEY_SEARCH", "KEY_FINDER")
                "KEY_CH_UP" -> listOf("KEY_CH_UP", "KEY_CHUP")
                "KEY_CH_DOWN" -> listOf("KEY_CH_DOWN", "KEY_CHDOWN")
                else -> listOf(command)
            }
        }

        if (protocol is LGProtocol) {
            return when (command) {
                "KEY_CH_UP" -> listOf("KEY_CH_UP", "KEY_CHANNEL_UP")
                "KEY_CH_DOWN" -> listOf("KEY_CH_DOWN", "KEY_CHANNEL_DOWN")
                else -> listOf(command)
            }
        }

        if (protocol is AndroidTVRemoteProtocol || protocol is AdbProtocol || protocol is WirelessAdbProtocol) {
            return when (command) {
                "KEY_VOICE" -> listOf("KEY_VOICE", "VOICE")
                "KEY_SEARCH" -> listOf("KEY_SEARCH", "SEARCH")
                // Google TV often handles options/settings better than legacy KEY_MENU.
                "KEY_MENU" -> listOf("KEY_SETTINGS", "KEY_MENU")
                "KEY_CH_UP" -> listOf("KEY_CH_UP", "KEY_CHANNEL_UP")
                "KEY_CH_DOWN" -> listOf("KEY_CH_DOWN", "KEY_CHANNEL_DOWN")
                else -> listOf(command)
            }
        }

        return listOf(command)
    }

    private fun mapAppIdForDevice(appId: String, device: TVDevice): String {
        return when (device.brand) {
            TVBrand.SAMSUNG -> when (appId) {
                "com.netflix.ninja" -> "11101200001"
                "com.google.android.youtube.tv", "com.google.android.youtube.tvunplugged" -> "111299001912"
                "com.disney.disneyplus" -> "3201901017640"
                "com.amazon.amazonvideo.livingroom" -> "3201512006785"
                "com.spotify.tv.android" -> "3201606009684"
                "com.android.tv.settings" -> "org.tizen.settings"
                else -> appId
            }

            TVBrand.LG -> when (appId) {
                "com.netflix.ninja" -> "netflix"
                "com.google.android.youtube.tv", "com.google.android.youtube.tvunplugged" -> "youtube.leanback.v4"
                "com.disney.disneyplus" -> "com.disney.disneyplus-prod"
                "com.amazon.amazonvideo.livingroom" -> "amazon"
                "com.spotify.tv.android" -> "spotify-beehive"
                "com.android.tv.settings" -> "com.webos.app.settings"
                else -> appId
            }

            else -> appId
        }
    }

    private fun updatePlaybackStateFromCommand(command: String) {
        when (command) {
            "KEY_PLAY" -> _playbackState.value = PlaybackState.PLAYING
            "KEY_PAUSE", "KEY_STOP", "KEY_HOME" -> _playbackState.value = PlaybackState.IDLE
            "KEY_PLAY_PAUSE" -> {
                _playbackState.value = if (_playbackState.value == PlaybackState.PLAYING) {
                    PlaybackState.IDLE
                } else {
                    PlaybackState.PLAYING
                }
            }
        }
    }
}

