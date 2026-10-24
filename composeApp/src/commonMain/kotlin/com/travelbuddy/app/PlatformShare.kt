package com.travelbuddy.app

expect object PlatformShare {
    fun shareText(text: String, title: String = "Share Trip ID")
    fun openDialer(phoneNumber: String)
    fun copyToClipboard(text: String, label: String = "Trip ID")
    fun isNetworkAvailable(): Boolean
    fun setLightStatusBar(isLight: Boolean)
}
