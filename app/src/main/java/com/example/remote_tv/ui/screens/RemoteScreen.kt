package com.example.remote_tv.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.widget.Toast
import com.example.remote_tv.data.debug.InAppDiagnostics
import com.example.remote_tv.data.model.AppThemeMode
import com.example.remote_tv.data.model.TVBrand
import com.example.remote_tv.data.model.TVDevice
import com.example.remote_tv.ui.components.DeviceSelectionDialog
import com.example.remote_tv.ui.viewmodel.TVViewModel
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

private data class NavTab(val icon: ImageVector, val contentDescription: String)

private val navTabs = listOf(
    NavTab(Icons.Filled.SettingsRemote, "Remote"),
    NavTab(Icons.Filled.ViewModule, "Channels"),
    NavTab(Icons.Filled.Cast, "Cast"),
    NavTab(Icons.Filled.PlayCircle, "Macro"),
    NavTab(Icons.Filled.Settings, "Settings"),
)

@Composable
fun RemoteScreen(viewModel: TVViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val currentDevice by viewModel.currentDevice.collectAsState()
    val connectingDeviceKey by viewModel.connectingDeviceKey.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanError by viewModel.scanError.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()
    val diagnosticLogs by viewModel.diagnosticLogs.collectAsState()
    val settingsUiState by viewModel.settingsUiState.collectAsState()
    val isCasting by viewModel.isCasting.collectAsState()
    val castStatus by viewModel.castStatus.collectAsState()
    
    val context = LocalContext.current

    var lastConnectedKey by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { result ->
            val allGranted = result.values.all { it }
            viewModel.onLocationPermissionResult(allGranted)
        },
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let(viewModel::castImageFromUri)
        },
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let(viewModel::castVideoFromUri)
        },
    )

    LaunchedEffect(uiState.selectedTab, uiState.hasLocationPermission) {
        if (uiState.selectedTab == 2) {
            if (uiState.hasLocationPermission) {
                viewModel.onCastTabOpened()
            } else {
                locationPermissionLauncher.launch(discoveryPermissions())
            }
        }
    }

    LaunchedEffect(currentDevice?.ipAddress, currentDevice?.port, uiState.selectedTab) {
        val currentKey = currentDevice?.let { "${it.ipAddress}:${it.port}" }
        if (currentKey != null && currentKey != lastConnectedKey && uiState.selectedTab == 2) {
            delay(1000)
            viewModel.selectTab(0)
        }
        lastConnectedKey = currentKey
    }

    LaunchedEffect(uiState.actionMessage) {
        val message = uiState.actionMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.consumeActionMessage()
    }

    if (uiState.showDeviceDialog) {
        DeviceSelectionDialog(
            devices = discoveredDevices,
            onDismiss = { viewModel.hideDeviceDialog() },
            onDeviceSelected = { viewModel.connectToDevice(it) },
            onManualConnect = { ip, port ->
                viewModel.connectToDevice(
                    TVDevice("manual", "Manual TV", ip, port, TVBrand.ANDROID_TV)
                )
            }
        )
    }

    Scaffold(
        bottomBar = {
            CustomBottomNavigation(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                0 -> MainRemoteTab(viewModel)
                1 -> ChannelsScreen(
                    isTvConnected = currentDevice != null,
                    launchedAppId = uiState.launchedAppId,
                    onLaunchApp = { packageName ->
                        viewModel.launchApp(packageName)
                    }
                )
                2 -> CastScreen(
                    devices = discoveredDevices,
                    connectedDevice = currentDevice,
                    connectingDeviceKey = connectingDeviceKey,
                    isScanning = isScanning,
                    scanError = scanError,
                    connectionError = connectionError,
                    hasLocationPermission = uiState.hasLocationPermission,
                    localIpAddress = uiState.localIpAddress,
                    localSubnet = uiState.localSubnet,
                    diagnosticLogs = diagnosticLogs,
                    isCastSessionActive = isCasting,
                    castStatus = castStatus,
                    onRequestPermission = {
                        locationPermissionLauncher.launch(discoveryPermissions())
                    },
                    onRefreshScan = {
                        if (uiState.hasLocationPermission) {
                            viewModel.refreshCastScan()
                        } else {
                            locationPermissionLauncher.launch(discoveryPermissions())
                        }
                    },
                    onDeviceSelected = viewModel::connectToDevice,
                    onPairAndConnect = viewModel::pairAndConnectToDevice,
                    onClearDiagnostics = viewModel::clearDiagnosticLogs,
                    onScreenMirroringClick = {
                        val opened = openScreenMirroringSettings(context)

                        if (opened) {
                            val target = currentDevice?.name
                            val message = if (target != null) {
                                "Select $target, then tap Start now to mirror this phone"
                            } else {
                                "Select your TV, then tap Start now to mirror this phone"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Không mở được màn Cast trực tiếp. Hãy mở Quick Settings > Cast/Smart View để truyền màn hình.",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                    onPickImage = { imagePickerLauncher.launch("image/*") },
                    onPickVideo = { videoPickerLauncher.launch("video/*") },
                    onStopCasting = viewModel::stopCasting,
                )
                3 -> MacroScreen(viewModel)
                4 -> SettingsScreen(
                    settingsUiState = settingsUiState,
                    onThemeChanged = { isDarkMode ->
                        viewModel.setThemeMode(
                            if (isDarkMode) AppThemeMode.DARK else AppThemeMode.LIGHT
                        )
                    },
                    onLanguageChanged = viewModel::setLanguage,
                    onAutoReconnectChanged = viewModel::setAutoReconnectLastDevice,
                    onAutoScanCastChanged = viewModel::setAutoScanOnCastTab,
                    onKeepScreenOnChanged = viewModel::setKeepScreenOn,
                    onForgetLastDevice = viewModel::forgetLastConnectedDevice,
                    onClearDiagnostics = viewModel::clearDiagnosticLogs,
                    onProfileSave = viewModel::updateUserProfile,
                    onDismissProfileError = viewModel::clearProfileError
                )
                else -> ComingSoonScreen()
            }
        }
    }
}

private fun discoveryPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

private fun openScreenMirroringSettings(context: android.content.Context): Boolean {
    val candidates = listOf(
        Intent("android.settings.panel.action.CAST"),
        Intent(android.provider.Settings.ACTION_CAST_SETTINGS),
        Intent("android.settings.CAST_SETTINGS"),
        Intent("android.settings.WIFI_DISPLAY_SETTINGS"),
        Intent("com.samsung.android.smartmirroring.CAST_SETTINGS"),
        Intent("com.android.settings.WIFI_DISPLAY_SETTINGS"),
    )

    candidates.forEach { intent ->
        val launchIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val canHandle = launchIntent.resolveActivity(context.packageManager) != null
        if (!canHandle) {
            return@forEach
        }

        val opened = runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrElse { false }

        if (opened) {
            InAppDiagnostics.info("RemoteScreen", "Opened mirroring settings via action=${intent.action}")
            return true
        }
    }

    InAppDiagnostics.warn("RemoteScreen", "No mirroring settings activity available")
    return false
}

@Composable
fun CustomBottomNavigation(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                MaterialTheme.colorScheme.surface, 
                RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .border(
                1.dp, 
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), 
                RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navTabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index

                Column(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.contentDescription,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ComingSoonScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Coming Soon", color = MaterialTheme.colorScheme.onBackground)
    }
}
