package com.example.remote_tv.data.debug

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object InAppDiagnostics {

    private const val maxEntries = 200
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun info(tag: String, message: String) = append("I", tag, message)
    fun warn(tag: String, message: String) = append("W", tag, message)
    fun error(tag: String, message: String) = append("E", tag, message)

    fun clear() {
        _logs.value = emptyList()
    }

    private fun append(level: String, tag: String, message: String) {
        val timestamp = LocalTime.now().format(formatter)
        val line = "$timestamp $level/$tag: $message"
        val updated = (_logs.value + line).takeLast(maxEntries)
        _logs.value = updated
    }
}
