package com.example.remote_tv.data.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.remote_tv.data.debug.InAppDiagnostics
import com.example.remote_tv.data.model.TVBrand
import com.example.remote_tv.data.model.TVDevice
import com.example.remote_tv.data.model.isCastOnlyEndpoint
import java.net.Inet4Address
import java.net.InetAddress
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
    private val appContext = context.applicationContext
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val subnetScanner = LocalSubnetScanner(context)
    private val discoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Keep subnet scan focused on control-ready ports to reduce false positives.
    private val quickScanPorts = listOf(5555, 6466, 6467, 8001, 8002, 3000)
    private val defaultScanPorts = listOf(5555, 6466, 6467, 8001, 8002, 3000, 8008, 8009)
    private val deviceLock = Any()

    private val _discoveredDevices = MutableStateFlow<List<TVDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TVDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private var castListener: NsdManager.DiscoveryListener? = null
    private var remoteListener: NsdManager.DiscoveryListener? = null
    private var adbTlsPairingListener: NsdManager.DiscoveryListener? = null
    private var adbTlsConnectListener: NsdManager.DiscoveryListener? = null
    private var samsungListener: NsdManager.DiscoveryListener? = null
    private var rokuListener: NsdManager.DiscoveryListener? = null
    private var deepScanJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun startDiscovery() {
        stopDiscovery()
        acquireMulticastLockIfNeeded()
        _discoveredDevices.value = emptyList()
        _scanError.value = null
        _isScanning.value = true
        InAppDiagnostics.info(TAG, "Start discovery: NSD + subnet scan")

        castListener = buildListener("_googlecast._tcp")
        remoteListener = buildListener("_androidtvremote2._tcp")
        adbTlsPairingListener = buildListener("_adb-tls-pairing._tcp")
        adbTlsConnectListener = buildListener("_adb-tls-connect._tcp")
        samsungListener = buildListener("_samsungmsf._tcp")
        rokuListener = buildListener("_roku-ecp._tcp")

        try {
            nsdManager.discoverServices("_googlecast._tcp", NsdManager.PROTOCOL_DNS_SD, castListener)
        } catch (e: Exception) {
            Log.e(TAG, "Cast discovery error: ${e.message}")
            InAppDiagnostics.error(TAG, "Cast NSD error: ${e.message}")
        }
        try {
            nsdManager.discoverServices("_androidtvremote2._tcp", NsdManager.PROTOCOL_DNS_SD, remoteListener)
        } catch (e: Exception) {
            Log.e(TAG, "AndroidTVRemote discovery error: ${e.message}")
            InAppDiagnostics.error(TAG, "AndroidTV NSD error: ${e.message}")
        }
        try {
            nsdManager.discoverServices("_adb-tls-pairing._tcp", NsdManager.PROTOCOL_DNS_SD, adbTlsPairingListener)
        } catch (e: Exception) {
            Log.e(TAG, "ADB TLS pairing discovery error: ${e.message}")
            InAppDiagnostics.error(TAG, "ADB TLS pairing NSD error: ${e.message}")
        }
        try {
            nsdManager.discoverServices("_adb-tls-connect._tcp", NsdManager.PROTOCOL_DNS_SD, adbTlsConnectListener)
        } catch (e: Exception) {
            Log.e(TAG, "ADB TLS connect discovery error: ${e.message}")
            InAppDiagnostics.error(TAG, "ADB TLS NSD error: ${e.message}")
        }
        try {
            nsdManager.discoverServices("_samsungmsf._tcp", NsdManager.PROTOCOL_DNS_SD, samsungListener)
        } catch (e: Exception) {
            Log.e(TAG, "Samsung discovery error: ${e.message}")
            InAppDiagnostics.error(TAG, "Samsung NSD error: ${e.message}")
        }
        try {
            nsdManager.discoverServices("_roku-ecp._tcp", NsdManager.PROTOCOL_DNS_SD, rokuListener)
        } catch (e: Exception) {
            Log.e(TAG, "Roku discovery error: ${e.message}")
            InAppDiagnostics.error(TAG, "Roku NSD error: ${e.message}")
        }

        deepScanJob = discoveryScope.launch {
            try {
                // Stage 1: quick scan for Android TV / Google TV related ports.
                val quickDevices = subnetScanner.scan(
                    candidatePorts = quickScanPorts,
                    connectTimeoutMs = 120,
                    maxConcurrency = 96,
                )
                quickDevices.forEach { updateDeviceList(it) }
                InAppDiagnostics.info(TAG, "Quick subnet scan finished. devices=${quickDevices.size}")

                // Stage 2: broader scan for other brands and fallback services.
                val remainingPorts = (defaultScanPorts - quickScanPorts.toSet()).distinct()
                if (remainingPorts.isNotEmpty()) {
                    val scannedDevices = subnetScanner.scan(
                        candidatePorts = remainingPorts,
                        connectTimeoutMs = 180,
                        maxConcurrency = 64,
                    )
                    scannedDevices.forEach { updateDeviceList(it) }
                    InAppDiagnostics.info(TAG, "Full subnet scan finished. devices=${scannedDevices.size}")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                val message = "Subnet scan failed: ${e.message ?: "unknown"}"
                Log.e(TAG, message, e)
                _scanError.value = message
                InAppDiagnostics.error(TAG, message)
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
        adbTlsPairingListener?.let { try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {} }
        adbTlsConnectListener?.let { try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {} }
        samsungListener?.let { try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {} }
        rokuListener?.let { try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {} }
        castListener = null
        remoteListener = null
        adbTlsPairingListener = null
        adbTlsConnectListener = null
        samsungListener = null
        rokuListener = null
        _isScanning.value = false
        releaseMulticastLock()
        InAppDiagnostics.info(TAG, "Stop discovery")
    }

    private fun acquireMulticastLockIfNeeded() {
        if (multicastLock?.isHeld == true) {
            return
        }

        runCatching {
            val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val lock = wifiManager?.createMulticastLock("remote-tv-cast-discovery")
            lock?.setReferenceCounted(false)
            lock?.acquire()
            multicastLock = lock
            if (lock != null) {
                InAppDiagnostics.info(TAG, "Multicast lock acquired")
            } else {
                InAppDiagnostics.warn(TAG, "Multicast lock unavailable on this device")
            }
        }.onFailure { error ->
            InAppDiagnostics.warn(TAG, "Multicast lock acquire failed: ${error.message}")
        }
    }

    private fun releaseMulticastLock() {
        runCatching {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                InAppDiagnostics.info(TAG, "Multicast lock released")
            }
        }.onFailure { error ->
            InAppDiagnostics.warn(TAG, "Multicast lock release failed: ${error.message}")
        }
        multicastLock = null
    }

    private fun buildListener(serviceType: String): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Discovery started: $regType")
                InAppDiagnostics.info(TAG, "NSD started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "Resolve failed: $errorCode")
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val host = resolveServiceHostAddress(serviceInfo) ?: return
                        val name = serviceInfo.serviceName
                        val discoveredType = serviceInfo.serviceType.ifBlank { serviceType }
                        val isAdbPairingService = discoveredType.contains("adb-tls-pairing", true)
                        val brand = when {
                            discoveredType.contains("androidtvremote2", true) -> TVBrand.ANDROID_TV
                            isAdbPairingService -> TVBrand.ANDROID_TV
                            discoveredType.contains("adb-tls-connect", true) -> TVBrand.ANDROID_TV
                            discoveredType.contains("samsungmsf", true) -> TVBrand.SAMSUNG
                            discoveredType.contains("roku-ecp", true) -> TVBrand.ROKU
                            name.contains("Samsung", true) -> TVBrand.SAMSUNG
                            name.contains("LG", true) -> TVBrand.LG
                            discoveredType.contains("googlecast", true) -> TVBrand.ANDROID_TV
                            else -> TVBrand.UNKNOWN
                        }
                        val device = TVDevice(
                            id = serviceInfo.serviceName,
                            name = name,
                            ipAddress = host,
                            port = serviceInfo.port,
                            brand = brand,
                            pairPort = if (isAdbPairingService) serviceInfo.port else null,
                        )
                        Log.d(TAG, "Resolved: $name [$brand] at $host:${serviceInfo.port}")
                        InAppDiagnostics.info(TAG, "Resolved: $name [$brand] $host:${serviceInfo.port}")
                        updateDeviceList(device)
                    }
                })
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.w(TAG, "Service lost: ${service.serviceName}")
                InAppDiagnostics.warn(TAG, "Service lost: ${service.serviceName}")
                _discoveredDevices.value = _discoveredDevices.value.filter { it.id != service.serviceName }
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _scanError.value = "Discovery failed for $serviceType (error $errorCode)"
                InAppDiagnostics.error(TAG, "NSD failed: $serviceType error=$errorCode")
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
        val preferred = if (selectionScore(incoming) >= selectionScore(existing)) incoming else existing
        val secondary = if (preferred === incoming) existing else incoming

        // Prefer non-scan identifiers when quality is similar.
        val preferredId = when {
            !preferred.id.startsWith("scan-") -> preferred.id
            !secondary.id.startsWith("scan-") -> secondary.id
            else -> preferred.id
        }

        return existing.copy(
            id = preferredId,
            name = preferred.name,
            port = preferred.port,
            brand = preferred.brand,
            pairPort = preferred.pairPort ?: secondary.pairPort,
        )
    }

    private fun selectionScore(device: TVDevice): Int {
        return endpointPriority(device) * 10 + deviceQuality(device)
    }

    private fun endpointPriority(device: TVDevice): Int {
        if (device.isCastOnlyEndpoint()) {
            return 8
        }

        return when (device.port) {
            5555 -> 62 // Most control-ready for Android TV in current project.
            8002 -> 61
            8001 -> 60
            3000 -> 58
            6466, 6467 -> 52
            8008 -> 44
            8009 -> if (device.brand == TVBrand.SAMSUNG) 50 else 8
            else -> 20
        }
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

    private fun resolveServiceHostAddress(serviceInfo: NsdServiceInfo): String? {
        val rawAddress = serviceInfo.host?.hostAddress?.trim().orEmpty()
        val hostName = serviceInfo.host?.hostName?.trim().orEmpty()

        if (rawAddress.isNotBlank() && !isIpv6LinkLocal(rawAddress)) {
            return rawAddress
        }

        val ipv4FromHostName = resolveIpv4FromHostName(hostName)
        if (ipv4FromHostName != null) {
            InAppDiagnostics.info(
                TAG,
                "Resolved IPv4 fallback for ${serviceInfo.serviceName}: $ipv4FromHostName (raw=${rawAddress.ifBlank { "n/a" }})"
            )
            return ipv4FromHostName
        }

        return rawAddress.takeIf { it.isNotBlank() }
    }

    private fun resolveIpv4FromHostName(hostName: String): String? {
        if (hostName.isBlank()) return null

        return runCatching {
            InetAddress.getAllByName(hostName)
                .firstOrNull { address ->
                    address is Inet4Address && !address.isLoopbackAddress
                }
                ?.hostAddress
        }.getOrNull()
    }

    private fun isIpv6LinkLocal(address: String): Boolean {
        val normalized = address.substringBefore('%')
        return normalized.contains(':') && normalized.startsWith("fe80", ignoreCase = true)
    }
}

