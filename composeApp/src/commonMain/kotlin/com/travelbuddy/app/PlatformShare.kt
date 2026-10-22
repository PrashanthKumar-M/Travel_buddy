package com.travelbuddy.app

expect object PlatformShare {
    fun shareText(text: String, title: String = "Share Trip ID")
}
