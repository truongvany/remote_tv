package com.example.remote_tv.data.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.remote_tv.data.model.TVBrand
import com.example.remote_tv.data.model.TVDevice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class TVDiscoveryService(context: Context) {

    private val TAG = "TVDiscovery"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val subnetScanner = LocalSubnetScanner(context)
    private val discoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val defaultScanPorts = listOf(6466, 6467, 5555, 8008, 8009, 8002, 8060, 3000, 7236)
    private val deviceLock = Any()

    private val _discoveredDevices = MutableStateFlow<List<TVDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TVDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private var castListener: NsdManager.DiscoveryListener? = null
    private var remoteListener: NsdManager.DiscoveryListener? = null
    private var samsungListener: NsdManager.DiscoveryListener? = null
    private var rokuListener: NsdManager.DiscoveryListener? = null
    private var deepScanJob: Job? = null

    fun startDiscovery() {
        stopDiscovery()
        _discoveredDevices.value = emptyList()
        _scanError.value = null
        _isScanning.value = true

        castListener = buildListener("_googlecast._tcp")
        remoteListener = buildListener("_androidtvremote2._tcp")
        samsungListener = buildListener("_samsungmsf._tcp")
        rokuListener = buildListener("_roku-ecp._tcp")

        try {
            nsdManager.discoverServices("_googlecast._tcp", NsdManager.PROTOCOL_DNS_SD, castListener)
        } catch (e: Exception) {
            Log.e(TAG, "Cast discovery error: ${e.message}")
        }
        try {
            nsdManager.discoverServices("_androidtvremote2._tcp", NsdManager.PROTOCOL_DNS_SD, remoteListener)
        } catch (e: Exception) {
            Log.e(TAG, "AndroidTVRemote discovery error: ${e.message}")
        }
        try {
            nsdManager.discoverServices("_samsungmsf._tcp", NsdManager.PROTOCOL_DNS_SD, samsungListener)
        } catch (e: Exception) {
            Log.e(TAG, "Samsung discovery error: ${e.message}")
        }
        try {
            nsdManager.discoverServices("_roku-ecp._tcp", NsdManager.PROTOCOL_DNS_SD, rokuListener)
        } catch (e: Exception) {
            Log.e(TAG, "Roku discovery error: ${e.message}")
        }

        deepScanJob = discoveryScope.launch {
            try {
                val scannedDevices = subnetScanner.scan(defaultScanPorts)
                scannedDevices.forEach { updateDeviceList(it) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                val message = "Subnet scan failed: ${e.message ?: "unknown"}"
                Log.e(TAG, message, e)
                _scanError.value = message
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun stopDiscovery() {
        deepScanJob?.cancel()
        deepScanJob = null
        castListener?.let { try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {} }
        remoteListener?.let { try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {} }
        samsungListener?.let { try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {} }
        rokuListener?.let { try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {} }
        castListener = null
        remoteListener = null
        samsungListener = null
        rokuListener = null
        _isScanning.value = false
    }

    private fun buildListener(@Suppress("UNUSED_PARAMETER") serviceType: String): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Discovery started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "Resolve failed: $errorCode")
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val host = serviceInfo.host?.hostAddress ?: return
                        val name = serviceInfo.serviceName
                        val brand = when {
                            serviceInfo.serviceType.contains("androidtvremote2", true) -> TVBrand.ANDROID_TV
                            serviceInfo.serviceType.contains("samsungmsf", true) -> TVBrand.SAMSUNG
                            serviceInfo.serviceType.contains("roku-ecp", true) -> TVBrand.ROKU
                            name.contains("Samsung", true) -> TVBrand.SAMSUNG
                            name.contains("LG", true) -> TVBrand.LG
                            serviceInfo.serviceType.contains("googlecast", true) -> TVBrand.ANDROID_TV
                            else -> TVBrand.UNKNOWN
                        }
                        val device = TVDevice(
                            id = serviceInfo.serviceName,
                            name = name,
                            ipAddress = host,
                            port = serviceInfo.port,
                            brand = brand
                        )
                        Log.d(TAG, "Resolved: $name [$brand] at $host:${serviceInfo.port}")
                        updateDeviceList(device)
                    }
                })
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.w(TAG, "Service lost: ${service.serviceName}")
                _discoveredDevices.value = _discoveredDevices.value.filter { it.id != service.serviceName }
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _scanError.value = "Discovery failed for $serviceType (error $errorCode)"
                try { nsdManager.stopServiceDiscovery(this) } catch (_: Exception) {}
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                try { nsdManager.stopServiceDiscovery(this) } catch (_: Exception) {}
            }
        }
    }

    private fun updateDeviceList(device: TVDevice) {
        synchronized(deviceLock) {
            val list = _discoveredDevices.value.toMutableList()
            val index = list.indexOfFirst { it.ipAddress == device.ipAddress }

            if (index != -1) {
                list[index] = mergeDevice(existing = list[index], incoming = device)
            } else {
                list.add(device)
            }

            _discoveredDevices.value = list
        }
    }

    private fun mergeDevice(existing: TVDevice, incoming: TVDevice): TVDevice {
        val preferred = if (deviceQuality(incoming) >= deviceQuality(existing)) incoming else existing
        return existing.copy(
            id = preferred.id,
            name = preferred.name,
            port = preferred.port,
            brand = preferred.brand,
        )
    }

    private fun deviceQuality(device: TVDevice): Int {
        var score = 0

        if (!device.id.startsWith("scan-")) {
            score += 3
        }

        if (device.brand != TVBrand.UNKNOWN) {
            score += 2
        }

        val hasGenericName =
            device.name.startsWith("TV Device", ignoreCase = true) ||
                device.name.startsWith("Android TV (", ignoreCase = true)
        if (!hasGenericName) {
            score += 1
        }

        return score
    }
}

