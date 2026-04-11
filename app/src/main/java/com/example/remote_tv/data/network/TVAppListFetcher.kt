package com.example.remote_tv.data.network

import android.util.Log
import com.example.remote_tv.data.model.TVApp
import com.example.remote_tv.data.model.TVBrand
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Lấy danh sách ứng dụng đang được cài đặt trên TV.
 *
 * Hỗ trợ:
 * - Samsung Tizen TV: REST API `GET http://ip:8001/api/v2/applications`
 * - Android TV: ADB shell `pm list packages -3` qua TCP port 5555
 */
class TVAppListFetcher(private val client: HttpClient) {

    private val TAG = "TVAppListFetcher"
    private val json = Json { ignoreUnknownKeys = true }

    /** Ứng dụng phổ biến fallback khi TV không trả về danh sách */
    private val popularApps = listOf(
        TVApp("com.netflix.ninja", "Netflix"),
        TVApp("com.google.android.youtube.tv", "YouTube"),
        TVApp("com.disney.disneyplus", "Disney+"),
        TVApp("com.amazon.amazonvideo.livingroom", "Prime Video"),
        TVApp("com.spotify.tv.android", "Spotify"),
        TVApp("com.hbo.hbonow", "HBO Max"),
        TVApp("com.apple.atve.androidtv.appletv", "Apple TV"),
    )

    suspend fun fetchApps(ip: String, port: Int, brand: TVBrand): List<TVApp> {
        return when (brand) {
            TVBrand.SAMSUNG -> fetchSamsungApps(ip, port)
            TVBrand.ANDROID_TV -> fetchAndroidTVAppsViaAdb(ip)
            TVBrand.LG -> fetchLgApps(ip)
            else -> popularApps
        }
    }

    // ----------------------------------------------------------------
    // Samsung: REST API
    // ----------------------------------------------------------------

    private suspend fun fetchSamsungApps(ip: String, port: Int): List<TVApp> {
        val endpoints = buildList {
            if (port == 8001 || port == 8009) add("http://$ip:8001/api/v2/applications")
            if (port == 8002 || port == 8009) add("https://$ip:8002/api/v2/applications")
            add("http://$ip:8001/api/v2/applications")
        }.distinct()

        for (url in endpoints) {
            try {
                val response = client.get(url)
                if (response.status.isSuccess()) {
                    val body = response.bodyAsText()
                    val parsed = parseSamsungAppList(body)
                    if (parsed.isNotEmpty()) {
                        Log.d(TAG, "Samsung apps fetched: ${parsed.size} from $url")
                        return parsed
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Samsung app list failed at $url: ${e.message}")
            }
        }

        Log.w(TAG, "Samsung app list unavailable — returning popular apps")
        return popularAppsForSamsung()
    }

    private fun parseSamsungAppList(body: String): List<TVApp> {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            val data = root["data"]?.jsonArray ?: return emptyList()
            data.mapNotNull { element ->
                val obj = element.jsonObject
                val appId = obj["appId"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val name = obj["name"]?.jsonPrimitive?.content ?: appId
                val iconUri = obj["iconUri"]?.jsonPrimitive?.content
                TVApp(id = appId, name = name, iconUrl = iconUri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse Samsung apps failed: ${e.message}")
            emptyList()
        }
    }

    private fun popularAppsForSamsung() = listOf(
        TVApp("11101200001", "Netflix"),
        TVApp("111299001912", "YouTube"),
        TVApp("3201901017640", "Disney+"),
        TVApp("3201512006785", "Prime Video"),
        TVApp("3201606009684", "Spotify"),
    )

    // ----------------------------------------------------------------
    // Android TV: ADB pm list packages
    // ----------------------------------------------------------------

    private suspend fun fetchAndroidTVAppsViaAdb(ip: String): List<TVApp> = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, 5555), 3000)
            val out = socket.getOutputStream()
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))

            // Gửi lệnh pm list packages -3 (chỉ third-party apps)
            out.write("pm list packages -3\n".toByteArray())
            out.flush()

            Thread.sleep(1500)

            val result = StringBuilder()
            while (input.ready()) {
                result.appendLine(input.readLine())
            }
            socket.close()

            val packages = result.lines()
                .filter { it.startsWith("package:") }
                .map { it.removePrefix("package:").trim() }
                .filter { it.isNotBlank() }

            if (packages.isEmpty()) return@withContext popularApps

            Log.d(TAG, "ADB packages found: ${packages.size}")
            packages.map { pkg ->
                TVApp(id = pkg, name = friendlyAppName(pkg))
            }
        } catch (e: Exception) {
            Log.w(TAG, "ADB app list failed: ${e.message}")
            popularApps
        }
    }

    // ----------------------------------------------------------------
    // LG: Basic REST (placeholder)
    // ----------------------------------------------------------------

    private suspend fun fetchLgApps(ip: String): List<TVApp> {
        // LG WebOS app list via SSAP: ssap://com.webos.applicationManager/listApps
        // Cần WebSocket session đang hoạt động — placeholder dùng popular apps
        return listOf(
            TVApp("netflix", "Netflix"),
            TVApp("youtube.leanback.v4", "YouTube"),
            TVApp("com.disney.disneyplus-prod", "Disney+"),
            TVApp("amazon", "Prime Video"),
            TVApp("spotify-beehive", "Spotify"),
        )
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /** Tên thân thiện cho package phổ biến */
    private fun friendlyAppName(packageName: String): String {
        return when {
            "netflix" in packageName -> "Netflix"
            "youtube" in packageName -> "YouTube"
            "disney" in packageName -> "Disney+"
            "amazon" in packageName || "livingroom" in packageName -> "Prime Video"
            "spotify" in packageName -> "Spotify"
            "hbo" in packageName -> "HBO Max"
            "appletv" in packageName || "apple.atve" in packageName -> "Apple TV"
            "twitch" in packageName -> "Twitch"
            "plex" in packageName -> "Plex"
            "kodi" in packageName -> "Kodi"
            "vlc" in packageName -> "VLC"
            else -> packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
    }
}
