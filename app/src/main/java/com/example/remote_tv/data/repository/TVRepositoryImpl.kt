package com.example.remote_tv.data.repository

import android.content.Context
import com.example.remote_tv.data.connection.TVConnectionManager
import com.example.remote_tv.data.debug.InAppDiagnostics
import com.example.remote_tv.data.discovery.TVDiscoveryService
import com.example.remote_tv.data.model.TVDevice
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.StateFlow

class TVRepositoryImpl(context: Context) : TVRepository {

    private val httpClient = HttpClient {
        install(WebSockets)
        install(ContentNegotiation) { json() }
    }

    private val discoveryService = TVDiscoveryService(context)
    private val connectionManager = TVConnectionManager(httpClient)

    override val discoveredDevices: StateFlow<List<TVDevice>> = discoveryService.discoveredDevices
    override val currentDevice: StateFlow<TVDevice?> = connectionManager.currentDevice
    override val isScanning: StateFlow<Boolean> = discoveryService.isScanning
    override val scanError: StateFlow<String?> = discoveryService.scanError
    override val connectionError: StateFlow<String?> = connectionManager.connectionError
    override val diagnosticLogs: StateFlow<List<String>> = InAppDiagnostics.logs

    override fun startDiscovery() = discoveryService.startDiscovery()
    override fun stopDiscovery() = discoveryService.stopDiscovery()

    override fun connectToDevice(device: TVDevice) {
        connectionManager.connect(device)
    }

    override fun clearDiagnosticLogs() {
        InAppDiagnostics.clear()
    }

    override fun disconnect() {
        connectionManager.disconnect()
    }

    override fun scheduleReconnect() {
        connectionManager.scheduleReconnect()
    }

    override suspend fun sendCommand(command: String): Boolean {
        return connectionManager.sendCommand(command)
    }

    override suspend fun launchApp(appId: String): Boolean {
        return connectionManager.launchApp(appId)
    }
}

