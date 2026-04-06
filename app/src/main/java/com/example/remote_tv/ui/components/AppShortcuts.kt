package com.example.remote_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.ui.theme.ButtonBackground
import com.example.remote_tv.ui.theme.TextSecondary

data class AppShortcut(
    val name: String,
    val packageId: String,
    val textColor: Color = Color.White,
    val accentTint: Color = Color.Transparent
)

val defaultAppShortcuts = listOf(
    AppShortcut("NETFLIX", "com.netflix.ninja", Color(0xFFE50914), Color(0x33FF0000)),
    AppShortcut("YouTube", "com.google.android.youtube.tv", Color.White, Color(0x33FF4E4E)),
    AppShortcut("Disney+", "com.disney.disneyplus", Color(0xFF70A9FF), Color(0x332F7CFF)),
)

@Composable
fun QuickLaunch(
    apps: List<AppShortcut> = defaultAppShortcuts,
    isEnabled: Boolean = true,
    onLaunchApp: (String) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QUICK LAUNCH",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color(0xFF1D1D1D))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            apps.forEach { app ->
                AppCard(
                    app = app,
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
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .alpha(if (isEnabled) 1f else 0.45f)
            .height(74.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(ButtonBackground)
            .border(1.dp, Color(0xFF252525), RoundedCornerShape(18.dp))
            .clickable(enabled = isEnabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(app.accentTint)
        )

        when (app.name) {
            "YouTube" -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Filled.SmartDisplay,
                        contentDescription = "YouTube",
                        tint = Color(0xFFFA3D3D)
                    )
                    Text(
                        text = app.name,
                        color = app.textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            "Disney+" -> {
                Text(
                    text = app.name,
                    color = app.textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic
                )
            }

            else -> {
                Text(
                    text = app.name,
                    color = app.textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
