package com.example.remote_tv.data.repository

import android.content.Context
import com.example.remote_tv.data.AdbKeyManager
import com.example.remote_tv.data.connection.TVConnectionManager
import com.example.remote_tv.data.debug.InAppDiagnostics
import com.example.remote_tv.data.discovery.TVDiscoveryService
import com.example.remote_tv.data.model.TVDevice
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class TVRepositoryImpl(context: Context) : TVRepository {

    /**
     * TrustManager chấp nhận mọi certificate (self-signed) — dùng cho
     * kết nối WSS với Samsung TV trên mạng LAN nội bộ.
     * KHÔNG dùng cho các request internet công khai.
     */
    private val unsafeTrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    private val unsafeSSLContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf<TrustManager>(unsafeTrustManager), SecureRandom())
    }

    private val unsafeOkHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(unsafeSSLContext.socketFactory, unsafeTrustManager)
        .hostnameVerifier { _, _ -> true }
        .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val httpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = unsafeOkHttpClient
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 2_000
            requestTimeoutMillis = 5_000
            socketTimeoutMillis = 5_000
        }
        install(WebSockets)
        install(ContentNegotiation) { json() }
    }

    private val discoveryService = TVDiscoveryService(context)
    private val connectionManager = TVConnectionManager(httpClient)

    init {
        // Khởi tạo RSA keypair dùng cho ADB authentication
        AdbKeyManager.init(context)
    }

    override val discoveredDevices: StateFlow<List<TVDevice>> = discoveryService.discoveredDevices
    override val currentDevice: StateFlow<TVDevice?> = connectionManager.currentDevice
    override val connectingDeviceKey: StateFlow<String?> = connectionManager.connectingDeviceKey
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

