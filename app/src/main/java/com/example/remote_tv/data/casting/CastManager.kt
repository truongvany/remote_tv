package com.example.remote_tv.data.casting

import android.content.Context
import android.util.Log
import com.example.remote_tv.data.model.TVDevice
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

    private var castContext: CastContext? = null
    private var currentSession: CastSession? = null

    init {
        try {
            castContext = CastContext.getSharedInstance(context)
        } catch (e: Exception) {
            Log.w(TAG, "Cast SDK not initialized: ${e.message}")
        }
    }

    /**
     * Truyền một URL video tới TV.
     */
    fun castVideo(url: String, title: String) {
        val session = castContext?.sessionManager?.currentCastSession
        if (session == null) {
            Log.e(TAG, "No active cast session.")
            return
        }

        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        movieMetadata.putString(MediaMetadata.KEY_TITLE, title)

        val mediaInfo = MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("video/mp4")
            .setMetadata(movieMetadata)
            .build()

        val remoteMediaClient = session.remoteMediaClient
        remoteMediaClient?.load(MediaLoadRequestData.Builder().setMediaInfo(mediaInfo).build())
        
        _isCasting.value = true
        Log.d(TAG, "Casting video: $url")
    }

    fun stopCasting() {
        castContext?.sessionManager?.endCurrentSession(true)
        _isCasting.value = false
    }
}
