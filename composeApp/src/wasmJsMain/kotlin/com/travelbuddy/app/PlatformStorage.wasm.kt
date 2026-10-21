package com.travelbuddy.app

actual object PlatformStorage {
    private val memoryStore = mutableMapOf<String, String>()

    actual fun getString(key: String, defaultValue: String?): String? {
        return memoryStore[key] ?: defaultValue
    }

    actual fun setString(key: String, value: String?) {
        if (value == null) {
            memoryStore.remove(key)
        } else {
            memoryStore[key] = value
        }
    }

    actual fun clear() {
        memoryStore.clear()
    }
}

actual fun getCurrentHourOfDay(): Int {
    return 12
}
