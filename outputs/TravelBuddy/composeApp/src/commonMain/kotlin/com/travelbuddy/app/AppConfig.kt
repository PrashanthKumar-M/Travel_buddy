package com.travelbuddy.app

/**
 * Runtime endpoints for Travel Buddy.
 * Supports Local development, Android emulator (10.0.2.2), and Render free hosting.
 */
data class AppConfig(
    val apiBaseUrl: String = "http://localhost:8080",
    val webSocketUrl: String = "ws://localhost:8080/ws/live",
    val renderBackendUrl: String = "https://travelbuddy-api.onrender.com",
    val mapStyleUrl: String? = null,
    /** Enable remote API synchronization when backend is reachable */
    val useRemoteApi: Boolean = true
) {
    companion object {
        /** Default config for Android Emulator */
        fun forAndroidEmulator() = AppConfig(
            apiBaseUrl = "http://10.0.2.2:8080",
            webSocketUrl = "ws://10.0.2.2:8080/ws/live",
            useRemoteApi = true
        )

        /** Default config for Render Cloud Deployment */
        fun forRender(renderBaseUrl: String = "https://travelbuddy-api.onrender.com") = AppConfig(
            apiBaseUrl = renderBaseUrl.trimEnd('/'),
            webSocketUrl = renderBaseUrl.replace("https://", "wss://").replace("http://", "ws://").trimEnd('/') + "/ws/live",
            useRemoteApi = true
        )
    }
}
