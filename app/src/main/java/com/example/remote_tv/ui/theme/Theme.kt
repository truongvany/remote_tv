package com.example.remote_tv.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = OrangeAccent,
    secondary = ButtonBackground,
    tertiary = CardBackground,
    background = DarkBackground,
    surface = DarkBackground,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = TextSecondary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF424242), // Dark Gray for primary in light mode
    secondary = LightButton,
    tertiary = LightCard,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = LightTextPrimary,
    onTertiary = LightTextSecondary,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
)

@Composable
fun REMOTE_TVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}