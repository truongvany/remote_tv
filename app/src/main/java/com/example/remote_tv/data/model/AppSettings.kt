package com.example.remote_tv.data.model

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val languageCode: String = "vi",
    val autoReconnectLastDevice: Boolean = true,
    val autoScanOnCastTab: Boolean = true,
    val keepScreenOn: Boolean = false,
)
