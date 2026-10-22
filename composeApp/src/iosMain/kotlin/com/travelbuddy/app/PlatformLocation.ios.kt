package com.travelbuddy.app

actual object PlatformLocation {
    actual fun getCurrentLocation(): RealLocation? = null
    actual fun startLocationUpdates(onUpdate: (RealLocation) -> Unit) {}
    actual fun stopLocationUpdates() {}
    actual fun geocodeAddress(addressText: String): Pair<Double, Double>? = null
    actual fun reverseGeocode(latitude: Double, longitude: Double): String? = null
}
