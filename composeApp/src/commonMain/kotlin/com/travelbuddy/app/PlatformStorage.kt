package com.travelbuddy.app

expect object PlatformStorage {
    fun getString(key: String, defaultValue: String? = null): String?
    fun setString(key: String, value: String?)
    fun clear()
}

expect fun getCurrentHourOfDay(): Int

fun getTimeBasedGreeting(): String {
    val hour = runCatching { getCurrentHourOfDay() }.getOrDefault(12)
    return when (hour) {
        in 4..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

fun getInitials(name: String): String {
    val clean = name.trim().filter { it.isLetterOrDigit() || it.isWhitespace() }
    val parts = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> "TB"
    }
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

    fun setUserName(name: String) {
        PlatformStorage.setString(KEY_USER_NAME, name)
    }

    fun setUserEmail(email: String) {
        PlatformStorage.setString(KEY_USER_IDENTIFIER, email)
    }

    fun isLoggedIn(): Boolean {
        return PlatformStorage.getString(KEY_LOGGED_IN) == "true"
    }

    fun getUserId(): String {
        return PlatformStorage.getString(KEY_USER_ID) ?: ""
    }

    fun getUserIdentifier(): String {
        return PlatformStorage.getString(KEY_USER_IDENTIFIER) ?: ""
    }

    fun getUserName(): String {
        return PlatformStorage.getString(KEY_USER_NAME) ?: ""
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
