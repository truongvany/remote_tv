package com.example.remote_tv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.ui.theme.ButtonBackground
import com.example.remote_tv.ui.theme.OrangeAccent
import com.example.remote_tv.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ControlButtons(
    onCommand: (String) -> Unit,
    isVoiceListening: Boolean = false,
    onVoiceClick: () -> Unit = { onCommand("KEY_VOICE") },
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RemoteIconButton(Icons.AutoMirrored.Filled.Undo, "BACK") { onCommand("KEY_BACK") }
            RemoteIconButton(Icons.Filled.Home, "HOME") { onCommand("KEY_HOME") }
            RemoteIconButton(Icons.Filled.Menu, "MENU") { onCommand("KEY_MENU") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteIconButton(Icons.Filled.Search, "SEARCH") { onCommand("KEY_SEARCH") }

            VoiceButton(
                isListening = isVoiceListening,
                onClick = onVoiceClick,
            )

            RemoteIconButton(Icons.AutoMirrored.Filled.VolumeOff, "MUTE") { onCommand("KEY_MUTE") }
        }
    }
}

@Composable
fun RemoteIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(ButtonBackground, RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFF262626), RoundedCornerShape(24.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color(0xFFA8A8A8),
                modifier = Modifier.size(34.dp)
            )
        }
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun VoiceButton(
    isListening: Boolean,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "voice-orbit")
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing)
        ),
        label = "voice-rotation"
    )

    Box(
        modifier = Modifier
            .size(92.dp)
            .shadow(18.dp, CircleShape)
            .background(OrangeAccent, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            repeat(4) { index ->
                val angle = Math.toRadians((rotation.value + index * 90f).toDouble())
                val radius = 28f
                val offsetX = (cos(angle) * radius).toFloat().dp
                val offsetY = (sin(angle) * radius).toFloat().dp

                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .size(if (index % 2 == 0) 8.dp else 6.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                )
            }
        }

        Icon(
            Icons.Filled.Mic,
            contentDescription = "Voice",
            tint = Color.Black,
            modifier = Modifier.size(40.dp)
        )
    }
}
