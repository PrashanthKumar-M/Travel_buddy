package com.travelbuddy.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

actual object PlatformLocation {
    var appContext: Context? = null
    private var lastLocation: RealLocation? = null
    private var activeListener: LocationListener? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        appContext = context.applicationContext
        // Seed initial cached GPS location
        fetchCachedLocation()
    }

    @SuppressLint("MissingPermission")
    private fun fetchCachedLocation() {
        val ctx = appContext ?: return
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    lastLocation = toRealLocation(loc)
                }
            }
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLoc = when {
                gpsLoc != null && netLoc != null -> if (gpsLoc.time >= netLoc.time) gpsLoc else netLoc
                gpsLoc != null -> gpsLoc
                netLoc != null -> netLoc
                else -> null
            }
            if (bestLoc != null && lastLocation == null) {
                lastLocation = toRealLocation(bestLoc)
            }
        } catch (_: Exception) {
        }
    }

    actual fun getCurrentLocation(): RealLocation? {
        if (lastLocation == null) {
            fetchCachedLocation()
        }
        return lastLocation
    }

    @SuppressLint("MissingPermission")
    actual suspend fun fetchFreshLocation(): RealLocation? = suspendCancellableCoroutine { cont ->
        val ctx = appContext
        if (ctx == null) {
            cont.resume(lastLocation)
            return@suspendCancellableCoroutine
        }
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        val real = toRealLocation(loc)
                        lastLocation = real
                        if (cont.isActive) cont.resume(real)
                    } else {
                        // Fallback to fusedClient lastLocation
                        fusedClient.lastLocation.addOnSuccessListener { fallbackLoc ->
                            if (fallbackLoc != null) {
                                val real = toRealLocation(fallbackLoc)
                                lastLocation = real
                                if (cont.isActive) cont.resume(real)
                            } else {
                                if (cont.isActive) cont.resume(lastLocation)
                            }
                        }.addOnFailureListener {
                            if (cont.isActive) cont.resume(lastLocation)
                        }
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(lastLocation)
                }
        } catch (_: Exception) {
            if (cont.isActive) cont.resume(lastLocation)
        }
    }

    @SuppressLint("MissingPermission")
    actual fun startLocationUpdates(onUpdate: (RealLocation) -> Unit) {
        val ctx = appContext ?: return
        try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            stopLocationUpdates()

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val realLoc = toRealLocation(location)
                    lastLocation = realLoc
                    mainHandler.post { onUpdate(realLoc) }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            activeListener = listener

            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 5f, listener, Looper.getMainLooper())
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 5f, listener, Looper.getMainLooper())
            }
        } catch (_: Exception) {
        }
    }

    actual fun stopLocationUpdates() {
        val ctx = appContext ?: return
        try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            activeListener?.let { lm.removeUpdates(it) }
            activeListener = null
        } catch (_: Exception) {
        }
    }

    actual fun geocodeAddress(addressText: String): Pair<Double, Double>? {
        val ctx = appContext ?: return null
        val clean = addressText.trim()
        if (clean.isBlank()) return null
        return try {
            val geocoder = Geocoder(ctx, Locale.getDefault())
            @Suppress("DEPRECATION")
            val matches = geocoder.getFromLocationName(clean, 1)
            if (!matches.isNullOrEmpty()) {
                val match = matches[0]
                Pair(match.latitude, match.longitude)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    actual fun reverseGeocode(latitude: Double, longitude: Double): String? {
        val ctx = appContext ?: return null
        return try {
            val geocoder = Geocoder(ctx, Locale.getDefault())
            @Suppress("DEPRECATION")
            val matches = geocoder.getFromLocation(latitude, longitude, 1)
            if (!matches.isNullOrEmpty()) {
                val addr = matches[0]
                val subLocality = addr.subLocality ?: addr.thoroughfare ?: addr.featureName
                val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                val fullAddress = addr.getAddressLine(0)

                when {
                    !subLocality.isNullOrBlank() && !locality.isNullOrBlank() -> "$subLocality, $locality"
                    !fullAddress.isNullOrBlank() -> fullAddress.split(",").take(2).joinToString(",").trim()
                    !locality.isNullOrBlank() -> locality
                    !subLocality.isNullOrBlank() -> subLocality
                    else -> fullAddress
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun toRealLocation(loc: Location): RealLocation {
        val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6) else 0.0
        val address = reverseGeocode(loc.latitude, loc.longitude)
        return RealLocation(
            latitude = loc.latitude,
            longitude = loc.longitude,
            address = address,
            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else 0f,
            speedKmh = speedKmh
        )
    }
}
