package com.example.remote_tv.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // Animation state for icon scale
    val infiniteRotation = rememberInfiniteTransition(label = "rotation")
    val scale by infiniteRotation.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Pulse animation for glow effect
    val alphaRotation = rememberInfiniteTransition(label = "alpha")
    val glowAlpha by alphaRotation.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Trigger navigation after splash delay
    LaunchedEffect(Unit) {
        delay(3000) // 3 seconds splash screen
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0a0e27),
                        Color(0xFF2d1b4e),
                        Color(0xFF0a0e27)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glow background circle
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(
                        color = Color(0xFF00d4ff).copy(alpha = glowAlpha * 0.2f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .alpha(glowAlpha * 0.3f),
                contentAlignment = Alignment.Center
            ) {
                // Icon
                Icon(
                    painter = painterResource(id = R.drawable.ic_neon_remote),
                    contentDescription = "Remote Icon",
                    modifier = Modifier
                        .size(200.dp)
                        .scale(scale),
                    tint = Color(0xFF00d4ff)
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // App name
            Text(
                text = "COMMAND",
                color = Color(0xFF00d4ff),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "Universal Remote Control",
                color = Color(0xFF00d4ff).copy(alpha = 0.65f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(80.dp))

            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = Color(0xFF00d4ff),
                strokeWidth = 2.dp
            )
        }
    }
}
