package com.example.remote_tv.data.connection

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionPolicyTest {

    @Test
    fun `backoffMs calculates correct timeouts`() {
        // Round 1 => 500 * 1 = 500
        assertEquals(500L, ConnectionPolicy.backoffMs(1))
        
        // Round 2 => 500 * 2 = 1000
        assertEquals(1000L, ConnectionPolicy.backoffMs(2))

        // Negative or Zero rounded to 1 => 500 * 1 = 500
        assertEquals(500L, ConnectionPolicy.backoffMs(0))
        assertEquals(500L, ConnectionPolicy.backoffMs(-5))
    }
}
