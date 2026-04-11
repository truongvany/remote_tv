package com.example.remote_tv.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.remote_tv.data.model.AppSettings
import com.example.remote_tv.data.model.AppThemeMode
import com.example.remote_tv.data.model.SavedDevice
import com.example.remote_tv.data.model.TVBrand
import com.example.remote_tv.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore by preferencesDataStore(name = "app_preferences")

class AppPreferencesRepository(private val context: Context) {

    val appSettingsFlow: Flow<AppSettings> = context.appPreferencesDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[PreferencesKeys.themeMode]?.toAppThemeMode() ?: AppThemeMode.DARK,
            languageCode = prefs[PreferencesKeys.languageCode] ?: "en",
            autoReconnectLastDevice = prefs[PreferencesKeys.autoReconnectLastDevice] ?: true,
            autoScanOnCastTab = prefs[PreferencesKeys.autoScanOnCastTab] ?: true,
            keepScreenOn = prefs[PreferencesKeys.keepScreenOn] ?: false,
        )
    }

    val userProfileFlow: Flow<UserProfile> = context.appPreferencesDataStore.data.map { prefs ->
        UserProfile(
            id = prefs[PreferencesKeys.userId] ?: "local_user",
            displayName = prefs[PreferencesKeys.displayName] ?: "Guest",
            email = prefs[PreferencesKeys.email] ?: "",
            avatarSeed = prefs[PreferencesKeys.avatarSeed] ?: 1
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.themeMode] = mode.name
        }
    }

    suspend fun setLanguageCode(languageCode: String) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.languageCode] = languageCode
        }
    }

    suspend fun setAutoReconnectLastDevice(enabled: Boolean) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.autoReconnectLastDevice] = enabled
        }
    }

    suspend fun setAutoScanOnCastTab(enabled: Boolean) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.autoScanOnCastTab] = enabled
        }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.keepScreenOn] = enabled
        }
    }

    suspend fun updateUserProfile(displayName: String, email: String) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.displayName] = displayName.trim()
            prefs[PreferencesKeys.email] = email.trim()
        }
    }

    suspend fun setAvatarSeed(seed: Int) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.avatarSeed] = seed
        }
    }

    // ----------------------------------------------------------------
    // Last Device (Auto-Reconnect + Wake-on-LAN)
    // ----------------------------------------------------------------

    val lastDeviceFlow: Flow<SavedDevice?> = context.appPreferencesDataStore.data.map { prefs ->
        val ip = prefs[PreferencesKeys.lastDeviceIp] ?: return@map null
        val port = prefs[PreferencesKeys.lastDevicePort] ?: return@map null
        val brandName = prefs[PreferencesKeys.lastDeviceBrand] ?: return@map null
        val name = prefs[PreferencesKeys.lastDeviceName] ?: return@map null
        val mac = prefs[PreferencesKeys.lastDeviceMac]
        SavedDevice(
            ip = ip,
            port = port,
            brand = runCatching { TVBrand.valueOf(brandName) }.getOrDefault(TVBrand.UNKNOWN),
            name = name,
            macAddress = mac,
        )
    }

    suspend fun saveLastDevice(device: SavedDevice) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.lastDeviceIp] = device.ip
            prefs[PreferencesKeys.lastDevicePort] = device.port
            prefs[PreferencesKeys.lastDeviceBrand] = device.brand.name
            prefs[PreferencesKeys.lastDeviceName] = device.name
            device.macAddress?.let { prefs[PreferencesKeys.lastDeviceMac] = it }
        }
    }

    suspend fun clearLastDevice() {
        context.appPreferencesDataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.lastDeviceIp)
            prefs.remove(PreferencesKeys.lastDevicePort)
            prefs.remove(PreferencesKeys.lastDeviceBrand)
            prefs.remove(PreferencesKeys.lastDeviceName)
            prefs.remove(PreferencesKeys.lastDeviceMac)
        }
    }

    suspend fun loadLastDevice(): SavedDevice? = lastDeviceFlow.first()

    suspend fun saveLastDeviceMac(mac: String) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.lastDeviceMac] = mac
        }
    }

    // ----------------------------------------------------------------
    // Macros — stored as JSON string list
    // ----------------------------------------------------------------

    val macrosJsonFlow: Flow<String> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[PreferencesKeys.macrosJson] ?: "[]"
    }

    suspend fun saveMacrosJson(json: String) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.macrosJson] = json
        }
    }

    private object PreferencesKeys {
        val themeMode: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        val languageCode: Preferences.Key<String> = stringPreferencesKey("language_code")
        val autoReconnectLastDevice: Preferences.Key<Boolean> = booleanPreferencesKey("auto_reconnect_last_device")
        val autoScanOnCastTab: Preferences.Key<Boolean> = booleanPreferencesKey("auto_scan_on_cast_tab")
        val keepScreenOn: Preferences.Key<Boolean> = booleanPreferencesKey("keep_screen_on")

        val userId: Preferences.Key<String> = stringPreferencesKey("user_id")
        val displayName: Preferences.Key<String> = stringPreferencesKey("display_name")
        val email: Preferences.Key<String> = stringPreferencesKey("email")
        val avatarSeed: Preferences.Key<Int> = intPreferencesKey("avatar_seed")

        // Last device (auto-reconnect + WoL)
        val lastDeviceIp: Preferences.Key<String> = stringPreferencesKey("last_device_ip")
        val lastDevicePort: Preferences.Key<Int> = intPreferencesKey("last_device_port")
        val lastDeviceBrand: Preferences.Key<String> = stringPreferencesKey("last_device_brand")
        val lastDeviceName: Preferences.Key<String> = stringPreferencesKey("last_device_name")
        val lastDeviceMac: Preferences.Key<String> = stringPreferencesKey("last_device_mac")

        // Macros
        val macrosJson: Preferences.Key<String> = stringPreferencesKey("macros_json")
    }
}

private fun String.toAppThemeMode(): AppThemeMode {
    return runCatching { AppThemeMode.valueOf(this) }.getOrDefault(AppThemeMode.DARK)
}
