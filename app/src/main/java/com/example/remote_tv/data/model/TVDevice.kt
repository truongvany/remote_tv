package com.example.remote_tv.data.model

data class TVDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val brand: TVBrand = TVBrand.UNKNOWN,
    val isConnected: Boolean = false
)

enum class TVBrand {
    SAMSUNG, LG, ANDROID_TV, ROKU, FIRE_TV, UNKNOWN
}
