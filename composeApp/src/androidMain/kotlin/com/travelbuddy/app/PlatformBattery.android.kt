package com.travelbuddy.app

import android.content.Context
import android.os.BatteryManager

actual object PlatformBattery {
    var appContext: Context? = null

    actual fun getBatteryLevel(): Int {
        val ctx = appContext ?: return 92
        return try {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val cap = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (cap in 1..100) cap else 92
        } catch (_: Exception) {
            92
        }
    }
}
