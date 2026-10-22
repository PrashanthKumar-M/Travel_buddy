package com.travelbuddy.app.data

import com.travelbuddy.app.domain.Expense
import com.travelbuddy.app.domain.Settlement
import com.travelbuddy.app.domain.Trip
import com.travelbuddy.app.domain.TripMember
import com.travelbuddy.app.domain.TripStatus
import io.ktor.client.HttpClient
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
        response.bodyAsText().toTripOrFallback(request)
    }

    override suspend fun deleteTrip(tripId: String): Result<Boolean> = runCatching {
        val response = client.delete(config.deleteTripEndpoint(tripId))
        response.status.isSuccess()
    }

    override suspend fun joinTrip(tripCode: String, userId: String, userName: String): Result<Trip> = runCatching {
        val response = client.post(config.joinTripEndpoint()) {
            contentType(ContentType.Application.Json)
            setBody(JoinTripRequest(tripCode = tripCode, userId = userId, userName = userName))
        }
        response.requireSuccess()
        val text = response.bodyAsText()
        text.toTripOrFallback(null, tripCode)
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
            val members = obj["members"]?.jsonArray?.mapNotNull { m ->
                val mObj = m.jsonObject
                val mId = mObj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val mName = mObj["name"]?.jsonPrimitive?.contentOrNull ?: mId
                val lat = mObj["latitude"]?.jsonPrimitive?.doubleOrNull
                val lng = mObj["longitude"]?.jsonPrimitive?.doubleOrNull
                val speed = mObj["speedKmh"]?.jsonPrimitive?.doubleOrNull ?: mObj["speed"]?.jsonPrimitive?.doubleOrNull
                val battery = mObj["batteryPercent"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: mObj["battery"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                val avatar = mObj["avatarType"]?.jsonPrimitive?.contentOrNull ?: "MALE_1"
                TripMember(mId, mName, 0xFF2F80ED, true, lat, lng, speed, battery, avatar)
            } ?: emptyList()
            Trip(id, name, src, dst, dates, TripStatus.LIVE, members, coverImage = cover, vehicleType = vType, tripCode = tripCode)
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

    fun close() = client.close()
}

fun defaultHttpClient(): HttpClient = HttpClient {
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
        val lat = item["latitude"]?.jsonPrimitive?.doubleOrNull
        val lng = item["longitude"]?.jsonPrimitive?.doubleOrNull
        val speed = item["speedKmh"]?.jsonPrimitive?.doubleOrNull ?: item["speed"]?.jsonPrimitive?.doubleOrNull
        val battery = item["batteryPercent"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: item["battery"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        val avatar = item["avatarType"]?.jsonPrimitive?.contentOrNull ?: "MALE_1"
        TripMember(id, name, 0xFF2F80ED, true, lat, lng, speed, battery, avatar)
    } ?: request?.members?.map { TripMember(it, it, 0xFF2F80ED, true) } ?: emptyList()
    val fallbackDynamicId = request?.id ?: "trip-${(100000..999999).random()}"
    val tripId = json?.get("id")?.jsonPrimitive?.contentOrNull ?: expectedId.ifBlank { fallbackDynamicId }
    val tripCode = json?.get("tripCode")?.jsonPrimitive?.contentOrNull ?: expectedId.ifBlank { tripId }
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
        tripCode = tripCode
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
