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
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RemoteIconButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                label = "BACK",
                modifier = Modifier.weight(1f),
            ) { onCommand("KEY_BACK") }
            RemoteIconButton(
                icon = Icons.Filled.Home,
                label = "HOME",
                modifier = Modifier.weight(1f),
            ) { onCommand("KEY_HOME") }
            RemoteIconButton(
                icon = Icons.Filled.Menu,
                label = "MENU",
                modifier = Modifier.weight(1f),
            ) { onCommand("KEY_MENU") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteIconButton(
                icon = Icons.Filled.Search,
                label = "SEARCH",
                modifier = Modifier.weight(1f),
            ) { onCommand("KEY_SEARCH") }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                VoiceButton(
                    isListening = isVoiceListening,
                    onClick = onVoiceClick,
                )
            }

            RemoteIconButton(
                icon = Icons.AutoMirrored.Filled.VolumeOff,
                label = "MUTE",
                modifier = Modifier.weight(1f),
            ) { onCommand("KEY_MUTE") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ConnectedDualButton(
                title = "VOL",
                topIcon = Icons.AutoMirrored.Filled.VolumeUp,
                bottomIcon = Icons.AutoMirrored.Filled.VolumeDown,
                topLabel = "+",
                bottomLabel = "-",
                modifier = Modifier.weight(1f),
                onTopClick = { onCommand("KEY_VOL_UP") },
                onBottomClick = { onCommand("KEY_VOL_DOWN") },
            )

            ConnectedDualButton(
                title = "CH",
                topIcon = Icons.Filled.KeyboardArrowUp,
                bottomIcon = Icons.Filled.KeyboardArrowDown,
                topLabel = "+",
                bottomLabel = "-",
                modifier = Modifier.weight(1f),
                onTopClick = { onCommand("KEY_CH_UP") },
                onBottomClick = { onCommand("KEY_CH_DOWN") },
            )
        }
    }
}

@Composable
fun RemoteIconButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .background(ButtonBackground, RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF262626), RoundedCornerShape(20.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color(0xFFA8A8A8),
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ConnectedDualButton(
    title: String,
    topIcon: ImageVector,
    bottomIcon: ImageVector,
    topLabel: String,
    bottomLabel: String,
    modifier: Modifier = Modifier,
    onTopClick: () -> Unit,
    onBottomClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .background(ButtonBackground, RoundedCornerShape(22.dp))
                .border(1.dp, Color(0xFF262626), RoundedCornerShape(22.dp)),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable { onTopClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(topIcon, contentDescription = "$title up", tint = Color(0xFFA8A8A8), modifier = Modifier.size(24.dp))
                        Text(topLabel, color = Color(0xFFA8A8A8), fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0xFF242424), thickness = 1.dp)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable { onBottomClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(bottomIcon, contentDescription = "$title down", tint = Color(0xFFA8A8A8), modifier = Modifier.size(24.dp))
                        Text(bottomLabel, color = Color(0xFFA8A8A8), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text(
            text = title,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
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
            .size(76.dp)
            .shadow(12.dp, CircleShape)
            .background(OrangeAccent, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            repeat(4) { index ->
                val angle = Math.toRadians((rotation.value + index * 90f).toDouble())
                val radius = 24f
                val offsetX = (cos(angle) * radius).toFloat().dp
                val offsetY = (sin(angle) * radius).toFloat().dp

                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .size(if (index % 2 == 0) 6.dp else 4.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                )
            }
        }

        Icon(
            Icons.Filled.Mic,
            contentDescription = "Voice",
            tint = Color.Black,
            modifier = Modifier.size(32.dp)
        )
    }
}
