package com.example.remote_tv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceListeningOverlay(
    statusText: String,
    transcript: String,
    rmsLevel: Float,
    onStop: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "voice-overlay")
    val orbit = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(3600, easing = LinearEasing)),
        label = "voice-orbit-progress"
    )
    val glow = transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1300, easing = LinearEasing)),
        label = "voice-glow"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
            .clickable { onStop() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val particles = 34
            val baseRadius = size.minDimension * 0.16f
            val dynamicBoost = (rmsLevel.coerceIn(0f, 12f) / 12f) * 26f

            repeat(particles) { index ->
                val normalized = index / particles.toFloat()
                val direction = if (index % 2 == 0) 1f else -1f
                val angle = (normalized * 2f * PI.toFloat()) + (orbit.value * 2f * PI.toFloat() * direction)
                val layer = 1f + (index % 4) * 0.22f
                val radius = (baseRadius + dynamicBoost) * layer
                val x = center.x + cos(angle) * radius
                val y = center.y + sin(angle) * radius * 0.72f
                val particleSize = 1.8f + ((index % 5) * 1.1f)
                val alpha = 0.23f + ((index % 6) * 0.09f) * glow.value
                
                drawCircle(
                    color = primaryColor.copy(alpha = alpha.coerceAtMost(0.9f)),
                    radius = particleSize,
                    center = Offset(x, y)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size((112 + rmsLevel.coerceIn(0f, 10f) * 1.8f).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Listening",
                    tint = onPrimaryColor,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = statusText,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (transcript.isBlank()) "Say something..." else transcript,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    .clickable { onStop() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = "Stop listening",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
