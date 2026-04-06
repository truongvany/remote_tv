package com.example.remote_tv.ui.viewmodel

data class RemoteUiState(
    val selectedTab: Int = 0,
    val selectedMode: Int = 0,    // 0=DPad, 1=Touchpad, 2=Keyboard
    val showDeviceDialog: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val localIpAddress: String? = null,
    val localSubnet: String? = null,
)

