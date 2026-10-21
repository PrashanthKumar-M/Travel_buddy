package com.travelbuddy.app

expect object PlatformStorage {
    fun getString(key: String, defaultValue: String? = null): String?
    fun setString(key: String, value: String?)
    fun clear()
}

object SessionManager {
    private const val KEY_TOKEN = "tb_auth_token"
    private const val KEY_USER_ID = "tb_user_id"
    private const val KEY_USER_NAME = "tb_user_name"
    private const val KEY_USER_IDENTIFIER = "tb_user_identifier"
    private const val KEY_IS_EMAIL = "tb_is_email"
    private const val KEY_LOGGED_IN = "tb_is_logged_in"

    fun saveSession(token: String, userId: String, name: String, identifier: String, isEmail: Boolean) {
        PlatformStorage.setString(KEY_TOKEN, token)
        PlatformStorage.setString(KEY_USER_ID, userId)
        PlatformStorage.setString(KEY_USER_NAME, name)
        PlatformStorage.setString(KEY_USER_IDENTIFIER, identifier)
        PlatformStorage.setString(KEY_IS_EMAIL, if (isEmail) "true" else "false")
        PlatformStorage.setString(KEY_LOGGED_IN, "true")
    }

    fun isLoggedIn(): Boolean {
        return PlatformStorage.getString(KEY_LOGGED_IN) == "true"
    }

    fun getUserIdentifier(): String {
        return PlatformStorage.getString(KEY_USER_IDENTIFIER) ?: ""
    }

    fun getUserName(): String {
        return PlatformStorage.getString(KEY_USER_NAME) ?: "Traveler"
    }

    fun isEmail(): Boolean {
        return PlatformStorage.getString(KEY_IS_EMAIL) == "true"
    }

    fun getToken(): String? {
        return PlatformStorage.getString(KEY_TOKEN)
    }

    fun clearSession() {
        PlatformStorage.clear()
    }
}
