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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

actual object PlatformLocation {
    var appContext: Context? = null
    private var lastLocation: RealLocation? = null
    private val activeListeners = CopyOnWriteArraySet<(RealLocation) -> Unit>()
    private var activeListener: LocationListener? = null
    private var activeFusedCallback: LocationCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isTrackingStarted = false

    // Asynchronous background worker scope (never blocks Android main thread / UI looper)
    private val bgScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // In-memory address cache to eliminate redundant geocoding during continuous tracking
    private var cachedAddress: String? = null
    private var cachedAddressLat: Double = 0.0
    private var cachedAddressLng: Double = 0.0
    private val isGeocodingInProgress = AtomicBoolean(false)

    fun init(context: Context) {
        appContext = context.applicationContext
        fetchCachedLocation()
        startContinuousTracking()
    }

    private fun hasLocationPermission(): Boolean {
        val ctx = appContext ?: return false
        val fine = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1 == 0.0 || lon1 == 0.0 || lat2 == 0.0 || lon2 == 0.0) return Double.MAX_VALUE
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return 6371000.0 * c
    }

    private fun triggerAsyncGeocode(lat: Double, lng: Double) {
        if (lat == 0.0 && lng == 0.0) return
        if (isGeocodingInProgress.compareAndSet(false, true)) {
            bgScope.launch {
                try {
                    val resolved = doBackgroundReverseGeocode(lat, lng)
                    if (!resolved.isNullOrBlank()) {
                        cachedAddress = resolved
                        cachedAddressLat = lat
                        cachedAddressLng = lng
                        val current = lastLocation
                        if (current != null) {
                            val updated = current.copy(address = resolved)
                            lastLocation = updated
                            mainHandler.post {
                                activeListeners.forEach { it(updated) }
                            }
                        }
                    }
                } finally {
                    isGeocodingInProgress.set(false)
                }
            }
        }
    }

    /**
     * Updates device location directly (e.g. from Google Maps' native blue-dot location changes).
     * 100% non-blocking: never blocks the UI thread.
     */
    fun updateManualLocation(latitude: Double, longitude: Double, speedKmh: Double = 0.0) {
        if (latitude == 0.0 && longitude == 0.0) return

        val dist = haversineMeters(latitude, longitude, cachedAddressLat, cachedAddressLng)
        val addr = if (dist < 150.0 && !cachedAddress.isNullOrBlank()) {
            cachedAddress
        } else {
            triggerAsyncGeocode(latitude, longitude)
            lastLocation?.address ?: "Pinpoint Location (${round(latitude * 10000.0) / 10000.0}, ${round(longitude * 10000.0) / 10000.0})"
        }

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
     * 3. Latching timer (2000ms): resumes with the absolute freshest, most accurate fix available.
     * Never blocks UI thread.
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

            // Latch timeout runnable (2000ms max wait)
            val timeoutRunnable = Runnable {
                val chosen = recentCandidate?.let { toRealLocation(it) } ?: lastLocation
                safeResume(chosen)
            }
            mainHandler.postDelayed(timeoutRunnable, 2000L)

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
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).apply {
                setMinUpdateIntervalMillis(1500L)
                setMinUpdateDistanceMeters(2f)
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
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 2f, listener, Looper.getMainLooper())
            }
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    actual fun startLocationUpdates(onUpdate: (RealLocation) -> Unit) {
        activeListeners.add(onUpdate)
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
        return try {
            val future = CompletableFuture.supplyAsync {
                try {
                    val geocoder = Geocoder(ctx, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val matches = geocoder.getFromLocationName(clean, 1)
                    if (!matches.isNullOrEmpty()) {
                        val match = matches[0]
                        Pair(match.latitude, match.longitude)
                    } else {
                        // HTTP fallback on background thread
                        val encoded = URLEncoder.encode(clean, "UTF-8")
                        val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&countrycodes=in&format=json&limit=1")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.setRequestProperty("User-Agent", "TravelBuddy-App/1.0")
                        conn.connectTimeout = 2000
                        conn.readTimeout = 2000
                        if (conn.responseCode == 200) {
                            val text = conn.inputStream.bufferedReader().use { it.readText() }
                            val latRegex = Regex(""""lat"\s*:\s*"([^"]+)"""")
                            val lonRegex = Regex(""""lon"\s*:\s*"([^"]+)"""")
                            val lat = latRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
                            val lon = lonRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
                            if (lat != null && lon != null) Pair(lat, lon) else null
                        } else null
                    }
                } catch (_: Exception) {
                    null
                }
            }
            // If on main thread, do not block the UI (max 100ms), background callers can wait up to 2500ms
            val timeout = if (Looper.myLooper() == Looper.getMainLooper()) 100L else 2500L
            future.get(timeout, TimeUnit.MILLISECONDS)
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
            val timeout = if (Looper.myLooper() == Looper.getMainLooper()) 100L else 3000L
            future.get(timeout, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Non-blocking reverse geocoding:
     * - Returns cached address immediately if near the requested coordinates.
     * - If called on main thread: NEVER blocks. Returns cached address or formatted coordinate, and triggers async resolution.
     * - If called on background thread: executes geocoding.
     */
    actual fun reverseGeocode(latitude: Double, longitude: Double): String? {
        if (latitude == 0.0 && longitude == 0.0) return null

        val dist = haversineMeters(latitude, longitude, cachedAddressLat, cachedAddressLng)
        if (dist < 150.0 && !cachedAddress.isNullOrBlank()) {
            return cachedAddress
        }

        // If called on main thread, do NOT block the UI looper!
        if (Looper.myLooper() == Looper.getMainLooper()) {
            triggerAsyncGeocode(latitude, longitude)
            return cachedAddress ?: "Pinpoint Location (${round(latitude * 10000.0) / 10000.0}, ${round(longitude * 10000.0) / 10000.0})"
        }

        return doBackgroundReverseGeocode(latitude, longitude)
    }

    private fun doBackgroundReverseGeocode(latitude: Double, longitude: Double): String? {
        val ctx = appContext ?: return null
        if (latitude == 0.0 && longitude == 0.0) return null

        // 1. Native Geocoder
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

                val resolved = when {
                    street != null && !locality.isNullOrBlank() -> "$street, $locality"
                    street != null -> street
                    !fullAddress.isNullOrBlank() -> fullAddress.split(",").take(3).joinToString(",").trim()
                    !locality.isNullOrBlank() -> locality
                    else -> fullAddress
                }
                if (!resolved.isNullOrBlank()) return resolved
            }
        } catch (_: Exception) {}

        // 2. HTTP Reverse Geocoding Fallback (OSM Nominatim)
        try {
            val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$latitude&lon=$longitude&format=json&accept-language=en")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "TravelBuddy-App/1.0")
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val regex = Regex(""""display_name"\s*:\s*"([^"]+)"""")
                val match = regex.find(text)
                val full = match?.groupValues?.get(1)?.replace("\\", "")
                val formatted = full?.split(",")?.take(3)?.joinToString(",")?.trim()
                if (!formatted.isNullOrBlank()) return formatted
            }
        } catch (_: Exception) {}

        return "Pinpoint Location (${round(latitude * 10000.0) / 10000.0}, ${round(longitude * 10000.0) / 10000.0})"
    }

    /**
     * Converts Location to RealLocation in 0ms (non-blocking).
     */
    private fun toRealLocation(loc: Location): RealLocation {
        val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6) else 0.0
        val dist = haversineMeters(loc.latitude, loc.longitude, cachedAddressLat, cachedAddressLng)
        val address = if (dist < 150.0 && !cachedAddress.isNullOrBlank()) {
            cachedAddress
        } else {
            triggerAsyncGeocode(loc.latitude, loc.longitude)
            lastLocation?.address ?: "Pinpoint Location (${round(loc.latitude * 10000.0) / 10000.0}, ${round(loc.longitude * 10000.0) / 10000.0})"
        }

        return RealLocation(
            latitude = loc.latitude,
            longitude = loc.longitude,
            address = address,
            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else 0f,
            speedKmh = speedKmh
        )
    }
}
