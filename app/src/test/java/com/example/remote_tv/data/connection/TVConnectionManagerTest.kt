package com.example.remote_tv.data.connection

import com.example.remote_tv.data.model.PlaybackState
import com.example.remote_tv.data.model.TVBrand
import com.example.remote_tv.data.model.TVDevice
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class TVConnectionManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var httpClient: HttpClient
    private lateinit var tvConnectionManager: TVConnectionManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        httpClient = mock(HttpClient::class.java)
        tvConnectionManager = TVConnectionManager(httpClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be disconnected and IDLE`() = runTest {
        assertEquals(null, tvConnectionManager.currentDevice.value)
        assertEquals(null, tvConnectionManager.connectionError.value)
        assertEquals(PlaybackState.IDLE, tvConnectionManager.playbackState.value)
    }

    @Test
    fun `disconnect should reset state`() = runTest {
        tvConnectionManager.disconnect()
        
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, tvConnectionManager.currentDevice.value)
        assertEquals(null, tvConnectionManager.connectionError.value)
        assertEquals(PlaybackState.IDLE, tvConnectionManager.playbackState.value)
    }
}
