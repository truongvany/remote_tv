package com.example.remote_tv.ui.screens

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote_tv.data.model.TVBrand
import com.example.remote_tv.data.model.TVDevice
import com.example.remote_tv.ui.theme.CardBackground
import com.example.remote_tv.ui.theme.OrangeAccent
import com.example.remote_tv.ui.theme.TextSecondary

@Composable
fun CastScreen(
    devices: List<TVDevice>,
    connectedDevice: TVDevice?,
    connectingDeviceKey: String?,
    isScanning: Boolean,
    scanError: String?,
    connectionError: String?,
    hasLocationPermission: Boolean,
    localIpAddress: String?,
    localSubnet: String?,
    diagnosticLogs: List<String>,
    onRequestPermission: () -> Unit,
    onRefreshScan: () -> Unit,
    onDeviceSelected: (TVDevice) -> Unit,
    onClearDiagnostics: () -> Unit,
) {
    var showDebugInfo by rememberSaveable { mutableStateOf(false) }

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
            text = if (isScanning) "DETECTING NEARBY DISPLAYS..." else "SCAN COMPLETE",
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Nearby Devices Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "NEARBY DEVICES",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (showDebugInfo) "HIDE DEBUG" else "DEBUG INFO",
                color = Color(0xFFFFB38F),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.clickable { showDebugInfo = !showDebugInfo },
            )
            Text(
                "REFRESH",
                color = OrangeAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.clickable { onRefreshScan() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showDebugInfo) {
            DebugInfoCard(
                localIpAddress = localIpAddress,
                localSubnet = localSubnet,
                isScanning = isScanning,
                hasLocationPermission = hasLocationPermission,
                logs = diagnosticLogs,
                onClearLogs = onClearDiagnostics,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (connectionError != null) {
            ConnectionErrorCard(message = connectionError)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!hasLocationPermission) {
            PermissionRequiredCard(onRequestPermission = onRequestPermission)
        } else if (scanError != null) {
            ScanErrorCard(message = scanError, onRetry = onRefreshScan)
        } else if (devices.isEmpty()) {
            EmptyDiscoveryCard(isScanning = isScanning)
        } else {
            devices.forEachIndexed { index, device ->
                val connectionKey = "${device.ipAddress}:${device.port}"
                val isConnected =
                    connectedDevice?.ipAddress == device.ipAddress &&
                        connectedDevice?.port == device.port
                val isConnecting = connectingDeviceKey == connectionKey
                DeviceItem(
                    device = device,
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    onClick = { onDeviceSelected(device) }
                )

                if (index < devices.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

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
fun DeviceItem(device: TVDevice, isConnected: Boolean, isConnecting: Boolean, onClick: () -> Unit) {
    val status = when {
        isConnecting -> "CONNECTING..."
        isConnected -> "CONNECTED"
        else -> "AVAILABLE"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isConnected) Color(0xFF151515) else Color(0xFF0D0D0D))
            .then(if (isConnected) Modifier.border(1.dp, OrangeAccent.copy(alpha = 0.3f), RoundedCornerShape(24.dp)) else Modifier)
            .clickable(enabled = !isConnecting) { onClick() }
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
                Icon(
                    imageVector = iconForBrand(device.brand),
                    contentDescription = null,
                    tint = if (isConnected) OrangeAccent else Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(device.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    status,
                    color = if (isConnected || isConnecting) OrangeAccent else Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = OrangeAccent,
                    )
                } else {
                    Icon(
                        Icons.Filled.SignalCellularAlt,
                        contentDescription = null,
                        tint = if (isConnected) OrangeAccent else Color(0xFF333333)
                    )
                }
                Text(
                    text = "${device.ipAddress}:${device.port}",
                    color = Color(0xFF7A7A7A),
                    fontSize = 10.sp,
                )
            }
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
private fun PermissionRequiredCard(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF121212))
            .border(1.dp, OrangeAccent.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = "Location Permission Required",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Grant location permission to scan devices on your current Wi-Fi network.",
                color = Color.Gray,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) {
                Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ScanErrorCard(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF191113))
            .border(1.dp, Color(0xFF7A2B2B), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Text("Scan error", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = Color(0xFFE2B9B9), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onRetry) {
                Text("Try again", color = OrangeAccent)
            }
        }
    }
}

@Composable
private fun ConnectionErrorCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1A1210))
            .border(1.dp, Color(0xFF8A533E), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Connection hint",
                color = Color(0xFFFFC7A3),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = Color(0xFFE5C0AF),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun EmptyDiscoveryCard(isScanning: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0C0C0C))
            .border(1.dp, Color(0xFF1F1F1F), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Text(
            text = if (isScanning) {
                "Scanning IP range in your Wi-Fi subnet..."
            } else {
                "No TV found. Tap REFRESH or use manual IP connection."
            },
            color = Color(0xFFB3B3B3),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun DebugInfoCard(
    localIpAddress: String?,
    localSubnet: String?,
    isScanning: Boolean,
    hasLocationPermission: Boolean,
    logs: List<String>,
    onClearLogs: () -> Unit,
) {
    val ipText = localIpAddress ?: "Unavailable"
    val subnetText = localSubnet ?: "Unavailable"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF121212))
            .border(1.dp, Color(0xFF2B2B2B), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Debug Network Info",
                    color = Color(0xFFFFB38F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onClearLogs) {
                    Text("Clear Logs", color = Color(0xFFFFB38F), fontSize = 11.sp)
                }
            }
            Text(text = "Local IP: $ipText", color = Color(0xFFE0E0E0), fontSize = 12.sp)
            Text(text = "Subnet: $subnetText", color = Color(0xFFE0E0E0), fontSize = 12.sp)
            Text(
                text = "Permission: ${if (hasLocationPermission) "Granted" else "Missing"}",
                color = Color(0xFF9E9E9E),
                fontSize = 11.sp,
            )
            Text(
                text = "Scan status: ${if (isScanning) "Running" else "Idle"}",
                color = Color(0xFF9E9E9E),
                fontSize = 11.sp,
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "In-app Logs",
                color = Color(0xFFFFB38F),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )

            if (logs.isEmpty()) {
                Text(
                    text = "No logs yet. Tap a TV or press remote buttons to capture events.",
                    color = Color(0xFF8E8E8E),
                    fontSize = 11.sp,
                )
            } else {
                logs.takeLast(12).forEach { line ->
                    Text(
                        text = line,
                        color = Color(0xFFB9B9B9),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                    )
                }
            }
        }
    }
}

private fun iconForBrand(brand: TVBrand): ImageVector {
    return when (brand) {
        TVBrand.ROKU -> Icons.Filled.Tv
        TVBrand.FIRE_TV -> Icons.Filled.LiveTv
        TVBrand.SAMSUNG -> Icons.Filled.Tv
        TVBrand.LG -> Icons.Filled.Tv
        TVBrand.ANDROID_TV -> Icons.Filled.Cast
        TVBrand.UNKNOWN -> Icons.Filled.Tv
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
