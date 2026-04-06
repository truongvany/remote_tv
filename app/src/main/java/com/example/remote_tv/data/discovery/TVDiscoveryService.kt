package com.example.remote_tv.data.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.remote_tv.data.model.TVBrand
import com.example.remote_tv.data.model.TVDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("DEPRECATION")
class TVDiscoveryService(context: Context) {

    private val TAG = "TVDiscovery"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _discoveredDevices = MutableStateFlow<List<TVDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TVDevice>> = _discoveredDevices.asStateFlow()

    private var castListener: NsdManager.DiscoveryListener? = null
    private var remoteListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        stopDiscovery()
        _discoveredDevices.value = emptyList()

        castListener = buildListener("_googlecast._tcp")
        remoteListener = buildListener("_androidtvremote2._tcp")

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
    }

    fun stopDiscovery() {
        castListener?.let { try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {} }
        remoteListener?.let { try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {} }
        castListener = null
        remoteListener = null
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
                try { nsdManager.stopServiceDiscovery(this) } catch (_: Exception) {}
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                try { nsdManager.stopServiceDiscovery(this) } catch (_: Exception) {}
            }
        }
    }

    private fun updateDeviceList(device: TVDevice) {
        val list = _discoveredDevices.value.toMutableList()
        val index = list.indexOfFirst { it.ipAddress == device.ipAddress }
        if (index != -1) list[index] = device else list.add(device)
        _discoveredDevices.value = list
    }
}

