package com.example.remote_tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.remote_tv.data.model.TVApp

// ──────────────── DATA ────────────────

data class AppShortcut(
    val name: String,
    val packageId: String,
    val textColor: Color = Color.White,
    val accentTint: Color = Color.Transparent,
    val themeColor: Color = Color.Gray
)

val defaultAppShortcuts = listOf(
    AppShortcut(name = "NETFLIX",   packageId = "com.netflix.ninja",                 textColor = Color(0xFFE50914), themeColor = Color(0xFFE50914)),
    AppShortcut(name = "YouTube",   packageId = "com.google.android.youtube.tv",     textColor = Color(0xFFFF0000), themeColor = Color(0xFFFF0000)),
    AppShortcut(name = "FPT Play",  packageId = "com.fptplay.nettv",                 textColor = Color(0xFFFF6A2A), themeColor = Color(0xFFFF6A2A)),
)

// ──────────────── QUICK LAUNCH ────────────────

@Composable
fun QuickLaunch(
    apps: List<TVApp> = emptyList(),
    isEnabled: Boolean = true,
    onLaunchApp: (String) -> Unit = {}
) {
    var activeAppId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isEnabled) {
        if (!isEnabled) {
            activeAppId = null
        }
    }

    val displayApps = if (apps.isEmpty()) {
        defaultAppShortcuts
    } else {
        apps.take(3).map { tvApp ->
            val match = defaultAppShortcuts.find {
                it.packageId == tvApp.id || it.name.contains(tvApp.name, ignoreCase = true)
            }
            match?.copy(name = tvApp.name, packageId = tvApp.id) ?: AppShortcut(
                name       = tvApp.name.take(12),
                packageId  = tvApp.id,
                textColor  = MaterialTheme.colorScheme.onSecondary,
                accentTint = Color.Transparent
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Label
        Text(
            text          = "QUICK LAUNCH",
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color         = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            displayApps.forEach { app ->
                val iconUrl = apps.find { it.id == app.packageId }?.iconUrl
                val isActive = (app.packageId == activeAppId) && isEnabled

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppCard(
                        app       = app,
                        iconUrl   = iconUrl,
                        isEnabled = isEnabled,
                        isActive  = isActive,
                        modifier  = Modifier.fillMaxWidth(),
                        onClick   = { 
                            if (isEnabled) activeAppId = app.packageId
                            onLaunchApp(app.packageId) 
                        }
                    )
                    
                    if (isActive) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "đang phát",
                            fontSize = 10.sp,
                            color = app.themeColor,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        // Keep consistent height
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
            }
        }
    }
}

// ──────────────── APP CARD ────────────────

@Composable
fun AppCard(
    app: AppShortcut,
    iconUrl: String? = null,
    isEnabled: Boolean = true,
    isActive: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue    = if (isPressed) 0.95f else 1f,
        animationSpec  = spring(dampingRatio = 0.7f),
        label          = "scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (isEnabled) 1f else 0.6f)
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) app.themeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondary)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) app.themeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = true // Allow click regardless of isEnabled to show error message
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (iconUrl != null) {
            AsyncImage(
                model            = iconUrl,
                contentDescription = app.name,
                modifier         = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale     = ContentScale.Fit
            )
        } else {
            ShortcutLogoContent(app)
        }
    }
}

// ──────────────── SHORTCUT LOGO ────────────────

@Composable
private fun ShortcutLogoContent(app: AppShortcut) {
    when (app.name) {
        "YouTube", "YouTube TV" -> {
            Row(
                verticalAlignment      = Alignment.CenterVertically,
                horizontalArrangement  = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "YouTube",
                    tint               = Color(0xFFFF0000),
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text       = "YouTube",
                    color      = MaterialTheme.colorScheme.onSecondary,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        "NETFLIX" -> {
            Text(
                text       = "NETFLIX",
                color      = Color(0xFFE50914),
                fontSize   = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }

        "FPT Play" -> {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Filled.Tv,
                    contentDescription = "FPT Play",
                    tint               = Color(0xFFFF6A2A),
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text       = "FPT",
                    color      = Color(0xFFFF6A2A),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        "YT Music" -> {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = "YT Music",
                    tint               = Color(0xFFFF0044),
                    modifier           = Modifier.size(16.dp)
                )
                Text(
                    text       = "Music",
                    color      = MaterialTheme.colorScheme.onSecondary,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        "Prime Video" -> {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Prime Video",
                    tint               = Color(0xFF00A0D6),
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text       = "Prime",
                    color      = MaterialTheme.colorScheme.onSecondary,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        "Disney+" -> {
            Text(
                text       = "Disney+",
                color      = Color(0xFF2F7CFF),
                fontSize   = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle  = FontStyle.Italic
            )
        }

        else -> {
            Text(
                text       = app.name.take(8),
                color      = app.textColor,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
