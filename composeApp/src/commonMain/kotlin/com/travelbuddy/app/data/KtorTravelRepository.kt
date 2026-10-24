package com.travelbuddy.app.data

import com.travelbuddy.app.PlatformBattery
import com.travelbuddy.app.PlatformLocation
import com.travelbuddy.app.domain.Expense
import com.travelbuddy.app.domain.Settlement
import com.travelbuddy.app.domain.Trip
import com.travelbuddy.app.domain.TripMember
import com.travelbuddy.app.domain.TripStatus
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Production HTTP adapter for the routes supplied in Postman. */
class KtorTravelRepository(
    private val config: TravelApiConfig,
    private val client: HttpClient = defaultHttpClient()
) : TravelRepository {
    override suspend fun requestOtp(phone: String?, email: String?): Result<String?> = runCatching {
        val type = if (email != null && email.isNotBlank()) "EMAIL" else "MOBILE"
        val response = client.post(config.requestOtpEndpoint()) {
            contentType(ContentType.Application.Json)
            setBody(RequestOtpRequest(phone = phone, email = email, type = type))
        }
        response.requireSuccess()
        val text = response.bodyAsText()
        val json = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull()
        json?.get("otp")?.jsonPrimitive?.contentOrNull
    }

    override suspend fun verifyOtp(request: OtpVerificationRequest): Result<Unit> = runCatching {
        client.post(config.verifyOtpEndpoint()) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.requireSuccess()
    }

    override suspend fun createTrip(request: CreateTripRequest): Result<Trip> = runCatching {
        val response = client.post(config.createTripEndpoint()) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        response.requireSuccess()
        val created = response.bodyAsText().toTripOrFallback(request)

        // Immediately push creator's authentic live GPS coordinates so the server never keeps a default/Bengaluru pin
        val live = PlatformLocation.getCurrentLocation() ?: PlatformLocation.fetchFreshLocation()
        val initLat = when {
            live != null && live.latitude != 0.0 -> live.latitude
            request.sourceLat != 0.0 -> request.sourceLat
            else -> 0.0
        }
        val initLng = when {
            live != null && live.longitude != 0.0 -> live.longitude
            request.sourceLng != 0.0 -> request.sourceLng
            else -> 0.0
        }
        val initSpeed = live?.speedKmh ?: 0.0
        val initBattery = PlatformBattery.getBatteryLevel()

        if (initLat != 0.0 && initLng != 0.0) {
            runCatching {
                updateLocation(
                    LocationUpdateRequest(
                        tripId = created.id,
                        userId = request.createdBy,
                        latitude = initLat,
                        longitude = initLng,
                        speed = initSpeed,
                        battery = initBattery,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            val creatorNorm = request.createdBy.trim().lowercase().substringBefore("@")
            val updatedMembers = created.members.mapIndexed { idx, m ->
                val mNorm = m.name.trim().lowercase().substringBefore("@")
                if (idx == 0 || mNorm == creatorNorm || m.id.trim().lowercase() == creatorNorm) {
                    m.copy(latitude = initLat, longitude = initLng, speedKmh = initSpeed, batteryPercent = initBattery)
                } else m
            }
            created.copy(
                members = updatedMembers,
                sourceLat = if (created.sourceLat != 0.0) created.sourceLat else initLat,
                sourceLng = if (created.sourceLng != 0.0) created.sourceLng else initLng
            )
        } else {
            created
        }
    }

    override suspend fun deleteTrip(tripId: String): Result<Boolean> = runCatching {
        val response = client.delete(config.deleteTripEndpoint(tripId))
        response.status.isSuccess()
    }

    override suspend fun joinTrip(tripCode: String, userId: String, userName: String): Result<Trip> = runCatching {
        val live = PlatformLocation.getCurrentLocation() ?: PlatformLocation.fetchFreshLocation()
        val liveLat = live?.latitude?.takeIf { it != 0.0 }
        val liveLng = live?.longitude?.takeIf { it != 0.0 }
        val liveSpeed = live?.speedKmh ?: 0.0
        val liveBattery = PlatformBattery.getBatteryLevel()

        val response = client.post(config.joinTripEndpoint()) {
            contentType(ContentType.Application.Json)
            setBody(
                JoinTripRequest(
                    tripCode = tripCode,
                    userId = userId,
                    userName = userName,
                    latitude = liveLat,
                    longitude = liveLng
                )
            )
        }
        response.requireSuccess()
        val text = response.bodyAsText()
        val joined = text.toTripOrFallback(null, tripCode)

        // Immediately push joining member's live GPS to /api/locations so Leader never sees "Waiting for GPS" / "Connecting..."
        if (liveLat != null && liveLng != null) {
            runCatching {
                updateLocation(
                    LocationUpdateRequest(
                        tripId = joined.id,
                        userId = userName.ifBlank { userId },
                        latitude = liveLat,
                        longitude = liveLng,
                        speed = liveSpeed,
                        battery = liveBattery,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            val myNorm = userName.trim().lowercase().substringBefore("@")
            val myIdNorm = userId.trim().lowercase().substringBefore("@")
            val updatedMembers = joined.members.map { m ->
                val mNorm = m.name.trim().lowercase().substringBefore("@")
                val mId = m.id.trim().lowercase().substringBefore("@")
                if (mNorm == myNorm || mId == myNorm || mNorm == myIdNorm || mId == myIdNorm) {
                    m.copy(latitude = liveLat, longitude = liveLng, speedKmh = liveSpeed, batteryPercent = liveBattery)
                } else m
            }
            joined.copy(members = updatedMembers)
        } else {
            joined
        }
    }

    override suspend fun liveSnapshot(tripId: String): Result<Trip> = runCatching {
        val response = client.get(config.liveSnapshotEndpoint(tripId))
        response.requireSuccess()
        response.bodyAsText().toTripOrFallback(null, tripId)
    }

    override suspend fun updateLocation(request: LocationUpdateRequest): Result<Unit> = runCatching {
        client.post(config.updateLocationEndpoint()) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.requireSuccess()
    }

    override suspend fun addExpense(request: AddExpenseRequest): Result<Expense> = runCatching {
        val response = client.post(config.addExpenseEndpoint()) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        response.requireSuccess()
        Expense(
            id = response.bodyAsText().jsonString("id") ?: "pending",
            title = "Expense",
            amount = request.amount,
            paidBy = request.paidBy,
            splitWith = request.members.toSet()
        )
    }

    override suspend fun settlements(tripId: String): Result<List<Settlement>> = runCatching {
        val response = client.get(config.settlementsEndpoint(tripId))
        response.requireSuccess()
        response.bodyAsText().toSettlements()
    }

    override suspend fun getTrips(): Result<List<Trip>> = runCatching {
        val response = client.get(config.tripsEndpoint())
        response.requireSuccess()
        val text = response.bodyAsText()
        val jsonArray = Json.parseToJsonElement(text).jsonArray
        jsonArray.mapNotNull { element ->
            val obj = element.jsonObject
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Trip"
            val src = obj["source"]?.jsonPrimitive?.contentOrNull ?: ""
            val dst = obj["destination"]?.jsonPrimitive?.contentOrNull ?: ""
            val dates = obj["dates"]?.jsonPrimitive?.contentOrNull ?: ""
            val cover = obj["coverImage"]?.jsonPrimitive?.contentOrNull
            val vType = obj["vehicleType"]?.jsonPrimitive?.contentOrNull ?: "CAR"
            val tripCode = obj["tripCode"]?.jsonPrimitive?.contentOrNull ?: id
            val createdBy = obj["createdBy"]?.jsonPrimitive?.contentOrNull ?: obj["creator"]?.jsonPrimitive?.contentOrNull ?: ""
            val sLat = obj["sourceLat"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val sLng = obj["sourceLng"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val dLat = obj["destLat"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val dLng = obj["destLng"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val members = obj["members"]?.jsonArray?.mapNotNull { m ->
                val mObj = m.jsonObject
                val mId = mObj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val mName = mObj["name"]?.jsonPrimitive?.contentOrNull ?: mId
                val rawLat = mObj["latitude"]?.jsonPrimitive?.doubleOrNull
                val rawLng = mObj["longitude"]?.jsonPrimitive?.doubleOrNull
                // Filter out legacy server hardcoded Bengaluru default (12.9716, 77.5946)
                val isLegacyDefault = rawLat != null && rawLng != null &&
                    kotlin.math.abs(rawLat - 12.9716) < 0.00001 && kotlin.math.abs(rawLng - 77.5946) < 0.00001
                val lat = if (isLegacyDefault) null else rawLat
                val lng = if (isLegacyDefault) null else rawLng
                val speed = mObj["speedKmh"]?.jsonPrimitive?.doubleOrNull ?: mObj["speed"]?.jsonPrimitive?.doubleOrNull
                val battery = mObj["batteryPercent"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: mObj["battery"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                val avatar = mObj["avatarType"]?.jsonPrimitive?.contentOrNull ?: "MALE_1"
                TripMember(mId, mName, 0xFF2F80ED, true, lat, lng, speed, battery, avatar)
            } ?: emptyList()
            Trip(
                id = id,
                name = name,
                source = src,
                destination = dst,
                dates = dates,
                status = TripStatus.LIVE,
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
    }

    override suspend fun getExpenses(tripId: String): Result<List<Expense>> = runCatching {
        val response = client.get(config.expensesEndpoint(tripId))
        response.requireSuccess()
        val text = response.bodyAsText()
        val jsonArray = Json.parseToJsonElement(text).jsonArray
        jsonArray.mapNotNull { element ->
            val obj = element.jsonObject
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "Expense"
            val amount = obj["amount"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val paidBy = obj["paidBy"]?.jsonPrimitive?.contentOrNull ?: ""
            val splitWith = obj["splitWith"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet() ?: emptySet()
            Expense(id, title, amount, paidBy, splitWith)
        }
    }

    override suspend fun getChatMessages(tripId: String): Result<List<RemoteChatMessage>> = runCatching {
        val response = client.get(config.chatMessagesEndpoint(tripId))
        response.requireSuccess()
        val text = response.bodyAsText()
        val jsonArray = Json.parseToJsonElement(text).jsonArray
        jsonArray.mapNotNull { element ->
            val obj = element.jsonObject
            val msgText = obj["text"]?.jsonPrimitive?.contentOrNull
                ?: obj["message"]?.jsonPrimitive?.contentOrNull
                ?: ""
            RemoteChatMessage(
                id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                tripId = obj["tripId"]?.jsonPrimitive?.contentOrNull ?: tripId,
                senderId = obj["senderId"]?.jsonPrimitive?.contentOrNull ?: "",
                senderName = obj["senderName"]?.jsonPrimitive?.contentOrNull ?: "",
                message = msgText,
                text = msgText,
                timestamp = obj["timestamp"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
            )
        }
    }

    override suspend fun sendChatMessage(tripId: String, request: RemoteSendMessageRequest): Result<RemoteChatMessage> = runCatching {
        val response = client.post(config.sendChatMessageEndpoint(tripId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        response.requireSuccess()
        val obj = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val msgText = obj["text"]?.jsonPrimitive?.contentOrNull
            ?: obj["message"]?.jsonPrimitive?.contentOrNull
            ?: request.text
        RemoteChatMessage(
            id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
            tripId = obj["tripId"]?.jsonPrimitive?.contentOrNull ?: tripId,
            senderId = obj["senderId"]?.jsonPrimitive?.contentOrNull ?: request.senderId,
            senderName = obj["senderName"]?.jsonPrimitive?.contentOrNull ?: request.senderName,
            message = msgText,
            text = msgText,
            timestamp = obj["timestamp"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: System.currentTimeMillis()
        )
    }

    override suspend fun triggerSos(request: RemoteSosRequest): Result<Unit> = runCatching {
        client.post(config.triggerSosEndpoint()) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.requireSuccess()
    }

    fun close() = client.close()
}

fun defaultHttpClient(): HttpClient = HttpClient {
    install(HttpTimeout) {
        requestTimeoutMillis = 6_000
        connectTimeoutMillis = 4_000
        socketTimeoutMillis = 6_000
    }
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
    install(WebSockets) { pingIntervalMillis = 20_000 }
}

private fun HttpResponse.requireSuccess() {
    check(status.isSuccess()) { "Request failed: HTTP ${status.value}" }
}

private fun String.jsonObjectOrNull(): JsonObject? = runCatching { Json.parseToJsonElement(this).jsonObject }.getOrNull()
private fun String.jsonString(key: String): String? = jsonObjectOrNull()?.get(key)?.jsonPrimitive?.contentOrNull

private fun String.toTripOrFallback(request: CreateTripRequest?, expectedId: String = ""): Trip {
    val rootObj = jsonObjectOrNull()
    val json = if (rootObj?.containsKey("trip") == true) rootObj["trip"]?.jsonObject else rootObj
    val vType = json?.get("vehicleType")?.jsonPrimitive?.contentOrNull ?: request?.vehicleType ?: "CAR"
    val members = json?.get("members")?.jsonArray?.mapNotNull { element ->
        val item = element.jsonObject
        val id = item["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val name = item["name"]?.jsonPrimitive?.contentOrNull ?: id
        val rawLat = item["latitude"]?.jsonPrimitive?.doubleOrNull
        val rawLng = item["longitude"]?.jsonPrimitive?.doubleOrNull
        val isLegacyDefault = rawLat != null && rawLng != null &&
            kotlin.math.abs(rawLat - 12.9716) < 0.00001 && kotlin.math.abs(rawLng - 77.5946) < 0.00001
        val lat = if (isLegacyDefault) null else rawLat
        val lng = if (isLegacyDefault) null else rawLng
        val speed = item["speedKmh"]?.jsonPrimitive?.doubleOrNull ?: item["speed"]?.jsonPrimitive?.doubleOrNull
        val battery = item["batteryPercent"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: item["battery"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        val avatar = item["avatarType"]?.jsonPrimitive?.contentOrNull ?: "MALE_1"
        TripMember(id, name, 0xFF2F80ED, true, lat, lng, speed, battery, avatar)
    } ?: request?.members?.map { TripMember(it, it, 0xFF2F80ED, true) } ?: emptyList()
    val fallbackDynamicId = request?.id ?: "trip-${(100000..999999).random()}"
    val tripId = json?.get("id")?.jsonPrimitive?.contentOrNull ?: expectedId.ifBlank { fallbackDynamicId }
    val tripCode = json?.get("tripCode")?.jsonPrimitive?.contentOrNull ?: expectedId.ifBlank { tripId }
    val createdBy = json?.get("createdBy")?.jsonPrimitive?.contentOrNull
        ?: json?.get("creator")?.jsonPrimitive?.contentOrNull
        ?: request?.createdBy ?: ""
    val sLat = json?.get("sourceLat")?.jsonPrimitive?.doubleOrNull ?: request?.sourceLat ?: 0.0
    val sLng = json?.get("sourceLng")?.jsonPrimitive?.doubleOrNull ?: request?.sourceLng ?: 0.0
    val dLat = json?.get("destLat")?.jsonPrimitive?.doubleOrNull ?: request?.destLat ?: 0.0
    val dLng = json?.get("destLng")?.jsonPrimitive?.doubleOrNull ?: request?.destLng ?: 0.0
    return Trip(
        id = tripId,
        name = json?.get("name")?.jsonPrimitive?.contentOrNull ?: json?.get("tripName")?.jsonPrimitive?.contentOrNull ?: request?.tripName ?: "Live trip",
        source = json?.get("source")?.jsonPrimitive?.contentOrNull ?: request?.source ?: "",
        destination = json?.get("destination")?.jsonPrimitive?.contentOrNull ?: request?.destination ?: "",
        dates = json?.get("dates")?.jsonPrimitive?.contentOrNull ?: request?.dates ?: "Planned",
        status = TripStatus.LIVE,
        members = members,
        coverImage = json?.get("coverImage")?.jsonPrimitive?.contentOrNull ?: request?.coverImage,
        vehicleType = vType,
        tripCode = tripCode,
        createdBy = createdBy,
        sourceLat = sLat,
        sourceLng = sLng,
        destLat = dLat,
        destLng = dLng
    )
}

private fun String.toSettlements(): List<Settlement> = runCatching {
    Json.parseToJsonElement(this).jsonArray.mapNotNull { element ->
        val item = element.jsonObject
        val from = item["from"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val to = item["to"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val amount = item["amount"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
        Settlement(from, to, amount)
    }
}.getOrDefault(emptyList())
