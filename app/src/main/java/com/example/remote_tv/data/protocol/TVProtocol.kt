package com.example.remote_tv.data.protocol

interface TVProtocol {
    suspend fun connect(ip: String, port: Int): Boolean
    suspend fun disconnect()
    suspend fun sendCommand(command: String): Boolean
    suspend fun launchApp(appId: String): Boolean
}
