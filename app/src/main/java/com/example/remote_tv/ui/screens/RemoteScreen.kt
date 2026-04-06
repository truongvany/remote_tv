package com.example.remote_tv.ui.screens

import android.Manifest
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.remote_tv.data.model.AppThemeMode
import com.example.remote_tv.data.model.TVBrand
import com.example.remote_tv.data.model.TVDevice
import com.example.remote_tv.ui.components.DeviceSelectionDialog
import com.example.remote_tv.ui.theme.OrangeAccent
import com.example.remote_tv.ui.viewmodel.TVViewModel
import kotlinx.coroutines.delay

private data class NavTab(val icon: ImageVector, val contentDescription: String)

private val navTabs = listOf(
    NavTab(Icons.Filled.Home, "Home"),
    NavTab(Icons.Filled.Search, "Channels"),
    NavTab(Icons.Filled.Apps, "Cast"),
    NavTab(Icons.Filled.Person, "Settings"),
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

    var lastConnectedKey by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onLocationPermissionResult,
    )

    LaunchedEffect(uiState.selectedTab, uiState.hasLocationPermission) {
        if (uiState.selectedTab == 2) {
            if (uiState.hasLocationPermission) {
                viewModel.onCastTabOpened()
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
                1 -> ChannelsScreen()
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
                    onRequestPermission = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    onRefreshScan = {
                        if (uiState.hasLocationPermission) {
                            viewModel.refreshCastScan()
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    onDeviceSelected = viewModel::connectToDevice,
                    onClearDiagnostics = viewModel::clearDiagnosticLogs,
                )
                3 -> SettingsScreen(
                    settingsUiState = settingsUiState,
                    onThemeChanged = { isDarkMode ->
                        viewModel.setThemeMode(
                            if (isDarkMode) AppThemeMode.DARK else AppThemeMode.LIGHT
                        )
                    },
                    onLanguageChanged = viewModel::setLanguage,
                    onProfileSave = viewModel::updateUserProfile,
                    onDismissProfileError = viewModel::clearProfileError
                )
                else -> ComingSoonScreen()
            }
        }
    }
}

@Composable
fun CustomBottomNavigation(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(Color(0xFF000000), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .border(1.dp, Color(0xFF191919), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .padding(horizontal = 18.dp, vertical = 8.dp),
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
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = tab.contentDescription,
                        tint = if (isSelected) OrangeAccent else Color(0xFFADAAAA),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(OrangeAccent, CircleShape)
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
        Text("Coming Soon", color = Color.White)
    }
}
