package com.travelbuddy.app

actual object PlatformShare {
    actual fun shareText(text: String, title: String) {}
    actual fun openDialer(phoneNumber: String) {}
}
