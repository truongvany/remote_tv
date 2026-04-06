package com.example.remote_tv.ui.screens

import androidx.animation.core.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.ui.theme.CardBackground
import com.example.remote_tv.ui.theme.OrangeAccent
import com.example.remote_tv.ui.theme.TextSecondary

@Composable
fun CastScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        CastHeader()

        Spacer(modifier = Modifier.height(40.dp))
        
        // Scanning Animation Area
        ScanningAnimationArea()

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "SCANNING",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Text(
            text = "DETECTING NEARBY DISPLAYS...",
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Nearby Devices Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NEARBY DEVICES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("REFRESH", color = OrangeAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        DeviceItem("Living Room TV", "CONNECTED", isConnected = true)
        Spacer(modifier = Modifier.height(12.dp))
        DeviceItem("Kitchen Tablet", "AVAILABLE")
        Spacer(modifier = Modifier.height(12.dp))
        DeviceItem("Master Bedroom Apple TV", "AVAILABLE")

        Spacer(modifier = Modifier.height(40.dp))

        // Quick Actions Section
        Text(
            text = "QUICK ACTIONS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        ScreenMirroringCard()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QuickActionItem(Icons.Filled.PlayCircle, "YouTube", Color(0xFF2D1010), Color.Red, Modifier.weight(1f))
            QuickActionItem(Icons.Filled.PhotoLibrary, "Photos", Color(0xFF10182D), Color(0xFF4285F4), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun ScanningAnimationArea() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "scale1"
    )
    
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "alpha1"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
        // Ripple Effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = OrangeAccent.copy(alpha = alpha1),
                radius = (size.minDimension / 2) * scale1,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = OrangeAccent.copy(alpha = alpha1 * 0.5f),
                radius = (size.minDimension / 2) * (scale1 * 0.7f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        
        // Center Core
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(OrangeAccent, CircleShape)
                .border(4.dp, Color(0xFFB34A2A).copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Cast, contentDescription = null, tint = Color.Black, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
fun DeviceItem(name: String, status: String, isConnected: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isConnected) Color(0xFF151515) else Color(0xFF0D0D0D))
            .then(if (isConnected) Modifier.border(1.dp, OrangeAccent.copy(alpha = 0.3f), RoundedCornerShape(24.dp)) else Modifier)
            .clickable { }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Tv, contentDescription = null, tint = if (isConnected) OrangeAccent else Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(status, color = if (isConnected) OrangeAccent else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.SignalCellularAlt, contentDescription = null, tint = if (isConnected) OrangeAccent else Color(0xFF333333))
        }
        
        if (isConnected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-20).dp)
                    .width(4.dp)
                    .height(30.dp)
                    .background(OrangeAccent, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun ScreenMirroringCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(CardBackground)
            .padding(24.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(OrangeAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ScreenShare, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Screen Mirroring", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Full device display broadcast", color = Color.Gray, fontSize = 12.sp)
        }
        Icon(
            Icons.Filled.CastConnected, 
            contentDescription = null, 
            tint = Color(0xFF1A1A1A), 
            modifier = Modifier.size(100.dp).align(Alignment.BottomEnd).offset(x = 20.dp, y = 20.dp)
        )
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, title: String, bgColor: Color, iconColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(CardBackground)
            .clickable { }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CastHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(OrangeAccent, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.StayCurrentPortrait, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "COMMAND",
                color = OrangeAccent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = 1.sp
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(20.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF252525))
            )
        }
    }
}
