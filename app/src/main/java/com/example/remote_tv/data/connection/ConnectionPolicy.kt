package com.example.remote_tv.data.connection

object ConnectionPolicy {
    /** Th\u1eddi gian ch\u1edd t\u1ed1i \u0111a cho m\u1ed7i l\u1ea7n th\u1eed k\u1ebft n\u1ed1i (bao g\u1ed3m SSL handshake v\u1edbi WSS) */
    const val CONNECT_TIMEOUT_MS: Long = 2500
    const val MAX_CONNECT_ROUNDS: Int = 2
    const val BACKOFF_BASE_MS: Long = 500

    fun backoffMs(round: Int): Long {
        return BACKOFF_BASE_MS * round.coerceAtLeast(1)
    }
}
