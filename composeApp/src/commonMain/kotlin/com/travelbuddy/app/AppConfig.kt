package com.travelbuddy.app

/**
 * Runtime endpoints for Travel Buddy.
 * Connected to live production backend on Render: https://travel-buddy-3vz4.onrender.com
 */
data class AppConfig(
    val apiBaseUrl: String = "https://travel-buddy-3vz4.onrender.com",
    val webSocketUrl: String = "wss://travel-buddy-3vz4.onrender.com/ws/live",
    val renderBackendUrl: String = "https://travel-buddy-3vz4.onrender.com",
    val mapStyleUrl: String? = null,
    /** Enable remote API synchronization with live Render server */
    val useRemoteApi: Boolean = true
) {
    companion object {
        const val LIVE_RENDER_URL = "https://travel-buddy-3vz4.onrender.com"
        const val LIVE_WS_URL = "wss://travel-buddy-3vz4.onrender.com/ws/live"

        fun liveRender() = AppConfig(
            apiBaseUrl = LIVE_RENDER_URL,
            webSocketUrl = LIVE_WS_URL,
            renderBackendUrl = LIVE_RENDER_URL,
            useRemoteApi = true
        )

        fun forAndroidEmulator() = AppConfig(
            apiBaseUrl = "http://10.0.2.2:8080",
            webSocketUrl = "ws://10.0.2.2:8080/ws/live",
            useRemoteApi = true
        )
    }
}
