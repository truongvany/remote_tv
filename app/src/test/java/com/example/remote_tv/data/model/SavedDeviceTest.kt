package com.example.remote_tv.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedDeviceTest {

    @Test
    fun testSavedDeviceToTVDeviceMapping() {
        val savedDevice = SavedDevice(
            ip = "192.168.1.5",
            port = 8001,
            brand = TVBrand.SAMSUNG,
            name = "Living Room TV",
            macAddress = "00:11:22:33:44:55"
        )

        val tvDevice = savedDevice.toTVDevice()

        assertEquals("saved-192.168.1.5", tvDevice.id)
        assertEquals("Living Room TV", tvDevice.name)
        assertEquals("192.168.1.5", tvDevice.ipAddress)
        assertEquals(8001, tvDevice.port)
        assertEquals(TVBrand.SAMSUNG, tvDevice.brand)
        assertEquals("00:11:22:33:44:55", tvDevice.macAddress)
    }

    @Test
    fun testTVDeviceToSavedDeviceMapping() {
        val tvDevice = TVDevice(
            id = "device-123",
            name = "Bedroom TV",
            ipAddress = "192.168.1.10",
            port = 8009,
            brand = TVBrand.ANDROID_TV,
            isConnected = true,
            macAddress = "AA:BB:CC:DD:EE:FF",
            modelName = "Android Gen 2"
        )

        val savedDevice = tvDevice.toSavedDevice()

        assertEquals("192.168.1.10", savedDevice.ip)
        assertEquals(8009, savedDevice.port)
        assertEquals(TVBrand.ANDROID_TV, savedDevice.brand)
        assertEquals("Bedroom TV", savedDevice.name)
        assertEquals("AA:BB:CC:DD:EE:FF", savedDevice.macAddress)
    }
}

