package com.example.remote_tv.data.model

/**
 * Đại diện cho một ứng dụng được cài đặt trên TV.
 */
data class TVApp(
    val id: String,
    val name: String,
    val iconUrl: String? = null,
    val isRunning: Boolean = false,
)
