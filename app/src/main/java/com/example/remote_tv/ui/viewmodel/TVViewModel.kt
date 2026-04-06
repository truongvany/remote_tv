package com.example.remote_tv.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import java.net.Inet4Address
import android.util.Patterns
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.remote_tv.data.model.AppThemeMode
import com.example.remote_tv.data.model.TVDevice
import com.example.remote_tv.data.preferences.AppPreferencesRepository
import com.example.remote_tv.data.repository.TVRepository
import com.example.remote_tv.data.repository.TVRepositoryImpl
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TVViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TVRepository = TVRepositoryImpl(application)
    private val appPreferencesRepository = AppPreferencesRepository(application)

    val discoveredDevices: StateFlow<List<TVDevice>> = repository.discoveredDevices
    val currentDevice: StateFlow<TVDevice?> = repository.currentDevice
    val isScanning: StateFlow<Boolean> = repository.isScanning
    val scanError: StateFlow<String?> = repository.scanError

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    private val _settingsUiState = MutableStateFlow(SettingsUiState())
    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                hasLocationPermission = hasFineLocationPermission(),
                localIpAddress = resolveLocalIpv4Address(),
                localSubnet = resolveLocalSubnet(),
            )
        }
        observeSettings()
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
    }

    fun disconnect() = repository.disconnect()

    fun sendCommand(command: String) {
        viewModelScope.launch { repository.sendCommand(command) }
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

    fun launchApp(appId: String) {
        viewModelScope.launch { repository.launchApp(appId) }
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
        val network = cm.activeNetwork ?: return null
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
        val network = cm.activeNetwork ?: return null
        val linkProperties: LinkProperties = cm.getLinkProperties(network) ?: return null

        val linkAddress = linkProperties.linkAddresses.firstOrNull { address ->
            val inetAddress = address.address
            inetAddress is Inet4Address && !inetAddress.isLoopbackAddress
        } ?: return null

        val hostAddress = linkAddress.address.hostAddress ?: return null
        val prefixLength = linkAddress.prefixLength
        return "$hostAddress/$prefixLength"
    }
}
