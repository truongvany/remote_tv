package com.example.remote_tv.data.connection

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionPolicyTest {

    @Test
    fun testBackoffMs_roundOne_returnsBase() {
        val expected = ConnectionPolicy.BACKOFF_BASE_MS
        val result = ConnectionPolicy.backoffMs(1)
        assertEquals(expected, result)
    }

    @Test
    fun testBackoffMs_roundTwo_returnsDoubleBase() {
        val expected = ConnectionPolicy.BACKOFF_BASE_MS * 2
        val result = ConnectionPolicy.backoffMs(2)
        assertEquals(expected, result)
    }

    @Test
    fun testBackoffMs_negativeRound_returnsBase() {
        val expected = ConnectionPolicy.BACKOFF_BASE_MS
        val result = ConnectionPolicy.backoffMs(-1)
        assertEquals(expected, result)
    }

    @Test
    fun testBackoffMs_zeroRound_returnsBase() {
        val expected = ConnectionPolicy.BACKOFF_BASE_MS
        val result = ConnectionPolicy.backoffMs(0)
        assertEquals(expected, result)
    }
}

