package com.example.remote_tv.data.preferences

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleManager {
    fun applyLanguage(languageCode: String) {
        val targetLocales = LocaleListCompat.forLanguageTags(languageCode)
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() == targetLocales.toLanguageTags()) {
            return
        }
        AppCompatDelegate.setApplicationLocales(targetLocales)
    }
}
