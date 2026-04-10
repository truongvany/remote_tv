package com.example.remote_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.data.model.TVApp

data class AppShortcut(
    val name: String,
    val packageId: String,
    val textColor: Color = Color.White,
    val accentTint: Color = Color.Transparent
)

val defaultAppShortcuts = listOf(
    AppShortcut("NETFLIX", "com.netflix.ninja", Color(0xFFE50914), Color(0x11FF0000)),
    AppShortcut("YouTube", "com.google.android.youtube.tv", Color(0xFFFA3D3D), Color(0x11FF4E4E)),
    AppShortcut("Disney+", "com.disney.disneyplus", Color(0xFF2F7CFF), Color(0x112F7CFF)),
)

@Composable
fun QuickLaunch(
    apps: List<TVApp> = emptyList(),
    isEnabled: Boolean = true,
    onLaunchApp: (String) -> Unit = {}
) {
    val displayApps = if (apps.isEmpty()) {
        defaultAppShortcuts
    } else {
        apps.take(3).map { tvApp ->
            val defaultMatch = defaultAppShortcuts.find { it.packageId == tvApp.id || it.name.contains(tvApp.name, ignoreCase = true) }
            defaultMatch?.copy(name = tvApp.name, packageId = tvApp.id) ?: AppShortcut(
                name = tvApp.name.take(12),
                packageId = tvApp.id,
                textColor = MaterialTheme.colorScheme.onSecondary,
                accentTint = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.05f)
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QUICK LAUNCH",
                color = MaterialTheme.colorScheme.onTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            displayApps.forEach { app ->
                val iconUrl = apps.find { it.id == app.packageId }?.iconUrl
                AppCard(
                    app = app,
                    iconUrl = iconUrl,
                    isEnabled = isEnabled,
                    modifier = Modifier.weight(1f),
                    onClick = { onLaunchApp(app.packageId) }
                )
            }
        }
    }
}

@Composable
fun AppCard(
    app: AppShortcut,
    iconUrl: String? = null,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .alpha(if (isEnabled) 1f else 0.45f)
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable(enabled = isEnabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(app.accentTint)
        )

        if (iconUrl != null) {
            AsyncImage(
                model = iconUrl,
                contentDescription = app.name,
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            when (app.name) {
                "YouTube", "YouTube TV" -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Filled.SmartDisplay,
                            contentDescription = "YouTube",
                            tint = Color(0xFFFA3D3D),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "YouTube",
                            color = if (MaterialTheme.colorScheme.background == Color(0xFFF5F5F5)) Color(0xFF333333) else app.textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                "Disney+" -> {
                    Text(
                        text = app.name,
                        color = if (MaterialTheme.colorScheme.background == Color(0xFFF5F5F5)) Color(0xFF2F7CFF) else app.textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic
                    )
                }

                "NETFLIX" -> {
                    Text(
                        text = app.name,
                        color = if (MaterialTheme.colorScheme.background == Color(0xFFF5F5F5)) Color(0xFFE50914) else app.textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                else -> {
                    Text(
                        text = app.name,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
