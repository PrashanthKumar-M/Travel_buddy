package com.travelbuddy.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object AndroidNotificationHelper {
    var appContext: Context? = null
    private const val CHANNEL_ID = "travel_buddy_otp_channel"

    fun showOtpNotification(phone: String, otp: String) {
        val context = appContext ?: return
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Verification Codes",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Travel Buddy login and OTP verification codes"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Travel Buddy Verification")
                .setContentText("Your OTP is: $otp. Valid for 10 minutes.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Your Travel Buddy verification code is $otp. Valid for 10 minutes. Do not share this code."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
            println(">>> [Travel Buddy OTP Notification] Displayed code $otp for $phone")
        } catch (e: Throwable) {
            println(">>> [Travel Buddy OTP Notification] Error: ${e.message}")
        }
    }
}

actual fun dispatchDeviceOtpNotification(phone: String, otp: String) {
    AndroidNotificationHelper.showOtpNotification(phone, otp)
}
