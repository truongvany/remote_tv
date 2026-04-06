package com.example.remote_tv.data.repository

import com.example.remote_tv.data.model.AppLaunchResult
import com.example.remote_tv.data.model.PlaybackState
import com.example.remote_tv.data.model.TVDevice
import kotlinx.coroutines.flow.StateFlow

interface TVRepository {
    val discoveredDevices: StateFlow<List<TVDevice>>
    val currentDevice: StateFlow<TVDevice?>
    val connectingDeviceKey: StateFlow<String?>
    val isScanning: StateFlow<Boolean>
    val scanError: StateFlow<String?>
    val connectionError: StateFlow<String?>
    val playbackState: StateFlow<PlaybackState>
    val diagnosticLogs: StateFlow<List<String>>

    fun startDiscovery()
    fun stopDiscovery()
    fun connectToDevice(device: TVDevice)
    fun clearDiagnosticLogs()
    fun disconnect()
    fun scheduleReconnect()
    suspend fun sendCommand(command: String): Boolean
    suspend fun launchApp(appId: String): AppLaunchResult
}

