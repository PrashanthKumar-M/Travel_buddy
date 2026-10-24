package com.travelbuddy.app

import com.travelbuddy.app.domain.Trip
import com.travelbuddy.app.domain.TripMember
import com.travelbuddy.app.domain.TripStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

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

object TripStorage {
    private const val KEY_DELETED_TRIP_IDS = "tb_deleted_trip_ids"
    private const val KEY_CACHED_TRIPS = "tb_cached_trips_v2"
    private const val KEY_ACTIVE_TRIP_ID = "tb_cached_active_trip_id"

    fun getDeletedTripIds(): Set<String> {
        val raw = PlatformStorage.getString(KEY_DELETED_TRIP_IDS) ?: return emptySet()
        return raw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    }

    fun markTripDeleted(tripId: String) {
        val clean = tripId.trim().lowercase()
        if (clean.isBlank()) return
        val current = getDeletedTripIds().toMutableSet()
        current.add(clean)
        PlatformStorage.setString(KEY_DELETED_TRIP_IDS, current.joinToString(","))
    }

    fun isTripDeleted(tripId: String): Boolean {
        val clean = tripId.trim().lowercase()
        return getDeletedTripIds().contains(clean)
    }

    fun saveActiveTripId(tripId: String) {
        PlatformStorage.setString(KEY_ACTIVE_TRIP_ID, tripId)
    }

    fun getActiveTripId(): String? {
        return PlatformStorage.getString(KEY_ACTIVE_TRIP_ID)
    }

    fun saveCachedTrips(trips: List<Trip>) {
        try {
            val jsonArray = buildJsonArray {
                trips.forEach { trip ->
                    add(buildJsonObject {
                        put("id", trip.id)
                        put("name", trip.name)
                        put("source", trip.source)
                        put("destination", trip.destination)
                        put("dates", trip.dates)
                        put("status", trip.status.name)
                        put("coverImage", trip.coverImage)
                        put("vehicleType", trip.vehicleType)
                        put("tripCode", trip.tripCode)
                        put("createdBy", trip.createdBy)
                        put("sourceLat", trip.sourceLat)
                        put("sourceLng", trip.sourceLng)
                        put("destLat", trip.destLat)
                        put("destLng", trip.destLng)
                        put("members", buildJsonArray {
                            trip.members.forEach { m ->
                                add(buildJsonObject {
                                    put("id", m.id)
                                    put("name", m.name)
                                    put("color", m.color)
                                    put("isSharingLocation", m.isSharingLocation)
                                    m.latitude?.let { put("latitude", it) }
                                    m.longitude?.let { put("longitude", it) }
                                    m.speedKmh?.let { put("speedKmh", it) }
                                    m.batteryPercent?.let { put("batteryPercent", it) }
                                    put("avatarType", m.avatarType)
                                })
                            }
                        })
                    })
                }
            }
            PlatformStorage.setString(KEY_CACHED_TRIPS, jsonArray.toString())
        } catch (_: Exception) {}
    }

    fun loadCachedTrips(): List<Trip> {
        val raw = PlatformStorage.getString(KEY_CACHED_TRIPS) ?: return emptyList()
        return try {
            val deleted = getDeletedTripIds()
            val jsonArray = Json.parseToJsonElement(raw).jsonArray
            jsonArray.mapNotNull { el ->
                val obj = el.jsonObject
                val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val cleanId = id.trim().lowercase()
                if (cleanId in deleted || cleanId == "trip-coorg-expedition" || cleanId == "trip-active-1") return@mapNotNull null
                val tripCode = obj["tripCode"]?.jsonPrimitive?.contentOrNull ?: id
                if (tripCode.trim().lowercase() in deleted || tripCode.equals("TRIP-7489", ignoreCase = true)) return@mapNotNull null

                val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Trip"
                val source = obj["source"]?.jsonPrimitive?.contentOrNull ?: ""
                val dest = obj["destination"]?.jsonPrimitive?.contentOrNull ?: ""
                val dates = obj["dates"]?.jsonPrimitive?.contentOrNull ?: "Active"
                val statusStr = obj["status"]?.jsonPrimitive?.contentOrNull ?: "LIVE"
                val status = runCatching { TripStatus.valueOf(statusStr) }.getOrDefault(TripStatus.LIVE)
                val cover = obj["coverImage"]?.jsonPrimitive?.contentOrNull
                val vType = obj["vehicleType"]?.jsonPrimitive?.contentOrNull ?: "CAR"
                val createdBy = obj["createdBy"]?.jsonPrimitive?.contentOrNull ?: ""
                val sLat = obj["sourceLat"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val sLng = obj["sourceLng"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val dLat = obj["destLat"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val dLng = obj["destLng"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val members = obj["members"]?.jsonArray?.mapNotNull { mEl ->
                    val mObj = mEl.jsonObject
                    val mId = mObj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val mName = mObj["name"]?.jsonPrimitive?.contentOrNull ?: mId
                    val mColor = mObj["color"]?.jsonPrimitive?.longOrNull ?: 0xFF2F80ED
                    val mSharing = mObj["isSharingLocation"]?.jsonPrimitive?.booleanOrNull ?: true
                    val mLat = mObj["latitude"]?.jsonPrimitive?.doubleOrNull
                    val mLng = mObj["longitude"]?.jsonPrimitive?.doubleOrNull
                    val mSpeed = mObj["speedKmh"]?.jsonPrimitive?.doubleOrNull
                    val mBat = mObj["batteryPercent"]?.jsonPrimitive?.intOrNull
                    val mAvatar = mObj["avatarType"]?.jsonPrimitive?.contentOrNull ?: "MALE_1"
                    TripMember(mId, mName, mColor, mSharing, mLat, mLng, mSpeed, mBat, mAvatar)
                } ?: emptyList()

                Trip(
                    id = id,
                    name = name,
                    source = source,
                    destination = dest,
                    dates = dates,
                    status = status,
                    members = members,
                    coverImage = cover,
                    vehicleType = vType,
                    tripCode = tripCode,
                    createdBy = createdBy,
                    sourceLat = sLat,
                    sourceLng = sLng,
                    destLat = dLat,
                    destLng = dLng
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
