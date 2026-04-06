package com.example.remote_tv.data.model

data class UserProfile(
    val id: String = "local_user",
    val displayName: String = "Guest",
    val email: String = "",
    val avatarSeed: Int = 1
)
