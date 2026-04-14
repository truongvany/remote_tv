package com.example.remote_tv.data.model

data class TVDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val brand: TVBrand = TVBrand.UNKNOWN,
    val isConnected: Boolean = false,
    val macAddress: String? = null,
    val modelName: String? = null,
    val pairPort: Int? = null,
)

fun TVDevice.isCastOnlyEndpoint(): Boolean {
    if (port != 8009) {
        return false
    }

    // Samsung can use 8009 for remote APIs, so keep that path connectable.
    if (brand == TVBrand.SAMSUNG) {
        return false
    }

    return brand == TVBrand.ANDROID_TV || brand == TVBrand.UNKNOWN
}

enum class TVBrand {
    SAMSUNG, LG, ANDROID_TV, ROKU, FIRE_TV, UNKNOWN
}
