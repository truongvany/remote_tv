package com.example.remote_tv.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.remote_tv.data.model.AppSettings
import com.example.remote_tv.data.model.AppThemeMode
import com.example.remote_tv.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore by preferencesDataStore(name = "app_preferences")

class AppPreferencesRepository(private val context: Context) {

    val appSettingsFlow: Flow<AppSettings> = context.appPreferencesDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[PreferencesKeys.themeMode]?.toAppThemeMode() ?: AppThemeMode.DARK,
            languageCode = prefs[PreferencesKeys.languageCode] ?: "en"
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

    private object PreferencesKeys {
        val themeMode: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        val languageCode: Preferences.Key<String> = stringPreferencesKey("language_code")

        val userId: Preferences.Key<String> = stringPreferencesKey("user_id")
        val displayName: Preferences.Key<String> = stringPreferencesKey("display_name")
        val email: Preferences.Key<String> = stringPreferencesKey("email")
        val avatarSeed: Preferences.Key<Int> = intPreferencesKey("avatar_seed")
    }
}

private fun String.toAppThemeMode(): AppThemeMode {
    return runCatching { AppThemeMode.valueOf(this) }.getOrDefault(AppThemeMode.DARK)
}
