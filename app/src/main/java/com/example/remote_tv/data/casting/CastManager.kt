package com.example.remote_tv.data.casting

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.remote_tv.data.debug.InAppDiagnostics
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Quản lý việc truyền phát nội dung (Casting) tới thiết bị hỗ trợ Google Cast.
 */
class CastManager(private val context: Context) {
    private val TAG = "CastManager"

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val _castStatus = MutableStateFlow("Tap cast icon to connect")
    val castStatus: StateFlow<String> = _castStatus.asStateFlow()

    private val _castError = MutableStateFlow<String?>(null)
    val castError: StateFlow<String?> = _castError.asStateFlow()

    private var castContext: CastContext? = null
    private var currentSession: CastSession? = null
    private val localMediaServer = LocalMediaServer(context.applicationContext)

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            _castStatus.value = "Connecting cast session..."
            InAppDiagnostics.info(TAG, "Cast session starting")
        }

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            currentSession = session
            _isCasting.value = true
            _castStatus.value = "Cast connected: ${session.castDevice?.friendlyName ?: "TV"}"
            _castError.value = null
            InAppDiagnostics.info(TAG, "Cast session started id=$sessionId")
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            currentSession = null
            _isCasting.value = false
            _castStatus.value = "Cast connection failed"
            _castError.value = "Cast session start failed ($error)"
            InAppDiagnostics.error(TAG, "Cast session start failed code=$error")
        }

        override fun onSessionEnding(session: CastSession) {
            _castStatus.value = "Ending cast session..."
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            currentSession = null
            _isCasting.value = false
            _castStatus.value = "Tap cast icon to connect"
            if (error != 0) {
                _castError.value = "Cast session ended with code $error"
            }
            InAppDiagnostics.info(TAG, "Cast session ended code=$error")
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {
            _castStatus.value = "Resuming cast session..."
            InAppDiagnostics.info(TAG, "Cast session resuming id=$sessionId")
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            currentSession = session
            _isCasting.value = true
            _castStatus.value = "Cast connected: ${session.castDevice?.friendlyName ?: "TV"}"
            _castError.value = null
            InAppDiagnostics.info(TAG, "Cast session resumed suspended=$wasSuspended")
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            currentSession = null
            _isCasting.value = false
            _castStatus.value = "Cast resume failed"
            _castError.value = "Cast session resume failed ($error)"
            InAppDiagnostics.error(TAG, "Cast session resume failed code=$error")
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            _isCasting.value = false
            _castStatus.value = "Cast suspended"
            InAppDiagnostics.warn(TAG, "Cast session suspended reason=$reason")
        }
    }

    init {
        try {
            castContext = CastContext.getSharedInstance(context)
            castContext?.sessionManager?.addSessionManagerListener(sessionListener, CastSession::class.java)
            currentSession = castContext?.sessionManager?.currentCastSession
            if (currentSession != null) {
                _isCasting.value = true
                _castStatus.value = "Cast connected: ${currentSession?.castDevice?.friendlyName ?: "TV"}"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cast SDK not initialized: ${e.message}")
            InAppDiagnostics.warn(TAG, "Cast SDK init failed: ${e.message}")
        }
    }

    /**
     * Truyền một URL video tới TV.
     */
    fun castVideo(url: String, title: String): Boolean {
        return castMediaUrl(
            url = url,
            title = title,
            contentType = "video/mp4",
            metadataType = MediaMetadata.MEDIA_TYPE_MOVIE,
            streamType = MediaInfo.STREAM_TYPE_BUFFERED,
        )
    }

    fun castImageFromUri(uri: Uri, fallbackTitle: String = "Image"): Boolean {
        val endpoint = prepareImageEndpoint(uri, fallbackTitle) ?: run {
            _castError.value = "Cannot serve selected image over local network"
            return false
        }

        return castMediaUrl(
            url = endpoint.url,
            title = endpoint.title,
            contentType = endpoint.mimeType,
            metadataType = MediaMetadata.MEDIA_TYPE_PHOTO,
            streamType = MediaInfo.STREAM_TYPE_NONE,
        )
    }

    fun castVideoFromUri(uri: Uri, fallbackTitle: String = "Video"): Boolean {
        val endpoint = prepareVideoEndpoint(uri, fallbackTitle) ?: run {
            _castError.value = "Cannot serve selected video over local network"
            return false
        }

        val isVideo = endpoint.mimeType.startsWith("video/", ignoreCase = true)
        return castMediaUrl(
            url = endpoint.url,
            title = endpoint.title,
            contentType = endpoint.mimeType,
            metadataType = if (isVideo) MediaMetadata.MEDIA_TYPE_MOVIE else MediaMetadata.MEDIA_TYPE_PHOTO,
            streamType = if (isVideo) MediaInfo.STREAM_TYPE_BUFFERED else MediaInfo.STREAM_TYPE_BUFFERED,
        )
    }

    fun stopCasting() {
        castContext?.sessionManager?.endCurrentSession(true)
        _isCasting.value = false
        _castStatus.value = "Tap cast icon to connect"
    }

    fun prepareImageEndpoint(uri: Uri, fallbackTitle: String = "Image"): LocalMediaEndpoint? {
        return localMediaServer.registerMedia(
            uri = uri,
            fallbackMimeType = "image/jpeg",
            fallbackTitle = fallbackTitle,
        )
    }

    fun prepareVideoEndpoint(uri: Uri, fallbackTitle: String = "Video"): LocalMediaEndpoint? {
        return localMediaServer.registerMedia(
            uri = uri,
            fallbackMimeType = "video/mp4",
            fallbackTitle = fallbackTitle,
        )
    }

    fun release() {
        runCatching {
            castContext?.sessionManager?.removeSessionManagerListener(sessionListener, CastSession::class.java)
        }
        localMediaServer.clear()
    }

    private fun castMediaUrl(
        url: String,
        title: String,
        contentType: String,
        metadataType: Int,
        streamType: Int,
    ): Boolean {
        val session = castContext?.sessionManager?.currentCastSession ?: currentSession
        if (session == null) {
            _castError.value = "No active cast session. Tap cast icon to choose Google TV."
            _castStatus.value = "Cast session unavailable"
            InAppDiagnostics.warn(TAG, "Cast attempt without active session")
            return false
        }

        val metadata = MediaMetadata(metadataType)
        metadata.putString(MediaMetadata.KEY_TITLE, title)

        val mediaInfo = MediaInfo.Builder(url)
            .setStreamType(streamType)
            .setContentType(contentType)
            .setMetadata(metadata)
            .build()

        val remoteMediaClient = session.remoteMediaClient
        if (remoteMediaClient == null) {
            _castError.value = "Remote media client unavailable"
            InAppDiagnostics.warn(TAG, "Remote media client is null")
            return false
        }

        remoteMediaClient.load(MediaLoadRequestData.Builder().setMediaInfo(mediaInfo).build())

        _isCasting.value = true
        _castError.value = null
        _castStatus.value = "Casting: $title"
        Log.d(TAG, "Casting media: $url")
        InAppDiagnostics.info(TAG, "Casting media title=$title type=$contentType")
        return true
    }
}
