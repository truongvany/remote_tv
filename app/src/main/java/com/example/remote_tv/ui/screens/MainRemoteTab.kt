package com.example.remote_tv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.remote_tv.ui.components.*
import com.example.remote_tv.ui.viewmodel.TVViewModel

@Composable
fun MainRemoteTab(viewModel: TVViewModel) {
    val currentDevice by viewModel.currentDevice.collectAsState()
    val deviceName = currentDevice?.name ?: "Living Room TV"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(
            deviceName = deviceName,
            onPower = { viewModel.powerToggle() },
            onCastClick = { viewModel.selectTab(2) }
        )

        NowPlayingCard()

        val uiState by viewModel.uiState.collectAsState()

        ModeSelector(
            selectedMode = uiState.selectedMode,
            onModeSelected = { viewModel.selectMode(it) }
        )

        Spacer(modifier = Modifier.height(28.dp))

        when (uiState.selectedMode) {
            0 -> DPad(
                onDirection = { viewModel.sendDirection(it) },
                onOk = { viewModel.sendOk() }
            )
            1 -> Touchpad(
                onSwipeUp = { viewModel.sendCommand("UP") },
                onSwipeDown = { viewModel.sendCommand("DOWN") },
                onSwipeLeft = { viewModel.sendCommand("LEFT") },
                onSwipeRight = { viewModel.sendCommand("RIGHT") },
                onTap = { viewModel.sendOk() }
            )
            2 -> KeyboardInput(
                onSendText = { text -> viewModel.sendCommand("TEXT:$text") }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        ControlButtons(onCommand = { viewModel.sendCommand(it) })

        Spacer(modifier = Modifier.height(28.dp))

        QuickLaunch(onLaunchApp = { viewModel.launchApp(it) })

        Spacer(modifier = Modifier.height(24.dp))
    }
}
