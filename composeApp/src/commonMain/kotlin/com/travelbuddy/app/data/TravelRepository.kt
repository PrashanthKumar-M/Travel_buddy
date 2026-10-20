package com.travelbuddy.app.data

import com.travelbuddy.app.domain.Expense
import com.travelbuddy.app.domain.Settlement
import com.travelbuddy.app.domain.Trip
import kotlinx.serialization.Serializable

/** REST routes from the supplied Spring Boot Postman collection. */
data class TravelApiConfig(
    val baseUrl: String,
    /** Set only after the backend publishes its WebSocket endpoint and topic contract. */
    val webSocketUrl: String? = null
) {
    private val api get() = "${baseUrl.trimEnd('/')}/api"
    fun requestOtpEndpoint() = "$api/auth/request-otp"
    fun verifyOtpEndpoint() = "$api/auth/verify-otp"
    fun createTripEndpoint() = "$api/trips"
    fun liveSnapshotEndpoint(tripId: String) = "$api/trips/$tripId/live"
    fun updateLocationEndpoint() = "$api/locations"
    fun addExpenseEndpoint() = "$api/expenses"
    fun tripsEndpoint() = "$api/trips"
    fun expensesEndpoint(tripId: String) = "$api/expenses?tripId=$tripId"
    fun chatMessagesEndpoint(tripId: String) = "$api/chat/$tripId/messages"
    fun sendChatMessageEndpoint(tripId: String) = "$api/chat/$tripId/messages"
    fun settlementsEndpoint(tripId: String) = "$api/expenses/trips/$tripId/settlements"
    fun devicesEndpoint() = "$api/devices"
}

@Serializable data class RequestOtpRequest(val phone: String? = null, val email: String? = null, val type: String = "MOBILE")
@Serializable data class OtpVerificationRequest(val phone: String? = null, val email: String? = null, val otp: String)
@Serializable data class CreateTripRequest(
    val id: String? = null,
    val tripName: String,
    val source: String,
    val destination: String,
    val dates: String = "",
    val createdBy: String = "usr-1",
    val coverImage: String? = null,
    val members: List<String> = emptyList()
)
@Serializable data class LocationUpdateRequest(
    val tripId: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val battery: Int,
    val timestamp: Long
)
@Serializable data class AddExpenseRequest(
    val tripId: String,
    val amount: Double,
    val paidBy: String,
    val splitType: String = "equal",
    val members: List<String>,
    val title: String = "Expense"
)
@Serializable data class RemoteChatMessage(
    val id: String = "",
    val tripId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)
@Serializable data class RemoteSendMessageRequest(
    val senderId: String,
    val senderName: String,
    val message: String
)

interface TravelRepository {
    suspend fun requestOtp(phone: String? = null, email: String? = null): Result<String?>
    suspend fun verifyOtp(request: OtpVerificationRequest): Result<Unit>
    suspend fun getTrips(): Result<List<Trip>>
    suspend fun createTrip(request: CreateTripRequest): Result<Trip>
    suspend fun liveSnapshot(tripId: String): Result<Trip>
    suspend fun updateLocation(request: LocationUpdateRequest): Result<Unit>
    suspend fun getExpenses(tripId: String): Result<List<Expense>>
    suspend fun addExpense(request: AddExpenseRequest): Result<Expense>
    suspend fun settlements(tripId: String): Result<List<Settlement>>
    suspend fun getChatMessages(tripId: String): Result<List<RemoteChatMessage>>
    suspend fun sendChatMessage(tripId: String, request: RemoteSendMessageRequest): Result<RemoteChatMessage>
}

data class VehicleApiConfig(val baseUrl: String, val tenantId: String, val bearerToken: String? = null) {
    fun commandEndpoint() = "$baseUrl/$tenantId/api/addDeviceCommand"
    fun statusEndpoint() = "$baseUrl/$tenantId/api/deviceCmdStatus"
    fun statusByRequestEndpoint() = "$baseUrl/$tenantId/api/deviceCmdStatusByRequestId"
    fun vinImeiEndpoint() = "$baseUrl/commands/addVinImeiMapping"
}

data class VehicleCommand(val vin: String, val command: String, val commandType: String, val randomNumber: String)

interface VehicleCommandRepository {
    suspend fun sendCommand(command: VehicleCommand): Result<Long>
    suspend fun commandStatus(requestId: Long): Result<String>
    suspend fun registerVinImei(vin: String, imei: String): Result<Unit>
}
