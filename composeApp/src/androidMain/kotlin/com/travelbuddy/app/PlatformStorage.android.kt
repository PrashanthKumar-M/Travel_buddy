package com.travelbuddy.app

import android.content.Context
import android.content.SharedPreferences

actual object PlatformStorage {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences("travel_buddy_session", Context.MODE_PRIVATE)
        }
    }

    actual fun getString(key: String, defaultValue: String?): String? {
        return prefs?.getString(key, defaultValue) ?: defaultValue
    }

    actual fun setString(key: String, value: String?) {
        prefs?.edit()?.apply {
            if (value == null) {
                remove(key)
            } else {
                putString(key, value)
            }
            apply()
        }
    }

    actual fun clear() {
        prefs?.edit()?.clear()?.apply()
    }
}

actual fun getCurrentHourOfDay(): Int {
    return java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
}
