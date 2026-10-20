package com.travelbuddy.app

actual fun dispatchDeviceOtpNotification(phone: String, otp: String) {
    println(">>> [iOS OTP] Sent to +91 $phone: $otp")
}
