package com.travelbuddy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlatformStorage.init(applicationContext)
        PlatformBattery.appContext = applicationContext
        AndroidNotificationHelper.appContext = applicationContext
        val permissions = mutableListOf<String>()
        if (checkSelfPermission(android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.SEND_SMS)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 101)
        }
        setContent {
            TravelBuddyApp(
                AppConfig(
                    apiBaseUrl = BuildConfig.TRAVEL_BUDDY_API_URL,
                    webSocketUrl = BuildConfig.TRAVEL_BUDDY_WS_URL,
                    renderBackendUrl = BuildConfig.TRAVEL_BUDDY_API_URL,
                    useRemoteApi = true
                )
            )
        }
    }
}
