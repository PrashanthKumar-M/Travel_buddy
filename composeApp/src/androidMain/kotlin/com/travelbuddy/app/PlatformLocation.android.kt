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
    @Volatile private var lastFixTimeMs: Long = 0L
    private const val EXCELLENT_ACCURACY_M = 35f
    private const val GOOD_ACCURACY_M = 100f
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
    fun updateManualLocation(
        latitude: Double,
        longitude: Double,
        speedKmh: Double = 0.0,
        accuracyMeters: Float = 25f
    ) {
        if (latitude == 0.0 && longitude == 0.0) return

        val now = System.currentTimeMillis()
        val prev = lastLocation
        if (prev != null && accuracyMeters > 80f && prev.accuracyMeters in 0.1f..EXCELLENT_ACCURACY_M && now - lastFixTimeMs < 20_000L) {
            return
        }

        val dist = haversineMeters(latitude, longitude, cachedAddressLat, cachedAddressLng)
        val addr = if (dist < 15.0 && !cachedAddress.isNullOrBlank()) {
            cachedAddress
        } else {
            triggerAsyncGeocode(latitude, longitude)
            null
        }

        val realLoc = RealLocation(
            latitude = latitude,
            longitude = longitude,
            address = addr,
            accuracyMeters = accuracyMeters,
            speedKmh = speedKmh
        )
        lastLocation = realLoc
        lastFixTimeMs = now
        mainHandler.post {
            activeListeners.forEach { it(realLoc) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCachedLocation() {
        val ctx = appContext ?: return
        if (!hasLocationPermission()) return
        try {
            val now = System.currentTimeMillis()
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsLoc = try { lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
            val netLoc = try { lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
            val passLoc = try { lm?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (_: Exception) { null }
            val candidates = listOfNotNull(gpsLoc, netLoc, passLoc).filter { it.latitude != 0.0 && it.longitude != 0.0 }
            // Prefer recent accurate fixes instead of coarse cell-tower fixes
            val bestLoc = candidates.minByOrNull { loc ->
                val agePenalty = ((now - loc.time).coerceAtLeast(0L) / 10_000L).toFloat() * 10f
                val acc = if (loc.hasAccuracy()) loc.accuracy else 200f
                acc + agePenalty
            }
            if (bestLoc != null) {
                val realLoc = toRealLocation(bestLoc)
                lastLocation = realLoc
                lastFixTimeMs = bestLoc.time.coerceAtMost(now)
            }

            val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && loc.latitude != 0.0) {
                    acceptTrackingFix(loc)
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

        // If continuous tracking already produced a very fresh, precise fix, use it instantly.
        val current = lastLocation
        if (current != null && current.latitude != 0.0 &&
            current.accuracyMeters in 0.1f..EXCELLENT_ACCURACY_M &&
            System.currentTimeMillis() - lastFixTimeMs < 5_000L
        ) {
            return@withContext current
        }

        suspendCancellableCoroutine { cont ->
            val hasResumed = AtomicBoolean(false)
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)
            val cts = CancellationTokenSource()

            var gpsListener: LocationListener? = null
            var fusedCallback: LocationCallback? = null
            // Best (most accurate) live fix received during this request
            var bestFix: Location? = null

            fun cleanup() {
                try { cts.cancel() } catch (_: Exception) {}
                try { fusedCallback?.let { fusedClient.removeLocationUpdates(it) } } catch (_: Exception) {}
                try { gpsListener?.let { lm?.removeUpdates(it) } } catch (_: Exception) {}
            }

            lateinit var softTimeout: Runnable
            lateinit var hardTimeout: Runnable

            fun safeResume(loc: RealLocation?) {
                if (hasResumed.compareAndSet(false, true)) {
                    mainHandler.removeCallbacks(softTimeout)
                    mainHandler.removeCallbacks(hardTimeout)
                    cleanup()
                    if (loc != null && loc.latitude != 0.0) {
                        lastLocation = loc
                        lastFixTimeMs = System.currentTimeMillis()
                        mainHandler.post { activeListeners.forEach { it(loc) } }
                    }
                    if (cont.isActive) cont.resume(loc ?: lastLocation)
                }
            }

            // Every live fix goes through here: resume instantly when the fix is precise
            // (like Swiggy/Amazon's "Use current location"), otherwise keep the best one.
            fun onFix(loc: Location?) {
                if (loc == null || loc.latitude == 0.0 || hasResumed.get()) return
                val acc = if (loc.hasAccuracy()) loc.accuracy else 999f
                val bestAcc = bestFix?.let { if (it.hasAccuracy()) it.accuracy else 999f } ?: Float.MAX_VALUE
                if (acc <= bestAcc) bestFix = loc
                if (acc <= EXCELLENT_ACCURACY_M) {
                    safeResume(toRealLocation(loc))
                }
            }

            cont.invokeOnCancellation { cleanup() }

            // Soft timeout (1.8s): accept a good fix (<= 100m) if we have one.
            softTimeout = Runnable {
                val best = bestFix
                if (best != null && (!best.hasAccuracy() || best.accuracy <= GOOD_ACCURACY_M)) {
                    safeResume(toRealLocation(best))
                }
            }
            // Hard timeout (3.5s): return the best live fix, or the most accurate cached fix.
            hardTimeout = Runnable {
                val best = bestFix
                if (best != null) {
                    safeResume(toRealLocation(best))
                } else {
                    val now = System.currentTimeMillis()
                    val recentCached = listOfNotNull(
                        try { lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null },
                        try { lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null },
                        try { lm?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (_: Exception) { null }
                    ).filter { it.latitude != 0.0 && now - it.time < 300_000L }
                        .minByOrNull { if (it.hasAccuracy()) it.accuracy else 999f }
                    safeResume(recentCached?.let { toRealLocation(it) } ?: lastLocation)
                }
            }
            mainHandler.postDelayed(softTimeout, 1_800L)
            mainHandler.postDelayed(hardTimeout, 3_500L)

            // 1. Fused single high-accuracy fix
            try {
                val currentReq = com.google.android.gms.location.CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMaxUpdateAgeMillis(2_000L)
                    .setDurationMillis(3_500L)
                    .build()
                fusedClient.getCurrentLocation(currentReq, cts.token)
                    .addOnSuccessListener { loc -> onFix(loc) }
            } catch (_: Exception) {}

            // 2. Continuous fused stream during the request window (do not wait indoors)
            try {
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.locations.forEach { onFix(it) }
                    }
                }
                fusedCallback = callback
                val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 400L)
                    .setMinUpdateIntervalMillis(200L)
                    .setMinUpdateDistanceMeters(0f)
                    .setWaitForAccurateLocation(false)
                    .build()
                fusedClient.requestLocationUpdates(req, callback, Looper.getMainLooper())
            } catch (_: Exception) {}

            // 3. Hardware GPS + Network listeners (with 0m distance so stationary devices emit immediately)
            try {
                if (lm != null) {
                    val lnr = object : LocationListener {
                        override fun onLocationChanged(loc: Location) { onFix(loc) }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    gpsListener = lnr
                    if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400L, 0f, lnr, Looper.getMainLooper())
                    }
                    if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 400L, 0f, lnr, Looper.getMainLooper())
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Accepts a continuous-tracking fix unless it is a coarse (cell/wifi) fix that would make
     * the user's marker jump away from a recent precise GPS position.
     */
    private fun acceptTrackingFix(loc: Location) {
        if (loc.latitude == 0.0) return
        val acc = if (loc.hasAccuracy()) loc.accuracy else 999f
        val prev = lastLocation
        val now = System.currentTimeMillis()
        if (prev != null && acc > 100f && prev.accuracyMeters in 0.1f..GOOD_ACCURACY_M && now - lastFixTimeMs < 30_000L) {
            return
        }
        val realLoc = toRealLocation(loc)
        lastLocation = realLoc
        lastFixTimeMs = now
        mainHandler.post { activeListeners.forEach { it(realLoc) } }
    }

    @SuppressLint("MissingPermission")
    fun startContinuousTracking() {
        val ctx = appContext ?: return
        if (!hasLocationPermission()) return
        if (lastLocation == null) {
            fetchCachedLocation()
        }
        if (isTrackingStarted) return
        isTrackingStarted = true

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L).apply {
                setMinUpdateIntervalMillis(800L)
                setMinUpdateDistanceMeters(0f)
                setWaitForAccurateLocation(false)
            }.build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    acceptTrackingFix(loc)
                }
            }
            activeFusedCallback = callback
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: Exception) {}

        try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    acceptTrackingFix(location)
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
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 0f, listener, Looper.getMainLooper())
            }
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    actual fun startLocationUpdates(onUpdate: (RealLocation) -> Unit) {
        activeListeners.add(onUpdate)
        lastLocation?.let { onUpdate(it) }
        startContinuousTracking()
    }

    /** Removes only this listener (other screens keep receiving live updates). */
    actual fun removeLocationListener(onUpdate: (RealLocation) -> Unit) {
        activeListeners.remove(onUpdate)
    }

    actual fun stopLocationUpdates() {
        activeListeners.clear()
    }

    actual fun geocodeAddress(addressText: String): Pair<Double, Double>? {
        val ctx = appContext ?: return null
        val clean = addressText.trim()
        if (clean.isBlank()) return null
        // Never block the Android UI thread with synchronous network/binder geocoding
        if (Looper.myLooper() == Looper.getMainLooper()) return null

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
            future.get(2500L, TimeUnit.MILLISECONDS)
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
        if (dist < 15.0 && !cachedAddress.isNullOrBlank()) {
            return cachedAddress
        }

        // If called on main thread, do NOT block the UI looper!
        // Never return a stale address cached for a different (nearby) position.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            triggerAsyncGeocode(latitude, longitude)
            return "Pinpoint Location (${round(latitude * 100000.0) / 100000.0}, ${round(longitude * 100000.0) / 100000.0})"
        }

        val resolved = doBackgroundReverseGeocode(latitude, longitude)
        if (!resolved.isNullOrBlank() && !resolved.startsWith("Pinpoint")) {
            cachedAddress = resolved
            cachedAddressLat = latitude
            cachedAddressLng = longitude
        }
        return resolved
    }

    private fun doBackgroundReverseGeocode(latitude: Double, longitude: Double): String? {
        val ctx = appContext ?: return null
        if (latitude == 0.0 && longitude == 0.0) return null

        var nativeCandidate: String? = null

        // 1. Native Geocoder: inspect up to 5 candidates and pick the most specific street/premise address
        try {
            val geocoder = Geocoder(ctx, Locale.getDefault())
            @Suppress("DEPRECATION")
            val matches = geocoder.getFromLocation(latitude, longitude, 5)
            if (!matches.isNullOrEmpty()) {
                val bestAddr = matches.maxByOrNull { candidate ->
                    var score = 0
                    if (!candidate.subThoroughfare.isNullOrBlank()) score += 4
                    if (!candidate.thoroughfare.isNullOrBlank()) score += 4
                    if (!candidate.featureName.isNullOrBlank() && !candidate.featureName.contains("+")) score += 2
                    if (!candidate.subLocality.isNullOrBlank()) score += 3
                    if (!candidate.locality.isNullOrBlank()) score += 1
                    score
                } ?: matches[0]

                val fullAddress = bestAddr.getAddressLine(0)
                if (!fullAddress.isNullOrBlank()) {
                    val parts = fullAddress.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                    val country = bestAddr.countryName
                    if (parts.size > 2 && !country.isNullOrBlank() && parts.last().equals(country, ignoreCase = true)) {
                        parts.removeAt(parts.lastIndex)
                    }
                    // Strip a leading Plus Code (e.g. "7J4V+2C" or "XMPW+89") which is not human readable
                    if (parts.size > 1 && parts.first().contains("+")) {
                        parts.removeAt(0)
                    }
                    val exact = parts.take(5).joinToString(", ")
                    if (exact.isNotBlank()) {
                        val hasStreetDetail = !bestAddr.thoroughfare.isNullOrBlank() ||
                            !bestAddr.subThoroughfare.isNullOrBlank() ||
                            parts.size >= 4
                        if (hasStreetDetail) return exact
                        nativeCandidate = exact
                    }
                }
                if (nativeCandidate == null) {
                    val thoroughfare = bestAddr.thoroughfare
                    val subLocality = bestAddr.subLocality
                    val locality = bestAddr.locality ?: bestAddr.subAdminArea ?: bestAddr.adminArea
                    val composed = listOfNotNull(
                        bestAddr.subThoroughfare, thoroughfare, subLocality, locality
                    ).filter { it.isNotBlank() }.distinct().joinToString(", ")
                    if (composed.isNotBlank()) {
                        if (!thoroughfare.isNullOrBlank()) return composed
                        nativeCandidate = composed
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. HTTP Reverse Geocoding Fallback (OSM Nominatim zoom=18 for exact building/layout/street)
        try {
            val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$latitude&lon=$longitude&format=json&zoom=18&addressdetails=0&accept-language=en")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "TravelBuddy-App/1.0")
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val regex = Regex(""""display_name"\s*:\s*"([^"]+)"""")
                val match = regex.find(text)
                val full = match?.groupValues?.get(1)?.replace("\\", "")
                val formatted = full?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.take(5)?.joinToString(", ")
                if (!formatted.isNullOrBlank()) return formatted
            }
        } catch (_: Exception) {}

        if (!nativeCandidate.isNullOrBlank()) return nativeCandidate

        return "Pinpoint Location (${round(latitude * 10000.0) / 10000.0}, ${round(longitude * 10000.0) / 10000.0})"
    }

    /**
     * Converts Location to RealLocation in 0ms (non-blocking).
     */
    private fun toRealLocation(loc: Location): RealLocation {
        val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6) else 0.0
        val dist = haversineMeters(loc.latitude, loc.longitude, cachedAddressLat, cachedAddressLng)
        val address = if (dist < 15.0 && !cachedAddress.isNullOrBlank()) {
            cachedAddress
        } else {
            triggerAsyncGeocode(loc.latitude, loc.longitude)
            null
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
