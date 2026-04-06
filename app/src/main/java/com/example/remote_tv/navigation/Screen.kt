package com.example.remote_tv.navigation

sealed class Screen(val route: String) {
    object Remote   : Screen("remote")
    object Channels : Screen("channels")
    object Cast     : Screen("cast")
    object Settings : Screen("settings")
}

