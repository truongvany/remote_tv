package com.example.remote_tv.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.Socket
import java.net.InetSocketAddress
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
import com.example.remote_tv.data.model.toTVDevice
import com.example.remote_tv.data.network.WakeOnLanSender
import com.example.remote_tv.data.casting.CastManager
import com.example.remote_tv.data.preferences.AppPreferencesRepository
import com.example.remote_tv.data.preferences.MacroRepository
import com.example.remote_tv.data.repository.TVRepository
import com.example.remote_tv.data.repository.TVRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val _isWaking = MutableStateFlow(false)
    val isWaking: StateFlow<Boolean> = _isWaking.asStateFlow()

    private val _lastDeviceName = MutableStateFlow<String?>(null)
    val lastDeviceName: StateFlow<String?> = _lastDeviceName.asStateFlow()

    val isCasting: StateFlow<Boolean> = castManager.isCasting

    init {
        _uiState.update {
            it.copy(
                hasLocationPermission = hasFineLocationPermission(),
                localIpAddress = resolveLocalIpv4Address(),
                localSubnet = resolveLocalSubnet(),
            )
        }
        observeSettings()
        observePlaybackVisibility()
        observeMacros()
        autoReconnectLastDevice()
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
        if (_uiState.value.hasLocationPermission) {
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
        _uiState.update {
            it.copy(
                hasLocationPermission = granted,
                localIpAddress = resolveLocalIpv4Address(),
                localSubnet = resolveLocalSubnet(),
            )
        }
        if (granted && _uiState.value.selectedTab == 2) {
            repository.startDiscovery()
        }
    }

    fun showDeviceDialog() {
        _uiState.update { it.copy(showDeviceDialog = true) }
        repository.startDiscovery()
    }
    fun hideDeviceDialog() = _uiState.update { it.copy(showDeviceDialog = false) }

    fun connectToDevice(device: TVDevice) {
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

    fun clearDiagnosticLogs() {
        repository.clearDiagnosticLogs()
    }

    fun disconnect() = repository.disconnect()

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
            val success = repository.sendCommand(command)
            if (!success && command.startsWith("TEXT:")) {
                _uiState.update {
                    it.copy(actionMessage = "Cannot send text on current TV connection")
                }
            }
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
        viewModelScope.launch {
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
                        _uiState.update { it.copy(actionMessage = "ADB thất bại: TV chưa bật gỡ lỗi hoặc sai IP") }
                    }
                } else {
                    _uiState.update { it.copy(actionMessage = "Vui lòng kết nối TV trước khi dùng Quick Launch") }
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
        viewModelScope.launch {
            _uiState.update { it.copy(actionMessage = "Running: ${macro.name}") }
            for (command in macro.commands) {
                sendCommand(command)
                delay(macro.delayMs)
            }
            _uiState.update { it.copy(actionMessage = "${macro.name} done") }
        }
    }

    fun addMacro(macro: Macro) {
        viewModelScope.launch {
            val updated = _macros.value.toMutableList().also { it.add(macro) }
            macroRepository.saveMacros(updated)
        }
    }

    fun removeMacro(macroId: String) {
        viewModelScope.launch {
            val updated = _macros.value.filter { it.id != macroId }
            macroRepository.saveMacros(updated)
        }
    }

    // ----------------------------------------------------------------
    // Media Casting
    // ----------------------------------------------------------------

    fun castVideo(url: String, title: String) {
        castManager.castVideo(url, title)
    }

    fun stopCasting() {
        castManager.stopCasting()
    }

    // Hàm phụ trợ xử lý Socket ADB của Huy chạy trên luồng nền (IO)
    private suspend fun launchAppViaAdb(ip: String, packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, 5555), 2000)
            val out = socket.getOutputStream()
            val command = if (packageName.startsWith("am start")) {
                "$packageName\n"
            } else {
                "monkey -p $packageName -c android.intent.category.LAUNCHER 1\n"
            }
            out.write(command.toByteArray())
            out.flush()
            socket.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    // =======================================================

    fun consumeActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
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
        repository.stopDiscovery()
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
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