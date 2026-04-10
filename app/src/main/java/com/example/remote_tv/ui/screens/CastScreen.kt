package com.example.remote_tv.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.example.remote_tv.data.debug.InAppDiagnostics
import com.example.remote_tv.data.model.TVBrand
import com.example.remote_tv.data.model.TVDevice
import com.google.android.gms.cast.framework.CastButtonFactory

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
    isCastSessionActive: Boolean,
    castStatus: String,
    onRequestPermission: () -> Unit,
    onRefreshScan: () -> Unit,
    onDeviceSelected: (TVDevice) -> Unit,
    onClearDiagnostics: () -> Unit,
    onScreenMirroringClick: () -> Unit = {},
    onPickImage: () -> Unit = {},
    onPickVideo: () -> Unit = {},
    onStopCasting: () -> Unit = {},
) {
    var showDebugInfo by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

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
        
        ScanningAnimationArea()

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "SCANNING",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Text(
            text = if (isScanning) "DETECTING NEARBY DISPLAYS..." else "SCAN COMPLETE",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))
        CastSessionStatusPill(
            isActive = isCastSessionActive,
            status = castStatus,
        )
        if (!isCastSessionActive) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tip: Connect a TV in the list to push media directly, or use cast icon (top-right)",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontSize = 10.sp,
                lineHeight = 13.sp,
            )

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    openCastSettings(context)
                    Toast.makeText(context, "Choose Google TV in Cast settings first", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            ) {
                Icon(Icons.Filled.Cast, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect Cast Session", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "NEARBY DEVICES",
                color = MaterialTheme.colorScheme.onTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (showDebugInfo) "HIDE DEBUG" else "DEBUG INFO",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.clickable { showDebugInfo = !showDebugInfo },
            )
            Text(
                "REFRESH",
                color = MaterialTheme.colorScheme.primary,
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

        Text(
            text = "QUICK ACTIONS",
            color = MaterialTheme.colorScheme.onTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        ScreenMirroringCard(onClick = onScreenMirroringClick)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QuickActionItem(
                Icons.Filled.PhotoLibrary,
                "Cast Image",
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                MaterialTheme.colorScheme.primary,
                Modifier.weight(1f),
                onClick = onPickImage,
            )
            QuickActionItem(
                Icons.Filled.PlayCircle,
                "Cast Video",
                Color.Red.copy(alpha = 0.1f),
                Color.Red,
                Modifier.weight(1f),
                onClick = onPickVideo,
            )
        }

        if (isCastSessionActive) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onStopCasting,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Filled.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop Casting", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun CastSessionStatusPill(isActive: Boolean, status: String) {
    val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val background = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.secondary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = status,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun ScanningAnimationArea() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val pulseColor = MaterialTheme.colorScheme.primary
    
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = pulseColor.copy(alpha = alpha1 * 0.55f),
                radius = (size.minDimension / 2) * scale1,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Cast, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
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
            .background(if (isConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary)
            .then(if (isConnected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp)) else Modifier)
            .clickable(enabled = !isConnecting) { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForBrand(device.brand),
                    contentDescription = null,
                    tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(device.name, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    status,
                    color = if (isConnected || isConnecting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
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
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        Icons.Filled.SignalCellularAlt,
                        contentDescription = null,
                        tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
                Text(
                    text = "${device.ipAddress}:${device.port}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
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
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
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
            .background(MaterialTheme.colorScheme.secondary)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = "Location Permission Required",
                color = MaterialTheme.colorScheme.onSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Grant location permission to scan devices on your current Wi-Fi network.",
                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Grant Permission", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
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
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
            .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Text("Scan error", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onRetry) {
                Text("Try again", color = MaterialTheme.colorScheme.primary)
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
            .background(MaterialTheme.colorScheme.tertiary)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Connection hint",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f),
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
            .background(MaterialTheme.colorScheme.secondary)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Text(
            text = if (isScanning) {
                "Scanning IP range in your Wi-Fi subnet..."
            } else {
                "No TV found. Tap REFRESH or use manual IP connection."
            },
            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f),
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
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
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
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onClearLogs) {
                    Text("Clear Logs", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                }
            }
            Text(text = "Local IP: $ipText", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            Text(text = "Subnet: $subnetText", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            Text(
                text = "Permission: ${if (hasLocationPermission) "Granted" else "Missing"}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 11.sp,
            )
            Text(
                text = "Scan status: ${if (isScanning) "Running" else "Idle"}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 11.sp,
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "In-app Logs",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )

            if (logs.isEmpty()) {
                Text(
                    text = "No logs yet. Tap a TV or press remote buttons to capture events.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                )
            } else {
                logs.takeLast(12).forEach { line ->
                    Text(
                        text = line,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
fun ScreenMirroringCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.tertiary)
            .clickable { onClick() }
            .padding(24.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ScreenShare, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Screen Mirroring", color = MaterialTheme.colorScheme.onTertiary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Full device display broadcast", color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, title: String, bgColor: Color, iconColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.tertiary)
            .clickable { onClick() }
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
            Text(title, color = MaterialTheme.colorScheme.onTertiary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.StayCurrentPortrait, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "COMMAND",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = 1.sp
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            CastRouteChooserButton()
            Spacer(modifier = Modifier.width(20.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@Composable
private fun CastRouteChooserButton() {
    val context = LocalContext.current

    AndroidView(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
            .padding(4.dp),
        factory = { viewContext ->
            runCatching {
                MediaRouteButton(viewContext).apply {
                    runCatching {
                        CastButtonFactory.setUpMediaRouteButton(viewContext, this)
                    }.onFailure { error ->
                        InAppDiagnostics.warn("CastScreen", "Cast route setup failed: ${error.message}")
                        setOnClickListener { openCastSettings(viewContext) }
                    }
                    contentDescription = "Connect Cast Route"
                }
            }.getOrElse { error ->
                InAppDiagnostics.warn("CastScreen", "MediaRouteButton unavailable: ${error.message}")
                android.widget.ImageButton(viewContext).apply {
                    setImageResource(android.R.drawable.ic_menu_share)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setOnClickListener { openCastSettings(viewContext) }
                    contentDescription = "Open Cast Settings"
                }
            }
        },
        update = { button ->
            if (button is MediaRouteButton) {
                runCatching {
                    CastButtonFactory.setUpMediaRouteButton(context, button)
                }.onFailure { error ->
                    InAppDiagnostics.warn("CastScreen", "Cast route update failed: ${error.message}")
                    button.setOnClickListener { openCastSettings(context) }
                }
            } else {
                button.setOnClickListener { openCastSettings(context) }
            }
        },
    )
}

private fun openCastSettings(context: Context) {
    val candidates = listOf(
        Intent(Settings.ACTION_CAST_SETTINGS),
        Intent("android.settings.CAST_SETTINGS"),
        Intent("android.settings.WIFI_DISPLAY_SETTINGS"),
        Intent(Settings.ACTION_SETTINGS),
    )

    for (intent in candidates) {
        val launchIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val canHandle = launchIntent.resolveActivity(context.packageManager) != null
        if (canHandle) {
            runCatching {
                context.startActivity(launchIntent)
                return
            }
        }
    }

    InAppDiagnostics.warn("CastScreen", "No system activity available for Cast settings")
}
