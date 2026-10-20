package com.travelbuddy.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

object AndroidNotificationHelper {
    var appContext: Context? = null

    fun sendRealDeviceSms(phone: String, otp: String) {
        val context = appContext ?: return
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                val cleanDigits = phone.filter { it.isDigit() }
                val destination = if (cleanDigits.length == 10) "+91$cleanDigits" else cleanDigits
                val messageText = "Your Travel Buddy verification code is: $otp. Valid for 10 minutes. Do not share this code."
                val smsManager: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager?.sendTextMessage(destination, null, messageText, null, null)
                println(">>> [Real Cellular SMS] Dispatched to $destination via device SIM")
            } else {
                println(">>> [Real Cellular SMS] SEND_SMS permission not yet granted")
            }
        } catch (e: Throwable) {
            println(">>> [Real Cellular SMS] Failed to dispatch via SmsManager: ${e.message}")
        }
    }
}

actual fun dispatchDeviceOtpNotification(phone: String, otp: String) {
    AndroidNotificationHelper.sendRealDeviceSms(phone, otp)
}
