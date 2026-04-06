package com.example.remote_tv.ui.viewmodel

import android.app.Application
import android.util.Patterns
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

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    private val _settingsUiState = MutableStateFlow(SettingsUiState())
    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState.asStateFlow()

    init {
        repository.startDiscovery()
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
}
