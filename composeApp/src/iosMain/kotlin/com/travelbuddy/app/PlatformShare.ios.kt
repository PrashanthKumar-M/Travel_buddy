package com.travelbuddy.app

actual object PlatformShare {
    actual fun shareText(text: String, title: String) {}
    actual fun openDialer(phoneNumber: String) {}
    actual fun copyToClipboard(text: String, label: String) {}
    actual fun isNetworkAvailable(): Boolean = true
    actual fun setLightStatusBar(isLight: Boolean) {}
}
