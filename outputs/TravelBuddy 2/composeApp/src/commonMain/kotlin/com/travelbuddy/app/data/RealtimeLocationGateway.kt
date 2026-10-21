package com.travelbuddy.app.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class LiveLocationEvent(
    val tripId: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Double? = null,
    val battery: Int? = null,
    val timestamp: Long? = null
)

/** Connects to the exact server WS endpoint supplied at deployment time. */
class RealtimeLocationGateway(
    private val webSocketUrl: String,
    private val client: HttpClient = defaultHttpClient()
) {
    suspend fun collect(onLocation: (LiveLocationEvent) -> Unit) {
        client.webSocket(webSocketUrl) {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                frame.readText().toLiveLocationOrNull()?.let(onLocation)
            }
        }
    }

    fun close() = client.close()
}

/**
 * Shared outbound half of live tracking. Android/iOS platform location services
 * feed it verified device coordinates; the Spring Boot API persists the update
 * and broadcasts it to subscribed trip members.
 */
class LiveTrackingCoordinator(
    private val repository: TravelRepository,
    private val tripId: String,
    private val userId: String
) {
    suspend fun publish(
        latitude: Double,
        longitude: Double,
        speed: Double,
        battery: Int,
        timestamp: Long
    ): Result<Unit> = repository.updateLocation(
        LocationUpdateRequest(
            tripId = tripId,
            userId = userId,
            latitude = latitude,
            longitude = longitude,
            speed = speed,
            battery = battery,
            timestamp = timestamp
        )
    )
}

private fun String.toLiveLocationOrNull(): LiveLocationEvent? {
    return runCatching {
        val payload = Json.parseToJsonElement(this).jsonObject
        val tripId = requireNotNull(payload["tripId"]?.jsonPrimitive?.contentOrNull)
        val userId = requireNotNull(payload["userId"]?.jsonPrimitive?.contentOrNull)
        val latitude = requireNotNull(payload["latitude"]?.jsonPrimitive?.doubleOrNull)
        val longitude = requireNotNull(payload["longitude"]?.jsonPrimitive?.doubleOrNull)
        LiveLocationEvent(
            tripId = tripId,
            userId = userId,
            latitude = latitude,
            longitude = longitude,
            speed = payload["speed"]?.jsonPrimitive?.doubleOrNull,
            battery = payload["battery"]?.jsonPrimitive?.intOrNull,
            timestamp = payload["timestamp"]?.jsonPrimitive?.longOrNull
        )
    }.getOrNull()
}
