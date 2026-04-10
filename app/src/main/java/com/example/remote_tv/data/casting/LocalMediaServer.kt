package com.example.remote_tv.data.casting

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.remote_tv.data.debug.InAppDiagnostics
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID

data class LocalMediaEndpoint(
    val url: String,
    val mimeType: String,
    val title: String,
    val remoteOpenUrl: String,
)

class LocalMediaServer(private val context: Context) {

    private val tag = "LocalMediaServer"
    private val servedMedia = LinkedHashMap<String, ServedMedia>()
    private var server: MediaHttpServer? = null

    @Synchronized
    fun registerMedia(uri: Uri, fallbackMimeType: String, fallbackTitle: String): LocalMediaEndpoint? {
        val media = copyToCache(uri, fallbackMimeType, fallbackTitle) ?: return null
        val httpServer = ensureServerStarted() ?: return null
        val localIp = resolveLocalIpv4Address()

        if (localIp == null) {
            InAppDiagnostics.error(tag, "No local IPv4 available for media endpoint")
            media.file.delete()
            return null
        }

        val token = UUID.randomUUID().toString().replace("-", "")
        servedMedia[token] = media
        trimMediaCache(maxEntries = 8)

        val mediaUrl = "http://$localIp:${httpServer.listeningPort}/media/$token"
        val remoteOpenUrl = if (media.mimeType.startsWith("image/", ignoreCase = true)) {
            "http://$localIp:${httpServer.listeningPort}/preview/$token"
        } else {
            mediaUrl
        }

        val endpoint = LocalMediaEndpoint(
            url = mediaUrl,
            mimeType = media.mimeType,
            title = media.title,
            remoteOpenUrl = remoteOpenUrl,
        )

        InAppDiagnostics.info(tag, "Media endpoint ready ${endpoint.title} ${endpoint.mimeType}")
        return endpoint
    }

    @Synchronized
    fun clear() {
        servedMedia.values.forEach { media ->
            runCatching { media.file.delete() }
        }
        servedMedia.clear()
        server?.stop()
        server = null
        InAppDiagnostics.info(tag, "Local media server cleared")
    }

    @Synchronized
    private fun ensureServerStarted(): MediaHttpServer? {
        server?.let { return it }

        return try {
            val created = MediaHttpServer(0)
            created.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            if (created.listeningPort <= 0) {
                InAppDiagnostics.error(tag, "Failed to acquire listening port")
                null
            } else {
                server = created
                InAppDiagnostics.info(tag, "Local media server started on port ${created.listeningPort}")
                created
            }
        } catch (e: Exception) {
            InAppDiagnostics.error(tag, "Failed to start media server: ${e.message}")
            null
        }
    }

    private fun copyToCache(uri: Uri, fallbackMimeType: String, fallbackTitle: String): ServedMedia? {
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }

        val resolvedMime = resolver.getType(uri)
            ?: inferMimeFromName(displayName)
            ?: fallbackMimeType

        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(resolvedMime)
            ?: displayName?.substringAfterLast('.', "")
            ?: "bin"

        val title = displayName?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: fallbackTitle

        val targetDir = File(context.cacheDir, "cast_media").apply { mkdirs() }
        val targetFile = File(targetDir, "cast_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension")

        return try {
            resolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                InAppDiagnostics.error(tag, "Cannot open input stream for uri=$uri")
                return null
            }

            if (targetFile.length() <= 0L) {
                InAppDiagnostics.error(tag, "Copied media is empty for uri=$uri")
                targetFile.delete()
                null
            } else {
                ServedMedia(file = targetFile, mimeType = resolvedMime, title = title)
            }
        } catch (e: Exception) {
            InAppDiagnostics.error(tag, "Failed to cache media: ${e.message}")
            runCatching { targetFile.delete() }
            null
        }
    }

    private fun inferMimeFromName(name: String?): String? {
        if (name.isNullOrBlank()) return null
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext.isBlank()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    private fun resolveLocalIpv4Address(): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wifiIp = cm?.let { resolveWifiIpv4Address(it) }
        if (wifiIp != null) {
            return wifiIp
        }

        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (!networkInterface.isUp || networkInterface.isLoopback) {
                continue
            }

            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (address is Inet4Address && !address.isLoopbackAddress && !address.isLinkLocalAddress) {
                    return address.hostAddress
                }
            }
        }
        return null
    }

    private fun resolveWifiIpv4Address(connectivityManager: ConnectivityManager): String? {
        val wifiNetwork = resolveWifiNetwork(connectivityManager) ?: return null
        val linkProperties = connectivityManager.getLinkProperties(wifiNetwork) ?: return null

        return linkProperties.linkAddresses
            .firstOrNull { linkAddress ->
                val address = linkAddress.address
                address is Inet4Address && !address.isLoopbackAddress
            }
            ?.address
            ?.hostAddress
    }

    private fun resolveWifiNetwork(connectivityManager: ConnectivityManager): Network? {
        @Suppress("DEPRECATION")
        return connectivityManager.allNetworks.firstOrNull { network ->
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@firstOrNull false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
    }

    @Synchronized
    private fun trimMediaCache(maxEntries: Int) {
        while (servedMedia.size > maxEntries) {
            val firstKey = servedMedia.keys.firstOrNull() ?: break
            val media = servedMedia.remove(firstKey)
            runCatching { media?.file?.delete() }
        }
    }

    private fun resolveMediaByToken(token: String): ServedMedia? {
        synchronized(this) {
            return servedMedia[token]
        }
    }

    private data class ServedMedia(
        val file: File,
        val mimeType: String,
        val title: String,
    )

    private inner class MediaHttpServer(port: Int) : NanoHTTPD(port) {

        override fun serve(session: IHTTPSession): Response {
            val previewToken = extractToken(session.uri, "/preview/")
            if (previewToken != null) {
                val previewMedia = resolveMediaByToken(previewToken)
                    ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Media expired")
                return previewHtml(previewToken, previewMedia)
            }

            val token = extractToken(session.uri, "/media/")
            if (token == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
            }

            val media = resolveMediaByToken(token)
                ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Media expired")

            val file = media.file
            if (!file.exists() || !file.isFile) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Media file missing")
            }

            val totalLength = file.length()
            val range = resolveRange(session.headers["range"], totalLength)

            return when (range) {
                is RangeResult.Unsatisfiable -> {
                    newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, "text/plain", "")
                        .apply {
                            addHeader("Content-Range", "bytes */$totalLength")
                            addHeader("Accept-Ranges", "bytes")
                        }
                }

                is RangeResult.Full -> {
                    if (session.method == Method.HEAD) {
                        newFixedLengthResponse(Response.Status.OK, media.mimeType, "")
                            .apply {
                                addHeader("Content-Length", totalLength.toString())
                                addHeader("Accept-Ranges", "bytes")
                                addHeader("Cache-Control", "no-cache")
                            }
                    } else {
                        val stream = FileInputStream(file)
                        newFixedLengthResponse(Response.Status.OK, media.mimeType, stream, totalLength)
                            .apply {
                                addHeader("Content-Length", totalLength.toString())
                                addHeader("Accept-Ranges", "bytes")
                                addHeader("Cache-Control", "no-cache")
                            }
                    }
                }

                is RangeResult.Partial -> {
                    val contentLength = range.end - range.start + 1
                    if (session.method == Method.HEAD) {
                        newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, media.mimeType, "")
                            .apply {
                                addHeader("Content-Length", contentLength.toString())
                                addHeader("Content-Range", "bytes ${range.start}-${range.end}/$totalLength")
                                addHeader("Accept-Ranges", "bytes")
                                addHeader("Cache-Control", "no-cache")
                            }
                    } else {
                        val stream = FileInputStream(file)
                        skipFully(stream, range.start)
                        newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, media.mimeType, stream, contentLength)
                            .apply {
                                addHeader("Content-Length", contentLength.toString())
                                addHeader("Content-Range", "bytes ${range.start}-${range.end}/$totalLength")
                                addHeader("Accept-Ranges", "bytes")
                                addHeader("Cache-Control", "no-cache")
                            }
                    }
                }
            }
        }

                private fun extractToken(path: String, prefix: String): String? {
                        if (!path.startsWith(prefix)) {
                                return null
                        }

                        val remainder = path.removePrefix(prefix).trim('/')
                        if (remainder.isBlank()) {
                                return null
                        }

                        return remainder.substringBefore('/')
                }

                private fun previewHtml(token: String, media: ServedMedia): Response {
                        val safeTitle = escapeHtml(media.title)
                        val html = """
                                <!doctype html>
                                <html>
                                <head>
                                    <meta charset="utf-8" />
                                    <meta name="viewport" content="width=device-width, initial-scale=1" />
                                    <title>$safeTitle</title>
                                    <style>
                                        html, body {
                                            margin: 0;
                                            padding: 0;
                                            width: 100%;
                                            height: 100%;
                                            background: #000;
                                            overflow: hidden;
                                        }
                                        img {
                                            width: 100%;
                                            height: 100%;
                                            object-fit: contain;
                                        }
                                    </style>
                                </head>
                                <body>
                                    <img src="/media/$token" alt="$safeTitle" />
                                </body>
                                </html>
                        """.trimIndent()

                        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
                                .apply {
                                        addHeader("Cache-Control", "no-cache")
                                }
                }

                private fun escapeHtml(value: String): String {
                        return value
                                .replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\"", "&quot;")
                                .replace("'", "&#39;")
                }

        private fun skipFully(stream: FileInputStream, bytes: Long) {
            var remaining = bytes
            while (remaining > 0) {
                val skipped = stream.skip(remaining)
                if (skipped <= 0) break
                remaining -= skipped
            }
        }

        private fun resolveRange(rangeHeader: String?, fileLength: Long): RangeResult {
            if (rangeHeader.isNullOrBlank() || !rangeHeader.startsWith("bytes=")) {
                return RangeResult.Full
            }

            val raw = rangeHeader.removePrefix("bytes=").trim()
            val parts = raw.split('-', limit = 2)
            if (parts.size != 2) {
                return RangeResult.Full
            }

            val startPart = parts[0].trim()
            val endPart = parts[1].trim()

            val result = when {
                startPart.isBlank() -> {
                    val suffixLength = endPart.toLongOrNull() ?: return RangeResult.Unsatisfiable
                    if (suffixLength <= 0) return RangeResult.Unsatisfiable
                    val start = (fileLength - suffixLength).coerceAtLeast(0)
                    RangeResult.Partial(start = start, end = fileLength - 1)
                }

                else -> {
                    val start = startPart.toLongOrNull() ?: return RangeResult.Unsatisfiable
                    val end = if (endPart.isBlank()) {
                        fileLength - 1
                    } else {
                        endPart.toLongOrNull() ?: return RangeResult.Unsatisfiable
                    }

                    if (start < 0 || start >= fileLength || end < start) {
                        return RangeResult.Unsatisfiable
                    }

                    RangeResult.Partial(start = start, end = end.coerceAtMost(fileLength - 1))
                }
            }

            return result
        }
    }

    private sealed class RangeResult {
        data object Full : RangeResult()
        data class Partial(val start: Long, val end: Long) : RangeResult()
        data object Unsatisfiable : RangeResult()
    }
}
