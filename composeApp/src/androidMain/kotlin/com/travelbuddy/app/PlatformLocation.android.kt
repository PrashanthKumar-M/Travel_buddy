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
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

actual object PlatformLocation {
    var appContext: Context? = null
    private var lastLocation: RealLocation? = null
    private val activeListeners = java.util.concurrent.CopyOnWriteArraySet<(RealLocation) -> Unit>()
    private var activeListener: LocationListener? = null
    private var activeFusedCallback: LocationCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isTrackingStarted = false

    fun init(context: Context) {
        appContext = context.applicationContext
        // Seed initial cached GPS location
        fetchCachedLocation()
        startContinuousTracking()
    }

    /**
     * Updates device location directly (e.g., from Google Maps' native blue-dot location changes).
     */
    fun updateManualLocation(latitude: Double, longitude: Double, speedKmh: Double = 0.0) {
        if (latitude == 0.0 && longitude == 0.0) return
        val addr = reverseGeocode(latitude, longitude)
            ?: LocationCoordinateResolver.resolveNearestCityOrArea(latitude, longitude)
        val realLoc = RealLocation(
            latitude = latitude,
            longitude = longitude,
            address = addr,
            accuracyMeters = 5f,
            speedKmh = speedKmh
        )
        lastLocation = realLoc
        mainHandler.post {
            activeListeners.forEach { it(realLoc) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCachedLocation() {
        val ctx = appContext ?: return
        try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsLoc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val passLoc = lm?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            val bestLoc = listOfNotNull(gpsLoc, netLoc, passLoc).maxByOrNull { it.time }
            if (bestLoc != null) {
                lastLocation = toRealLocation(bestLoc)
            }

            val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val realLoc = toRealLocation(loc)
                    lastLocation = realLoc
                    mainHandler.post {
                        activeListeners.forEach { it(realLoc) }
                    }
                }
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
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsLoc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val passLoc = lm?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            val bestCached = listOfNotNull(gpsLoc, netLoc, passLoc).maxByOrNull { it.time }
            if (bestCached != null && lastLocation == null) {
                lastLocation = toRealLocation(bestCached)
            }

            val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        val real = toRealLocation(loc)
                        lastLocation = real
                        mainHandler.post { activeListeners.forEach { it(real) } }
                        if (cont.isActive) cont.resume(real)
                    } else {
                        // Fallback to fusedClient lastLocation
                        fusedClient.lastLocation.addOnSuccessListener { fallbackLoc ->
                            val chosen = fallbackLoc ?: bestCached
                            if (chosen != null) {
                                val real = toRealLocation(chosen)
                                lastLocation = real
                                mainHandler.post { activeListeners.forEach { it(real) } }
                                if (cont.isActive) cont.resume(real)
                            } else {
                                if (cont.isActive) cont.resume(lastLocation)
                            }
                        }.addOnFailureListener {
                            val real = bestCached?.let { toRealLocation(it) } ?: lastLocation
                            if (cont.isActive) cont.resume(real)
                        }
                    }
                }
                .addOnFailureListener {
                    val real = bestCached?.let { toRealLocation(it) } ?: lastLocation
                    if (cont.isActive) cont.resume(real)
                }
        } catch (_: Exception) {
            if (cont.isActive) cont.resume(lastLocation)
        }
    }

    @SuppressLint("MissingPermission")
    fun startContinuousTracking() {
        val ctx = appContext ?: return
        if (isTrackingStarted) return
        isTrackingStarted = true

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L).apply {
                setMinUpdateIntervalMillis(1000L)
                setMinUpdateDistanceMeters(0f)
            }.build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    val realLoc = toRealLocation(loc)
                    lastLocation = realLoc
                    mainHandler.post { activeListeners.forEach { it(realLoc) } }
                }
            }
            activeFusedCallback = callback
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: Exception) {}

        try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val realLoc = toRealLocation(location)
                    lastLocation = realLoc
                    mainHandler.post { activeListeners.forEach { it(realLoc) } }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            activeListener = listener

            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500L, 0f, listener, Looper.getMainLooper())
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1500L, 0f, listener, Looper.getMainLooper())
            }
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    actual fun startLocationUpdates(onUpdate: (RealLocation) -> Unit) {
        activeListeners.add(onUpdate)
        // Immediate callback with last cached location if already known
        lastLocation?.let { onUpdate(it) }
        startContinuousTracking()
    }

    actual fun stopLocationUpdates() {
        // Keep continuous tracking alive so location remains available, but clear specific disposable callbacks
        activeListeners.clear()
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
            val future = java.util.concurrent.CompletableFuture.supplyAsync {
                try {
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
            future.get(1500, java.util.concurrent.TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            null
        }
    }

    private fun toRealLocation(loc: Location): RealLocation {
        val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6) else 0.0
        val address = reverseGeocode(loc.latitude, loc.longitude)
            ?: LocationCoordinateResolver.resolveNearestCityOrArea(loc.latitude, loc.longitude)
        return RealLocation(
            latitude = loc.latitude,
            longitude = loc.longitude,
            address = address,
            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else 0f,
            speedKmh = speedKmh
        )
    }
}
