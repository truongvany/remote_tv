package com.example.remote_tv.data.model

/**
 * Thông tin TV cuối cùng được kết nối, lưu vào disk để auto-reconnect khi mở lại app.
 */
data class SavedDevice(
    val ip: String,
    val port: Int,
    val brand: TVBrand,
    val name: String,
    val macAddress: String? = null,
)

fun SavedDevice.toTVDevice(): TVDevice = TVDevice(
    id = "saved-$ip",
    name = name,
    ipAddress = ip,
    port = port,
    brand = brand,
    macAddress = macAddress,
)

fun TVDevice.toSavedDevice(): SavedDevice = SavedDevice(
    ip = ipAddress,
    port = port,
    brand = brand,
    name = name,
    macAddress = macAddress,
)
