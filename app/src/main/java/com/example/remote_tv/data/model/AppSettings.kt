package com.example.remote_tv.data.model

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val languageCode: String = "en"
)
