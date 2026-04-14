package com.example.remote_tv

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.remote_tv.data.model.AppThemeMode
import com.example.remote_tv.data.preferences.LocaleManager
import com.example.remote_tv.ui.screens.RemoteScreen
import com.example.remote_tv.ui.screens.SplashScreen
import com.example.remote_tv.ui.theme.REMOTE_TVTheme
import com.example.remote_tv.ui.viewmodel.TVViewModel
import com.google.android.gms.cast.framework.CastContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        runCatching {
            CastContext.getSharedInstance(this)
            Log.d("MainActivity", "CastContext warmed up")
        }.onFailure { error ->
            Log.w("MainActivity", "CastContext warm-up failed: ${error.message}")
        }

        setContent {
            val viewModel: TVViewModel = viewModel()
            val settingsUiState by viewModel.settingsUiState.collectAsState()
            val showSplash = remember { mutableStateOf(true) }

            LaunchedEffect(settingsUiState.appSettings.languageCode) {
                LocaleManager.applyLanguage(settingsUiState.appSettings.languageCode)
            }

            LaunchedEffect(settingsUiState.appSettings.keepScreenOn) {
                if (settingsUiState.appSettings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            REMOTE_TVTheme(darkTheme = settingsUiState.appSettings.themeMode == AppThemeMode.DARK) {
                if (showSplash.value) {
                    SplashScreen(onSplashFinished = { showSplash.value = false })
                } else {
                    RemoteScreen(viewModel = viewModel)
                }
            }
        }
    }
}
