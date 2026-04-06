package com.example.remote_tv.data.model

enum class AppLaunchStatus {
    SUCCESS,
    NO_SESSION,
    UNSUPPORTED,
    FAILED,
}

data class AppLaunchResult(
    val status: AppLaunchStatus,
    val requestedAppId: String,
    val resolvedAppId: String = requestedAppId,
    val message: String? = null,
) {
    val isSuccess: Boolean
        get() = status == AppLaunchStatus.SUCCESS

    companion object {
        fun success(requestedAppId: String, resolvedAppId: String): AppLaunchResult {
            return AppLaunchResult(
                status = AppLaunchStatus.SUCCESS,
                requestedAppId = requestedAppId,
                resolvedAppId = resolvedAppId,
            )
        }

        fun noSession(appId: String): AppLaunchResult {
            return AppLaunchResult(
                status = AppLaunchStatus.NO_SESSION,
                requestedAppId = appId,
                message = "No active connection",
            )
        }

        fun unsupported(requestedAppId: String, resolvedAppId: String, message: String): AppLaunchResult {
            return AppLaunchResult(
                status = AppLaunchStatus.UNSUPPORTED,
                requestedAppId = requestedAppId,
                resolvedAppId = resolvedAppId,
                message = message,
            )
        }

        fun failed(requestedAppId: String, resolvedAppId: String, message: String): AppLaunchResult {
            return AppLaunchResult(
                status = AppLaunchStatus.FAILED,
                requestedAppId = requestedAppId,
                resolvedAppId = resolvedAppId,
                message = message,
            )
        }
    }
}
