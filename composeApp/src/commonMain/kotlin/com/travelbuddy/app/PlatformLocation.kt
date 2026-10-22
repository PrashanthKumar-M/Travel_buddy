package com.travelbuddy.app

data class RealLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val accuracyMeters: Float = 0f,
    val speedKmh: Double = 0.0
)

expect object PlatformLocation {
    fun getCurrentLocation(): RealLocation?
    suspend fun fetchFreshLocation(): RealLocation?
    fun startLocationUpdates(onUpdate: (RealLocation) -> Unit)
    fun stopLocationUpdates()
    fun geocodeAddress(addressText: String): Pair<Double, Double>?
    fun reverseGeocode(latitude: Double, longitude: Double): String?
}
