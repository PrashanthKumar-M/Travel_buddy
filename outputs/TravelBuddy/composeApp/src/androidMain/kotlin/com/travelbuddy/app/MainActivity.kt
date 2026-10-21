package com.travelbuddy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TravelBuddyApp(AppConfig(apiBaseUrl = BuildConfig.TRAVEL_BUDDY_API_URL))
        }
    }
}
