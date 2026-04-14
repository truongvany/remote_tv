package com.example.remote_tv.ui.viewmodel

import android.app.Application
import android.os.Build
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.net.Inet4Address
import android.util.Log
import android.util.Patterns
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.remote_tv.data.model.AppLaunchStatus
import com.example.remote_tv.data.model.AppThemeMode
import com.example.remote_tv.data.model.Macro
import com.example.remote_tv.data.model.PlaybackState
import com.example.remote_tv.data.model.TVApp
import com.example.remote_tv.data.model.TVDevice
import com.example.remote_tv.data.model.isCastOnlyEndpoint
import com.example.remote_tv.data.model.toTVDevice
import com.example.remote_tv.data.network.WakeOnLanSender
import com.example.remote_tv.data.casting.CastManager
import com.example.remote_tv.data.casting.LocalMediaEndpoint
import com.example.remote_tv.data.protocol.AdbProtocol
import com.example.remote_tv.data.preferences.AppPreferencesRepository
import com.example.remote_tv.data.preferences.MacroRepository
import com.example.remote_tv.data.repository.TVRepository
import com.example.remote_tv.data.repository.TVRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class MacroRunState(
    val runningMacroId: String? = null,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val currentCommand: String? = null,
    val isCancelling: Boolean = false,
)

class TVViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TVRepository = TVRepositoryImpl(application)
    private val appPreferencesRepository = AppPreferencesRepository(application)
    private val macroRepository = MacroRepository(application)
    private val castManager = CastManager(application)

    val discoveredDevices: StateFlow<List<TVDevice>> = repository.discoveredDevices
    val currentDevice: StateFlow<TVDevice?> = repository.currentDevice
    val connectingDeviceKey: StateFlow<String?> = repository.connectingDeviceKey
    val isScanning: StateFlow<Boolean> = repository.isScanning
    val scanError: StateFlow<String?> = repository.scanError
    val connectionError: StateFlow<String?> = repository.connectionError
    val playbackState: StateFlow<PlaybackState> = repository.playbackState
    val diagnosticLogs: StateFlow<List<String>> = repository.diagnosticLogs
    val installedApps: StateFlow<List<TVApp>> = repository.installedApps

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    private val _settingsUiState = MutableStateFlow(SettingsUiState())
    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState.asStateFlow()

    private val _macros = MutableStateFlow<List<Macro>>(emptyList())
    val macros: StateFlow<List<Macro>> = _macros.asStateFlow()

    private val _macroRunState = MutableStateFlow(MacroRunState())
    val macroRunState: StateFlow<MacroRunState> = _macroRunState.asStateFlow()
    private var macroExecutionJob: Job? = null

    private val _isWaking = MutableStateFlow(false)
    val isWaking: StateFlow<Boolean> = _isWaking.asStateFlow()

    private val _lastDeviceName = MutableStateFlow<String?>(null)
    val lastDeviceName: StateFlow<String?> = _lastDeviceName.asStateFlow()
    private var hasAttemptedAutoReconnect = false

    private val commandMutex = Mutex()

    val isCasting: StateFlow<Boolean> = castManager.isCasting
    val castStatus: StateFlow<String> = castManager.castStatus
    val castError: StateFlow<String?> = castManager.castError

    init {
        _uiState.update {
            it.copy(
                hasLocationPermission = hasDiscoveryPermission(),
                localIpAddress = resolveLocalIpv4Address(),
                localSubnet = resolveLocalSubnet(),
            )
        }
        observeSettings()
        observePlaybackVisibility()
        observeMacros()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                appPreferencesRepository.appSettingsFlow,
                appPreferencesRepository.userProfileFlow
            ) { appSettings, userProfile ->
                appSettings to userProfile
            }.collect { (appSettings, userProfile) ->
                _settingsUiState.update {
                    it.copy(
                        appSettings = appSettings,
                        userProfile = userProfile,
                        isProfileSaving = false
                    )
                }

                if (appSettings.autoReconnectLastDevice && !hasAttemptedAutoReconnect) {
                    hasAttemptedAutoReconnect = true
                    autoReconnectLastDevice()
                }
            }
        }
    }

    private fun observePlaybackVisibility() {
        viewModelScope.launch {
            combine(currentDevice, playbackState) { device, state ->
                device != null && state == PlaybackState.PLAYING
            }.collect { shouldShowNowPlaying ->
                _uiState.update { current ->
                    current.copy(showNowPlaying = shouldShowNowPlaying)
                }
            }
        }
    }

    fun selectTab(index: Int) = _uiState.update { it.copy(selectedTab = index) }
    fun selectMode(index: Int) = _uiState.update { it.copy(selectedMode = index) }

    fun onCastTabOpened() {
        refreshNetworkDebugInfo()
        if (_uiState.value.hasLocationPermission && settingsUiState.value.appSettings.autoScanOnCastTab) {
            repository.startDiscovery()
        }
    }

    fun refreshCastScan() {
        refreshNetworkDebugInfo()
        if (_uiState.value.hasLocationPermission) {
            repository.startDiscovery()
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        val hasPermission = hasDiscoveryPermission()
        _uiState.update {
            it.copy(
                hasLocationPermission = hasPermission,
                localIpAddress = resolveLocalIpv4Address(),
                localSubnet = resolveLocalSubnet(),
            )
        }
        if (hasPermission && _uiState.value.selectedTab == 2 && settingsUiState.value.appSettings.autoScanOnCastTab) {
            repository.startDiscovery()
        }
    }

    fun showDeviceDialog() {
        _uiState.update { it.copy(showDeviceDialog = true) }
        repository.startDiscovery()
    }
    fun hideDeviceDialog() = _uiState.update { it.copy(showDeviceDialog = false) }

    fun connectToDevice(device: TVDevice) {
        if (device.isCastOnlyEndpoint()) {
            _uiState.update {
                it.copy(actionMessage = "Thiết bị này chỉ hỗ trợ Cast media. Hãy dùng Connect Cast Session để ghép TV trước.")
            }
            hideDeviceDialog()
            return
        }

        repository.connectToDevice(device)
        hideDeviceDialog()
        // Lưu thiết bị vào disk để auto-reconnect lần sau
        viewModelScope.launch {
            repository.saveLastDevice(device)
            _lastDeviceName.value = device.name
        }
        // Sau khi kết nối xong, lấy danh sách app
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // chờ kết nối ổn định
            fetchInstalledApps()
        }
    }

    fun pairAndConnectToDevice(device: TVDevice, pairPort: Int, pairCode: String, connectPort: Int) {
        viewModelScope.launch {
            val sanitizedCode = pairCode.trim().filter { it.isDigit() }
            if (sanitizedCode.length < 6) {
                _uiState.update { it.copy(actionMessage = "Mã pair không hợp lệ. Hãy nhập mã 6 số trên TV.") }
                return@launch
            }

            if (connectPort !in 1..65535 || pairPort !in 1..65535) {
                _uiState.update { it.copy(actionMessage = "Cổng pair/connect không hợp lệ.") }
                return@launch
            }

            val targetDevice = device.copy(port = connectPort, brand = com.example.remote_tv.data.model.TVBrand.ANDROID_TV)
            _uiState.update { it.copy(actionMessage = "Đang ghép đôi ${targetDevice.name}...") }

            val success = repository.pairAndConnectAndroidTv(
                device = targetDevice,
                pairPort = pairPort,
                pairingCode = sanitizedCode,
            )

            if (!success) {
                _uiState.update { it.copy(actionMessage = "Ghép đôi thất bại. Kiểm tra lại mã pair và cổng trên TV.") }
                return@launch
            }

            _uiState.update { it.copy(actionMessage = "Đã ghép đôi và kết nối ${targetDevice.name}") }
            repository.saveLastDevice(targetDevice)
            _lastDeviceName.value = targetDevice.name
            delay(1500)
            fetchInstalledApps()
        }
    }

    fun clearDiagnosticLogs() {
        repository.clearDiagnosticLogs()
    }

    fun disconnect() {
        repository.disconnect()
        _uiState.update { it.copy(launchedAppId = null) }
    }

    // ----------------------------------------------------------------
    // Auto-Reconnect
    // ----------------------------------------------------------------

    private fun autoReconnectLastDevice() {
        viewModelScope.launch {
            val saved = repository.loadLastDevice() ?: return@launch
            _lastDeviceName.value = saved.name
            Log.d("TVViewModel", "Auto-reconnecting to last device: ${saved.name} at ${saved.ip}:${saved.port}")
            _uiState.update { it.copy(actionMessage = "Reconnecting to ${saved.name}...") }
            repository.connectToDevice(saved.toTVDevice())
        }
    }

    private fun observeMacros() {
        viewModelScope.launch {
            macroRepository.macrosFlow.collect { list ->
                _macros.value = list
            }
        }
    }

    // ----------------------------------------------------------------
    // Wake-on-LAN
    // ----------------------------------------------------------------

    fun wakeLastDevice() {
        viewModelScope.launch {
            val saved = repository.loadLastDevice()
            val mac = saved?.macAddress
            if (mac == null) {
                _uiState.update { it.copy(actionMessage = "Không tìm thấy địa chỉ MAC của TV") }
                return@launch
            }
            _isWaking.value = true
            _uiState.update { it.copy(actionMessage = "Đang đánh thức ${saved.name}...") }

            val broadcastIp = _uiState.value.localSubnet
                ?.let { WakeOnLanSender.broadcastFromSubnet(it) }
                ?: "255.255.255.255"

            val success = repository.wakeDevice(mac, broadcastIp)
            _isWaking.value = false
            _uiState.update {
                it.copy(actionMessage = if (success) "Magic Packet đã gửi đến ${saved.name}" else "Gửi Wake-on-LAN thất bại")
            }
        }
    }

    fun sendCommand(command: String) {
        viewModelScope.launch {
            sendCommandInternal(command)
        }
    }

    fun sendText(text: String) {
        viewModelScope.launch {
            val normalized = text.trim()
            if (normalized.isBlank()) return@launch

            val sent = sendCommandInternal("TEXT:$normalized")
            if (!sent) {
                _uiState.update { it.copy(actionMessage = "Cannot send text on current TV connection") }
            }
        }
    }

    fun sendVoiceQuery(query: String) {
        viewModelScope.launch {
            val normalized = query.trim()
            if (normalized.isBlank()) return@launch

            val openSearch = sendCommandInternal("KEY_SEARCH")
            if (!openSearch) {
                _uiState.update { it.copy(actionMessage = "Cannot open search on current TV connection") }
                return@launch
            }

            delay(180)
            val sentText = sendCommandInternal("TEXT:$normalized")
            delay(120)
            val submitted = sendCommandInternal("KEY_ENTER")

            if (!sentText || !submitted) {
                _uiState.update { it.copy(actionMessage = "Voice recognized but text was not sent completely") }
            }
        }
    }

    private suspend fun sendCommandInternal(command: String): Boolean {
        return commandMutex.withLock {
            val success = repository.sendCommand(command)
            if (!success && command.startsWith("TEXT:")) {
                _uiState.update {
                    it.copy(actionMessage = "Cannot send text on current TV connection")
                }
            }
            success
        }
    }

    fun sendDirection(direction: String) = sendCommand(direction)
    fun sendOk()        = sendCommand("OK")
    fun powerToggle()   = sendCommand("KEY_POWER")
    fun volumeUp()      = sendCommand("KEY_VOL_UP")
    fun volumeDown()    = sendCommand("KEY_VOL_DOWN")
    fun mute()          = sendCommand("KEY_MUTE")
    fun back()          = sendCommand("KEY_BACK")
    fun home()          = sendCommand("KEY_HOME")
    fun menu()          = sendCommand("KEY_MENU")

    // =======================================================
    // ĐÃ UPDATE: KẾT HỢP REPOSITORY CỦA Ý VÀ ADB CỦA HUY
    // =======================================================
    fun launchApp(appId: String) {
        if (appId == "NOT_CONNECTED") {
            _uiState.update { it.copy(actionMessage = "Vui lòng kết nối TV trước khi dùng Quick Launch") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(launchedAppId = appId) }
            // Bước 1: Thử gọi theo luồng chuẩn của Repository
            val result = repository.launchApp(appId)

            // Bước 2: Nếu giao thức chuẩn từ chối (UNSUPPORTED) hoặc thất bại
            if (!result.isSuccess) {
                // Lấy IP của TV đang kết nối hiện tại
                val ip = currentDevice.value?.ipAddress

                if (ip != null) {
                    Log.d("TVViewModel", "Fallback sang ADB để mở app: $appId trên IP: $ip")

                    // Gọi bí thuật ADB của Huy
                    val adbSuccess = launchAppViaAdb(ip, appId)

                    if (adbSuccess) {
                        _uiState.update { it.copy(actionMessage = "Đã gửi lệnh mở App qua ADB") }
                    } else {
                        _uiState.update { it.copy(actionMessage = "ADB thất bại: TV chưa bật gỡ lỗi hoặc sai IP", launchedAppId = null) }
                    }
                } else {
                    _uiState.update { it.copy(actionMessage = "Vui lòng kết nối TV trước khi dùng Quick Launch", launchedAppId = null) }
                }
            } else {
                // Nếu giao thức gốc mở thành công
                _uiState.update { it.copy(actionMessage = null) }
            }
        }
    }

    // ----------------------------------------------------------------
    // App List Sync
    // ----------------------------------------------------------------

    fun fetchInstalledApps() {
        viewModelScope.launch {
            repository.fetchInstalledApps()
        }
    }

    // ----------------------------------------------------------------
    // Macro Keys
    // ----------------------------------------------------------------

    fun executeMacro(macro: Macro) {
        if (currentDevice.value == null) {
            _uiState.update { it.copy(actionMessage = "Connect TV before running macro") }
            return
        }

        if (macroExecutionJob?.isActive == true) {
            _uiState.update { it.copy(actionMessage = "A macro is already running") }
            return
        }

        val normalizedCommands = normalizeMacroCommands(macro.commands)
        if (normalizedCommands.isEmpty()) {
            _uiState.update { it.copy(actionMessage = "Macro has no valid commands") }
            return
        }

        macroExecutionJob = viewModelScope.launch {
            _macroRunState.value = MacroRunState(
                runningMacroId = macro.id,
                totalSteps = normalizedCommands.size,
            )
            _uiState.update { it.copy(actionMessage = "Running: ${macro.name}") }

            val stepDelay = macro.delayMs.coerceIn(50L, 5_000L)

            for ((index, command) in normalizedCommands.withIndex()) {
                if (!isActive) {
                    return@launch
                }

                _macroRunState.value = MacroRunState(
                    runningMacroId = macro.id,
                    currentStep = index + 1,
                    totalSteps = normalizedCommands.size,
                    currentCommand = command,
                )

                val sent = sendCommandInternal(command)
                if (!sent) {
                    _uiState.update {
                        it.copy(actionMessage = "Macro stopped at step ${index + 1}: $command")
                    }
                    return@launch
                }

                if (index < normalizedCommands.lastIndex) {
                    delay(stepDelay)
                }
            }

            _uiState.update { it.copy(actionMessage = "${macro.name} done") }
        }.also { job ->
            job.invokeOnCompletion {
                if (macroExecutionJob == job) {
                    macroExecutionJob = null
                }
                _macroRunState.value = MacroRunState()
            }
        }
    }

    fun stopMacroExecution() {
        val activeJob = macroExecutionJob
        if (activeJob == null || !activeJob.isActive) {
            return
        }

        _macroRunState.update { it.copy(isCancelling = true) }
        activeJob.cancel()
        _uiState.update { it.copy(actionMessage = "Macro stopped") }
    }

    fun addMacro(macro: Macro) {
        viewModelScope.launch {
            val normalizedName = macro.name.trim()
            val normalizedCommands = normalizeMacroCommands(macro.commands)
            if (normalizedName.isBlank() || normalizedCommands.isEmpty()) {
                _uiState.update { it.copy(actionMessage = "Macro name and commands are required") }
                return@launch
            }

            val normalizedMacro = macro.copy(
                name = normalizedName,
                commands = normalizedCommands,
                delayMs = macro.delayMs.coerceIn(50L, 5_000L),
            )

            val exists = _macros.value.any { it.id == normalizedMacro.id }
            val updated = if (exists) {
                _macros.value.map { item -> if (item.id == normalizedMacro.id) normalizedMacro else item }
            } else {
                _macros.value + normalizedMacro
            }

            macroRepository.saveMacros(updated)
            _uiState.update {
                it.copy(actionMessage = if (exists) "Macro updated" else "Macro saved")
            }
        }
    }

    fun updateMacro(macro: Macro) {
        addMacro(macro)
    }

    fun duplicateMacro(source: Macro) {
        val duplicate = source.copy(
            id = UUID.randomUUID().toString(),
            name = buildDuplicateMacroName(source.name),
        )
        addMacro(duplicate)
    }

    fun removeMacro(macroId: String) {
        viewModelScope.launch {
            val updated = _macros.value.filter { it.id != macroId }
            macroRepository.saveMacros(updated)
            _uiState.update { it.copy(actionMessage = "Macro deleted") }
        }
    }

    private fun buildDuplicateMacroName(baseName: String): String {
        val existingNames = _macros.value.map { it.name.lowercase() }.toSet()
        val baseCopy = "${baseName.trim()} Copy".trim()
        if (baseCopy.lowercase() !in existingNames) {
            return baseCopy
        }

        var index = 2
        while (true) {
            val candidate = "$baseCopy $index"
            if (candidate.lowercase() !in existingNames) {
                return candidate
            }
            index++
        }
    }

    private fun normalizeMacroCommands(commands: List<String>): List<String> {
        return commands.mapNotNull { raw ->
            val command = raw.trim()
            if (command.isBlank()) {
                return@mapNotNull null
            }

            when {
                command.startsWith("TEXT:", ignoreCase = true) -> {
                    val payload = command.substringAfter(':').trim()
                    if (payload.isBlank()) null else "TEXT:$payload"
                }

                command.startsWith("OPEN_URL:", ignoreCase = true) -> {
                    val payload = command.substringAfter(':').trim()
                    if (payload.isBlank()) null else "OPEN_URL:$payload"
                }

                command.startsWith("SEARCH_QUERY:", ignoreCase = true) -> {
                    val payload = command.substringAfter(':').trim()
                    if (payload.isBlank()) null else "SEARCH_QUERY:$payload"
                }

                command.startsWith("am ", ignoreCase = true) ||
                    command.startsWith("cmd ", ignoreCase = true) ||
                    command.startsWith("monkey ", ignoreCase = true) -> command

                else -> command.uppercase()
            }
        }
    }

    // ----------------------------------------------------------------
    // Media Casting
    // ----------------------------------------------------------------

    fun castVideo(url: String, title: String) {
        val success = castManager.castVideo(url, title)
        if (!success) {
            _uiState.update { it.copy(actionMessage = castError.value ?: "Cannot start cast session") }
        }
    }

    fun castImageFromUri(uri: Uri) {
        viewModelScope.launch {
            val connectedName = currentDevice.value?.name
            val pushedToConnectedTv = pushMediaToConnectedTv(
                endpoint = castManager.prepareImageEndpoint(uri, fallbackTitle = "Image"),
                mediaLabel = "image",
            )

            if (pushedToConnectedTv) {
                return@launch
            }

            val success = castManager.castImageFromUri(uri)
            if (!success) {
                _uiState.update {
                    val fallback = if (connectedName != null) {
                        "Cannot push image to $connectedName. Keep ADB connected or use Cast route icon."
                    } else {
                        "Cannot cast selected image"
                    }
                    it.copy(actionMessage = castError.value ?: fallback)
                }
            }
        }
    }

    fun castVideoFromUri(uri: Uri) {
        viewModelScope.launch {
            val connectedName = currentDevice.value?.name
            val pushedToConnectedTv = pushMediaToConnectedTv(
                endpoint = castManager.prepareVideoEndpoint(uri, fallbackTitle = "Video"),
                mediaLabel = "video",
            )

            if (pushedToConnectedTv) {
                return@launch
            }

            val success = castManager.castVideoFromUri(uri)
            if (!success) {
                _uiState.update {
                    val fallback = if (connectedName != null) {
                        "Cannot push video to $connectedName. Keep ADB connected or use Cast route icon."
                    } else {
                        "Cannot cast selected video"
                    }
                    it.copy(actionMessage = castError.value ?: fallback)
                }
            }
        }
    }

    fun stopCasting() {
        castManager.stopCasting()
    }

    private suspend fun pushMediaToConnectedTv(endpoint: LocalMediaEndpoint?, mediaLabel: String): Boolean {
        val device = currentDevice.value ?: return false
        val mediaEndpoint = endpoint ?: run {
            _uiState.update { it.copy(actionMessage = "Cannot prepare selected $mediaLabel") }
            return false
        }

        val targetUrl = mediaEndpoint.remoteOpenUrl.ifBlank { mediaEndpoint.url }
        val targetMime = if (targetUrl != mediaEndpoint.url) "text/html" else mediaEndpoint.mimeType

        val command = buildOpenUrlCommand(targetUrl, targetMime)
        val sent = sendCommandInternal(command)
        if (sent) {
            _uiState.update {
                it.copy(actionMessage = "Sent $mediaLabel to ${device.name}")
            }
            return true
        }

        return false
    }

    private fun buildOpenUrlCommand(url: String, mimeType: String): String {
        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
        val encodedMime = URLEncoder.encode(mimeType, StandardCharsets.UTF_8.toString())
        return "OPEN_URL:$encodedUrl|$encodedMime"
    }

    // ADB fallback chạy trên luồng nền (IO)
    private suspend fun launchAppViaAdb(ip: String, packageName: String): Boolean = withContext(Dispatchers.IO) {
        val adb = AdbProtocol()
        try {
            val connected = adb.connect(ip, 5555)
            if (!connected) {
                return@withContext false
            }

            adb.launchApp(packageName.trim())
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            runCatching { adb.disconnect() }
        }
    }
    // =======================================================

    fun consumeActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun showActionMessage(message: String) {
        _uiState.update { it.copy(actionMessage = message) }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            appPreferencesRepository.setThemeMode(themeMode)
        }
    }

    fun toggleThemeMode() {
        val nextMode = when (settingsUiState.value.appSettings.themeMode) {
            AppThemeMode.DARK -> AppThemeMode.LIGHT
            AppThemeMode.LIGHT -> AppThemeMode.DARK
        }
        setThemeMode(nextMode)
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            appPreferencesRepository.setLanguageCode(languageCode)
        }
    }

    fun setAutoReconnectLastDevice(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesRepository.setAutoReconnectLastDevice(enabled)
            if (enabled && !hasAttemptedAutoReconnect) {
                hasAttemptedAutoReconnect = true
                autoReconnectLastDevice()
            }
        }
    }

    fun setAutoScanOnCastTab(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesRepository.setAutoScanOnCastTab(enabled)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesRepository.setKeepScreenOn(enabled)
        }
    }

    fun forgetLastConnectedDevice() {
        viewModelScope.launch {
            repository.clearLastDevice()
            _lastDeviceName.value = null
            _uiState.update { it.copy(actionMessage = "Last connected TV removed") }
        }
    }

    fun updateUserProfile(displayName: String, email: String) {
        val normalizedName = displayName.trim()
        val normalizedEmail = email.trim()

        if (normalizedName.isBlank()) {
            _settingsUiState.update { it.copy(profileError = "PROFILE_NAME_EMPTY") }
            return
        }

        if (normalizedEmail.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            _settingsUiState.update { it.copy(profileError = "PROFILE_EMAIL_INVALID") }
            return
        }

        viewModelScope.launch {
            _settingsUiState.update { it.copy(isProfileSaving = true, profileError = null) }
            appPreferencesRepository.updateUserProfile(normalizedName, normalizedEmail)
            _settingsUiState.update { it.copy(isProfileSaving = false) }
        }
    }

    fun clearProfileError() {
        _settingsUiState.update { it.copy(profileError = null) }
    }

    override fun onCleared() {
        super.onCleared()
        macroExecutionJob?.cancel()
        repository.stopDiscovery()
        castManager.release()
    }

    private fun hasDiscoveryPermission(): Boolean {
        val app = getApplication<Application>()
        val hasFineLocation = ContextCompat.checkSelfPermission(
            app,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation) {
            return false
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        val hasNearbyWifi = ContextCompat.checkSelfPermission(
            app,
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
        ) == PackageManager.PERMISSION_GRANTED

        return hasNearbyWifi
    }

    private fun refreshNetworkDebugInfo() {
        _uiState.update {
            it.copy(
                localIpAddress = resolveLocalIpv4Address(),
                localSubnet = resolveLocalSubnet(),
            )
        }
    }

    private fun resolveLocalIpv4Address(): String? {
        val cm = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = resolveWifiNetwork(cm) ?: return null
        val linkProperties = cm.getLinkProperties(network) ?: return null
        return linkProperties.linkAddresses
            .firstOrNull { address ->
                val inetAddress = address.address
                inetAddress is Inet4Address && !inetAddress.isLoopbackAddress
            }
            ?.address
            ?.hostAddress
    }

    private fun resolveLocalSubnet(): String? {
        val cm = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = resolveWifiNetwork(cm) ?: return null
        val linkProperties: LinkProperties = cm.getLinkProperties(network) ?: return null

        val linkAddress = linkProperties.linkAddresses.firstOrNull { address ->
            val inetAddress = address.address
            inetAddress is Inet4Address && !inetAddress.isLoopbackAddress
        } ?: return null

        val hostAddress = linkAddress.address.hostAddress ?: return null
        val prefixLength = linkAddress.prefixLength
        return "$hostAddress/$prefixLength"
    }

    private fun resolveWifiNetwork(connectivityManager: ConnectivityManager): Network? {
        @Suppress("DEPRECATION")
        return connectivityManager.allNetworks.firstOrNull { network ->
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@firstOrNull false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
    }
}