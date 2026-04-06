package com.example.remote_tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.remote_tv.data.model.AppThemeMode
import com.example.remote_tv.data.preferences.LocaleManager
import com.example.remote_tv.ui.screens.RemoteScreen
import com.example.remote_tv.ui.theme.REMOTE_TVTheme
import com.example.remote_tv.ui.viewmodel.TVViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: TVViewModel = viewModel()
            val settingsUiState by viewModel.settingsUiState.collectAsState()

            LaunchedEffect(settingsUiState.appSettings.languageCode) {
                LocaleManager.applyLanguage(settingsUiState.appSettings.languageCode)
            }

            REMOTE_TVTheme(darkTheme = settingsUiState.appSettings.themeMode == AppThemeMode.DARK) {
                RemoteScreen(viewModel = viewModel)
            }
        }
    }
}
