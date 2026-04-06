package com.example.remote_tv.data.connection

object ConnectionPolicy {
    const val CONNECT_TIMEOUT_MS: Long = 1200
    const val MAX_CONNECT_ROUNDS: Int = 2
    const val BACKOFF_BASE_MS: Long = 350

    fun backoffMs(round: Int): Long {
        return BACKOFF_BASE_MS * round.coerceAtLeast(1)
    }
}
