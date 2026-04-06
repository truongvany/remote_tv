package com.example.remote_tv.data.discovery

import android.os.Build
import android.content.Context
import android.net.ConnectivityManager
import com.example.remote_tv.data.model.TVBrand
import com.example.remote_tv.data.model.TVDevice
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class LocalSubnetScanner(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    suspend fun scan(
        candidatePorts: List<Int>,
        connectTimeoutMs: Int = 180,
        maxConcurrency: Int = 64,
    ): List<TVDevice> = coroutineScope {
        val localIp = resolveLocalIpv4Address() ?: return@coroutineScope emptyList()
        if (isEmulatorLan(localIp)) {
            throw IllegalStateException(
                "Running on emulator network ($localIp). Use a physical phone on the same Wi-Fi as your TV to scan real devices."
            )
        }
        val subnetBase = resolveSubnetBase(localIp) ?: return@coroutineScope emptyList()

        val discovered = ConcurrentHashMap<String, TVDevice>()
        val semaphore = Semaphore(maxConcurrency)

        (1..254).map { host ->
            async(Dispatchers.IO) {
                val ipAddress = "$subnetBase.$host"
                if (ipAddress == localIp) {
                    return@async
                }

                candidatePorts.forEach { port ->
                    semaphore.withPermit {
                        if (isTcpPortOpen(ipAddress, port, connectTimeoutMs)) {
                            val key = "$ipAddress:$port"
                            discovered.putIfAbsent(key, buildScannedDevice(ipAddress, port))
                        }
                    }
                }
            }
        }.awaitAll()

        discovered.values.sortedBy { it.name }
    }

    private fun resolveLocalIpv4Address(): String? {
        val network = connectivityManager.activeNetwork ?: return null
        val linkProperties = connectivityManager.getLinkProperties(network) ?: return null

        return linkProperties.linkAddresses
            .firstOrNull { linkAddress ->
                val address = linkAddress.address
                address is Inet4Address && !address.isLoopbackAddress
            }
            ?.address
            ?.hostAddress
    }

    private fun resolveSubnetBase(localIp: String): String? {
        val parts = localIp.split('.')
        if (parts.size != 4) {
            return null
        }
        return "${parts[0]}.${parts[1]}.${parts[2]}"
    }

    private fun isTcpPortOpen(ipAddress: String, port: Int, timeoutMs: Int): Boolean {
        return runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ipAddress, port), timeoutMs)
            }
            true
        }.getOrElse { false }
    }

    private fun buildScannedDevice(ipAddress: String, port: Int): TVDevice {
        val brand = when (port) {
            8002, 8009 -> TVBrand.SAMSUNG
            3000 -> TVBrand.LG
            8060 -> TVBrand.ROKU
            7236 -> TVBrand.FIRE_TV
            6466, 6467, 8008 -> TVBrand.ANDROID_TV
            5555 -> TVBrand.UNKNOWN
            else -> TVBrand.UNKNOWN
        }

        val label = when (brand) {
            TVBrand.SAMSUNG -> "Samsung TV"
            TVBrand.LG -> "LG TV"
            TVBrand.ROKU -> "Roku TV"
            TVBrand.FIRE_TV -> "Fire TV"
            TVBrand.ANDROID_TV -> "Android TV"
            TVBrand.UNKNOWN -> if (port == 5555) "ADB Device" else "TV Device"
        }

        return TVDevice(
            id = "scan-$ipAddress-$port",
            name = "$label ($ipAddress)",
            ipAddress = ipAddress,
            port = port,
            brand = brand,
        )
    }

    private fun isEmulatorLan(localIp: String): Boolean {
        val isEmulatorDevice =
            Build.FINGERPRINT.startsWith("generic") ||
                Build.MODEL.contains("Emulator", ignoreCase = true) ||
                Build.MANUFACTURER.contains("Genymotion", ignoreCase = true)

        return isEmulatorDevice && localIp.startsWith("10.0.2.")
    }
}
