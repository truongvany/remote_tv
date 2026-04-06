package com.example.remote_tv.data.protocol

import android.util.Log
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

class LGProtocol(private val client: HttpClient) : TVProtocol {
    private val tag = "LGProtocol"
    private var clientKey: String? = null

    override suspend fun connect(ip: String, port: Int): Boolean {
        // LG WebOS discovery and pairing usually involves a REST call or WS
        // Simplified for this example
        return try {
            val response: HttpResponse = client.get("http://$ip:$port/roap/api/auth") {
                contentType(ContentType.Application.Json)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun disconnect() {
        clientKey = null
    }

    override suspend fun sendCommand(command: String): Boolean {
        Log.w(tag, "LG command bridge is not implemented yet. command=$command")
        return false
    }

    override suspend fun launchApp(appId: String): Boolean {
        Log.w(tag, "LG app launch bridge is not implemented yet. appId=$appId")
        return false
    }
}
