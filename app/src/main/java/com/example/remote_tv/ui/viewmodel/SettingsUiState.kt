package com.example.remote_tv.ui.viewmodel

import com.example.remote_tv.data.model.AppSettings
import com.example.remote_tv.data.model.UserProfile

data class SettingsUiState(
    val appSettings: AppSettings = AppSettings(),
    val userProfile: UserProfile = UserProfile(),
    val isProfileSaving: Boolean = false,
    val profileError: String? = null
)
