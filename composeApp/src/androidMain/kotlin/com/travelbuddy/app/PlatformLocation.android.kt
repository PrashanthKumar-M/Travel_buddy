package com.travelbuddy.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.round

actual object PlatformLocation {
    var appContext: Context? = null
    private var lastLocation: RealLocation? = null
    private val activeListeners = CopyOnWriteArraySet<(RealLocation) -> Unit>()
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

    private fun hasLocationPermission(): Boolean {
        val ctx = appContext ?: return false
        val fine = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Updates device location directly (e.g., from Google Maps' native blue-dot location changes).
     */
    fun updateManualLocation(latitude: Double, longitude: Double, speedKmh: Double = 0.0) {
        if (latitude == 0.0 && longitude == 0.0) return
        val addr = reverseGeocode(latitude, longitude)
            ?: "Pinpoint Location (${round(latitude * 10000.0) / 10000.0}, ${round(longitude * 10000.0) / 10000.0})"
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
        if (!hasLocationPermission()) return
        try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsLoc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val passLoc = lm?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            val bestLoc = listOfNotNull(gpsLoc, netLoc, passLoc).maxByOrNull { it.time }
            if (bestLoc != null && bestLoc.latitude != 0.0) {
                lastLocation = toRealLocation(bestLoc)
            }

            val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && loc.latitude != 0.0) {
                    val realLoc = toRealLocation(loc)
                    lastLocation = realLoc
                    mainHandler.post {
                        activeListeners.forEach { it(realLoc) }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    actual fun getCurrentLocation(): RealLocation? {
        if (lastLocation == null) {
            fetchCachedLocation()
        }
        return lastLocation
    }

    /**
     * Enterprise Swiggy / Amazon style instant high-accuracy location latch:
     * 1. Evaluates latest cached hardware and network fixes.
     * 2. Concurrently dispatches single-shot high-accuracy FusedLocationProviderClient + hardware GPS listener.
     * 3. Latching timer (2500ms): resumes with the absolute freshest, most accurate fix available.
     */
    @SuppressLint("MissingPermission")
    actual suspend fun fetchFreshLocation(): RealLocation? = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext lastLocation
        if (!hasLocationPermission()) return@withContext lastLocation

        suspendCancellableCoroutine { cont ->
            val hasResumed = AtomicBoolean(false)
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)
            val cts = CancellationTokenSource()

            var oneShotListener: LocationListener? = null
            var oneShotCallback: LocationCallback? = null

            fun cleanup() {
                try { cts.cancel() } catch (_: Exception) {}
                try { oneShotCallback?.let { fusedClient.removeLocationUpdates(it) } } catch (_: Exception) {}
                try { oneShotListener?.let { lm?.removeUpdates(it) } } catch (_: Exception) {}
            }

            fun safeResume(loc: RealLocation?) {
                if (hasResumed.compareAndSet(false, true)) {
                    cleanup()
                    if (loc != null && loc.latitude != 0.0) {
                        lastLocation = loc
                        mainHandler.post { activeListeners.forEach { it(loc) } }
                    }
                    if (cont.isActive) cont.resume(loc ?: lastLocation)
                }
            }

            cont.invokeOnCancellation {
                cleanup()
            }

            // Check cached location candidates
            val gpsLoc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val passLoc = lm?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            val recentCandidate = listOfNotNull(gpsLoc, netLoc, passLoc)
                .filter { it.latitude != 0.0 }
                .maxByOrNull { it.time }

            // Latch timeout runnable (2500ms max wait)
            val timeoutRunnable = Runnable {
                val chosen = recentCandidate?.let { toRealLocation(it) } ?: lastLocation
                safeResume(chosen)
            }
            mainHandler.postDelayed(timeoutRunnable, 2500L)

            // 1. Google Play Services Fused Location Client (High Accuracy)
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null && loc.latitude != 0.0) {
                        mainHandler.removeCallbacks(timeoutRunnable)
                        safeResume(toRealLocation(loc))
                    }
                }
                .addOnFailureListener {
                    // Handled by active listener or timeout
                }

            // 2. Active single-update listener on FusedClient
            try {
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val loc = result.lastLocation ?: return
                        if (loc.latitude != 0.0) {
                            mainHandler.removeCallbacks(timeoutRunnable)
                            safeResume(toRealLocation(loc))
                        }
                    }
                }
                oneShotCallback = callback
                val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
                    .setMaxUpdates(1)
                    .setMinUpdateIntervalMillis(200L)
                    .build()
                fusedClient.requestLocationUpdates(req, callback, Looper.getMainLooper())
            } catch (_: Exception) {}

            // 3. Hardware GPS provider direct listener
            try {
                if (lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    val lnr = object : LocationListener {
                        override fun onLocationChanged(loc: Location) {
                            if (loc.latitude != 0.0) {
                                mainHandler.removeCallbacks(timeoutRunnable)
                                safeResume(toRealLocation(loc))
                            }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    oneShotListener = lnr
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, lnr, Looper.getMainLooper())
                }
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun startContinuousTracking() {
        val ctx = appContext ?: return
        if (!hasLocationPermission() || isTrackingStarted) return
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
                    if (loc.latitude != 0.0) {
                        val realLoc = toRealLocation(loc)
                        lastLocation = realLoc
                        mainHandler.post { activeListeners.forEach { it(realLoc) } }
                    }
                }
            }
            activeFusedCallback = callback
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: Exception) {}

        try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (location.latitude != 0.0) {
                        val realLoc = toRealLocation(location)
                        lastLocation = realLoc
                        mainHandler.post { activeListeners.forEach { it(realLoc) } }
                    }
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
        activeListeners.clear()
    }

    actual fun geocodeAddress(addressText: String): Pair<Double, Double>? {
        val ctx = appContext ?: return null
        val clean = addressText.trim()
        if (clean.isBlank()) return null

        // 1. Native Geocoder
        val nativeResult = try {
            val future = CompletableFuture.supplyAsync {
                try {
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
            future.get(2500, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            null
        }

        if (nativeResult != null && nativeResult.first != 0.0) return nativeResult

        // 2. HTTP Geocoding Fallback (OSM Nominatim)
        return try {
            val future = CompletableFuture.supplyAsync {
                try {
                    val encoded = URLEncoder.encode(clean, "UTF-8")
                    val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&countrycodes=in&format=json&limit=1")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "TravelBuddy-App/1.0")
                    conn.connectTimeout = 2500
                    conn.readTimeout = 2500
                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val latRegex = Regex(""""lat"\s*:\s*"([^"]+)"""")
                        val lonRegex = Regex(""""lon"\s*:\s*"([^"]+)"""")
                        val latMatch = latRegex.find(text)
                        val lonMatch = lonRegex.find(text)
                        val lat = latMatch?.groupValues?.get(1)?.toDoubleOrNull()
                        val lon = lonMatch?.groupValues?.get(1)?.toDoubleOrNull()
                        if (lat != null && lon != null) Pair(lat, lon) else null
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
            future.get(2500, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            null
        }
    }

    actual fun searchAddressSuggestions(query: String): List<PlaceSuggestion> {
        val ctx = appContext ?: return emptyList()
        val clean = query.trim()
        if (clean.length < 2) return emptyList()
        return try {
            val future = CompletableFuture.supplyAsync {
                try {
                    val geocoder = Geocoder(ctx, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val matches = geocoder.getFromLocationName(clean, 6)
                    if (!matches.isNullOrEmpty()) {
                        matches.map { addr ->
                            val full = addr.getAddressLine(0) ?: ""
                            val mainText = addr.featureName
                                ?: addr.thoroughfare
                                ?: full.split(",").firstOrNull()?.trim()
                                ?: clean
                            PlaceSuggestion(
                                name = mainText,
                                description = full.ifBlank { "$mainText, ${addr.locality ?: addr.adminArea ?: ""}" },
                                latitude = addr.latitude,
                                longitude = addr.longitude
                            )
                        }
                    } else emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
            }
            future.get(3000, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            emptyList()
        }
    }

    actual fun reverseGeocode(latitude: Double, longitude: Double): String? {
        val ctx = appContext ?: return null
        if (latitude == 0.0 && longitude == 0.0) return null

        // 1. Native Geocoder
        val nativeAddr = try {
            val future = CompletableFuture.supplyAsync {
                try {
                    val geocoder = Geocoder(ctx, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val matches = geocoder.getFromLocation(latitude, longitude, 1)
                    if (!matches.isNullOrEmpty()) {
                        val addr = matches[0]
                        val feature = addr.featureName
                        val thoroughfare = addr.thoroughfare
                        val subLocality = addr.subLocality
                        val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                        val fullAddress = addr.getAddressLine(0)

                        val street = when {
                            !thoroughfare.isNullOrBlank() && !subLocality.isNullOrBlank() && thoroughfare != subLocality -> "$thoroughfare, $subLocality"
                            !thoroughfare.isNullOrBlank() -> thoroughfare
                            !subLocality.isNullOrBlank() -> subLocality
                            !feature.isNullOrBlank() && !feature.matches(Regex("""\d+""")) -> feature
                            else -> null
                        }

                        when {
                            street != null && !locality.isNullOrBlank() -> "$street, $locality"
                            street != null -> street
                            !fullAddress.isNullOrBlank() -> fullAddress.split(",").take(3).joinToString(",").trim()
                            !locality.isNullOrBlank() -> locality
                            else -> fullAddress
                        }
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
            future.get(2500, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            null
        }

        if (!nativeAddr.isNullOrBlank()) return nativeAddr

        // 2. HTTP Reverse Geocoding Fallback (OSM Nominatim)
        val onlineAddr = try {
            val future = CompletableFuture.supplyAsync {
                try {
                    val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$latitude&lon=$longitude&format=json&accept-language=en")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "TravelBuddy-App/1.0")
                    conn.connectTimeout = 2500
                    conn.readTimeout = 2500
                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val regex = Regex(""""display_name"\s*:\s*"([^"]+)"""")
                        val match = regex.find(text)
                        val full = match?.groupValues?.get(1)?.replace("\\", "")
                        full?.split(",")?.take(3)?.joinToString(",")?.trim()
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
            future.get(2500, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            null
        }

        if (!onlineAddr.isNullOrBlank()) return onlineAddr

        // 3. Exact Pinpoint Coordinate fallback (Never hardcoded city names!)
        val latStr = (round(latitude * 10000.0) / 10000.0).toString()
        val lngStr = (round(longitude * 10000.0) / 10000.0).toString()
        return "Pinpoint Location ($latStr, $lngStr)"
    }

    private fun toRealLocation(loc: Location): RealLocation {
        val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6) else 0.0
        val address = reverseGeocode(loc.latitude, loc.longitude)
            ?: "Pinpoint Location (${round(loc.latitude * 10000.0) / 10000.0}, ${round(loc.longitude * 10000.0) / 10000.0})"
        return RealLocation(
            latitude = loc.latitude,
            longitude = loc.longitude,
            address = address,
            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else 0f,
            speedKmh = speedKmh
        )
    }
}
