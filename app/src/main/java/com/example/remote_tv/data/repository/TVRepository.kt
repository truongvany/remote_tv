package com.example.remote_tv.data.repository

import com.example.remote_tv.data.model.TVDevice
import kotlinx.coroutines.flow.StateFlow

interface TVRepository {
    val discoveredDevices: StateFlow<List<TVDevice>>
    val currentDevice: StateFlow<TVDevice?>
    val isScanning: StateFlow<Boolean>
    val scanError: StateFlow<String?>
    val connectionError: StateFlow<String?>

    fun startDiscovery()
    fun stopDiscovery()
    fun connectToDevice(device: TVDevice)
    fun disconnect()
    fun scheduleReconnect()
    suspend fun sendCommand(command: String): Boolean
    suspend fun launchApp(appId: String): Boolean
}

