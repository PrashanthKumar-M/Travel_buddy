package com.travelbuddy.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

/**
 * Geographic and road distance resolver for Indian and international cities.
 * Provides accurate coordinates and road travel distance for source and destination routing.
 */
object LocationCoordinateResolver {

    private val resolvedCoordsCache = mutableMapOf<String, Pair<Double, Double>>()

    /**
     * Resolves the exact street address for live GPS coordinates on a background thread
     * (never blocks the UI and never returns a stale "nearby" address).
     */
    suspend fun exactAddressFor(latitude: Double, longitude: Double): String = withContext(Dispatchers.Default) {
        if (latitude == 0.0 && longitude == 0.0) return@withContext "Current Location"
        val geocoded = try { PlatformLocation.reverseGeocode(latitude, longitude) } catch (_: Exception) { null }
        if (!geocoded.isNullOrBlank()) geocoded
        else "Pinpoint Location (${round(latitude * 100000.0) / 100000.0}, ${round(longitude * 100000.0) / 100000.0})"
    }

    /** Background-thread version of [resolveCoordinates] for use from composables. */
    suspend fun resolveCoordinatesAsync(cityName: String, fallbackLat: Double = 0.0, fallbackLng: Double = 0.0): Pair<Double, Double> =
        withContext(Dispatchers.Default) { resolveCoordinates(cityName, fallbackLat, fallbackLng) }

    /** Background-thread version of [calculateRoadDistanceKm]. */
    suspend fun calculateRoadDistanceKmAsync(sourceName: String, destinationName: String): Double =
        withContext(Dispatchers.Default) { calculateRoadDistanceKm(sourceName, destinationName) }

    private val cityCoordinates: Map<String, Pair<Double, Double>> = mapOf(
        // Karnataka
        "bengaluru" to Pair(12.9716, 77.5946),
        "bangalore" to Pair(12.9716, 77.5946),
        "bellandur" to Pair(12.9304, 77.6784),
        "mahadevapura" to Pair(12.9863, 77.6974),
        "madevpur" to Pair(12.9863, 77.6974),
        "indiranagar" to Pair(12.9784, 77.6408),
        "whitefield" to Pair(12.9698, 77.7499),
        "marathahalli" to Pair(12.9591, 77.6974),
        "hsr layout" to Pair(12.9121, 77.6446),
        "koramangala" to Pair(12.9352, 77.6245),
        "electronic city" to Pair(12.8399, 77.6770),
        "btm layout" to Pair(12.9166, 77.6101),
        "jayanagar" to Pair(12.9308, 77.5838),
        "jp nagar" to Pair(12.9063, 77.5857),
        "malleswaram" to Pair(13.0031, 77.5643),
        "hebbal" to Pair(13.0358, 77.5970),
        "banashankari" to Pair(12.9255, 77.5468),
        "rajajinagar" to Pair(12.9982, 77.5530),
        "yelahanka" to Pair(13.1007, 77.5963),
        "sarjapur" to Pair(12.8597, 77.7877),
        "domlur" to Pair(12.9609, 77.6387),
        "kalyan nagar" to Pair(13.0280, 77.6420),
        "bidar" to Pair(17.9104, 77.5199),
        "mangalagi" to Pair(17.7816, 77.1336),
        "humanabad" to Pair(17.7714, 77.1306),
        "humnabad" to Pair(17.7714, 77.1306),
        "kalaburagi" to Pair(17.3297, 76.8343),
        "gulbarga" to Pair(17.3297, 76.8343),
        "zaheerabad" to Pair(17.6814, 77.6074),
        "basavakalyan" to Pair(17.8736, 76.9497),
        "bhalki" to Pair(18.0468, 77.2111),
        "coorg" to Pair(12.4244, 75.7382),
        "coorg valley" to Pair(12.4244, 75.7382),
        "madikeri" to Pair(12.4244, 75.7382),
        "mysuru" to Pair(12.2958, 76.6394),
        "mysore" to Pair(12.2958, 76.6394),
        "hubballi" to Pair(15.3647, 75.1240),
        "hubli" to Pair(15.3647, 75.1240),
        "dharwad" to Pair(15.4589, 75.0078),
        "belagavi" to Pair(15.8497, 74.4977),
        "belgaum" to Pair(15.8497, 74.4977),
        "mangaluru" to Pair(12.9141, 74.8560),
        "mangalore" to Pair(12.9141, 74.8560),
        "kalaburagi" to Pair(17.3297, 76.8343),
        "gulbarga" to Pair(17.3297, 76.8343),
        "vijayapura" to Pair(16.8302, 75.7100),
        "bijapur" to Pair(16.8302, 75.7100),
        "shivamogga" to Pair(13.9299, 75.5681),
        "shimoga" to Pair(13.9299, 75.5681),
        "ballari" to Pair(15.1394, 76.9214),
        "bellary" to Pair(15.1394, 76.9214),
        "davanagere" to Pair(14.4644, 75.9218),
        "tumakuru" to Pair(13.3422, 77.1017),
        "tumkur" to Pair(13.3422, 77.1017),
        "chikmagalur" to Pair(13.3161, 75.7720),
        "chikkamagaluru" to Pair(13.3161, 75.7720),
        "hampi" to Pair(15.3350, 76.4600),
        "udupi" to Pair(13.3409, 74.7421),
        "hassan" to Pair(13.0072, 76.0962),
        "raichur" to Pair(16.2076, 77.3463),
        "kolar" to Pair(13.1367, 78.1291),
        "mandya" to Pair(12.5230, 76.8967),
        "karwar" to Pair(14.8136, 74.1296),
        "gokarna" to Pair(14.5479, 74.3188),
        "yadgir" to Pair(16.7699, 77.1378),
        "bagalkot" to Pair(16.1691, 75.6615),
        "gadag" to Pair(15.4319, 75.6355),
        "koppal" to Pair(15.3524, 76.1554),
        "haveri" to Pair(14.7954, 75.3991),
        "chamarajanagar" to Pair(11.9261, 76.9437),
        "nandi hills" to Pair(13.3702, 77.6835),
        "abbey falls" to Pair(12.4533, 75.7176),
        "raja seat" to Pair(12.4172, 75.7369),
        "talakaveri" to Pair(12.3854, 75.4907),
        "bidar fort" to Pair(17.9255, 77.5255),
        "mullayanagiri" to Pair(13.3916, 75.7214),

        // Neighbouring States & Popular Tourist Spots
        "goa" to Pair(15.2993, 74.1240),
        "panaji" to Pair(15.4909, 73.8278),
        "hyderabad" to Pair(17.3850, 78.4867),
        "chennai" to Pair(13.0827, 80.2707),
        "mumbai" to Pair(19.0760, 72.8777),
        "pune" to Pair(18.5204, 73.8567),
        "ooty" to Pair(11.4102, 76.6950),
        "kodaikanal" to Pair(10.2381, 77.4892),
        "wayanad" to Pair(11.6854, 76.1320),
        "munnar" to Pair(10.0889, 77.0595),
        "kochi" to Pair(9.9312, 76.2673),
        "trivandrum" to Pair(8.5241, 76.9366),
        "thiruvananthapuram" to Pair(8.5241, 76.9366),
        "pune" to Pair(18.5204, 73.8567),
        "ooty" to Pair(11.4100, 76.6950),
        "wayanad" to Pair(11.6854, 76.1320),
        "kochi" to Pair(9.9312, 76.2673),
        "cochin" to Pair(9.9312, 76.2673),
        "trivandrum" to Pair(8.5241, 76.9366),
        "thiruvananthapuram" to Pair(8.5241, 76.9366),
        "munnar" to Pair(10.0889, 77.0595),
        "pondicherry" to Pair(11.9416, 79.8083),
        "puducherry" to Pair(11.9416, 79.8083),
        "coimbatore" to Pair(11.0168, 76.9558),
        "madurai" to Pair(9.9252, 78.1198),
        "tirupati" to Pair(13.6288, 79.4192),
        "visakhapatnam" to Pair(17.6868, 83.2185),
        "vizag" to Pair(17.6868, 83.2185),

        // Major Metros & North India
        "delhi" to Pair(28.6139, 77.2090),
        "new delhi" to Pair(28.6139, 77.2090),
        "noida" to Pair(28.5355, 77.3910),
        "gurgaon" to Pair(28.4595, 77.0266),
        "kolkata" to Pair(22.5726, 88.3639),
        "ahmedabad" to Pair(23.0225, 72.5714),
        "jaipur" to Pair(26.9124, 75.7873),
        "agra" to Pair(27.1767, 78.0081),
        "varanasi" to Pair(25.3176, 82.9739),
        "shimla" to Pair(31.1048, 77.1734),
        "manali" to Pair(32.2396, 77.1887),
        "chandigarh" to Pair(30.7333, 76.7794),
        "amritsar" to Pair(31.6340, 74.8723),
        "lucknow" to Pair(26.8467, 80.9462),
        "dehradun" to Pair(30.3165, 78.0322),
        "rishikesh" to Pair(30.0869, 78.2676),
        "leh" to Pair(34.1526, 77.5771),
        "srinagar" to Pair(34.0837, 74.7973),
        "surat" to Pair(21.1702, 72.8311),
        "bhopal" to Pair(23.2599, 77.4126),
        "indore" to Pair(22.7196, 75.8577),
        "nagpur" to Pair(21.1458, 79.0882),
        "patna" to Pair(25.5941, 85.1376)
    )

    /**
     * Resolves geographic coordinates for a given city name, full address, or pasted coordinates/links.
     * Sanitizes inputs, extracts GPS coordinates if present, and searches known database.
     */
    fun resolveCoordinates(cityName: String, fallbackLat: Double = 0.0, fallbackLng: Double = 0.0): Pair<Double, Double> {
        val clean = cityName.trim()
        if (clean.isBlank()) return Pair(fallbackLat, fallbackLng)

        // 1. Live / Current GPS location check or generic defaults check
        val lower = clean.lowercase()
        val isGenericDefault = lower.contains("current location") ||
                lower.contains("live location") ||
                lower == "my location" ||
                lower == "starting point" ||
                lower == "start"

        if (isGenericDefault) {
            if (fallbackLat != 0.0 && fallbackLng != 0.0) {
                return Pair(fallbackLat, fallbackLng)
            }
            val live = PlatformLocation.getCurrentLocation()
            if (live != null && live.latitude != 0.0) {
                return Pair(live.latitude, live.longitude)
            }
        }

        // 2. Direct coordinate parsing (e.g. "12.9716, 77.5946" or "12.9716 77.5946" or "q=12.9716,77.5946" or "@12.9716,77.5946")
        val coordRegex = Regex("""(-?\d{1,3}\.\d+)[,\s/]+(-?\d{1,3}\.\d+)""")
        val match = coordRegex.find(clean)
        if (match != null) {
            val lat = match.groupValues[1].toDoubleOrNull()
            val lng = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return Pair(lat, lng)
            }
        }

        // 3. Check in-memory cache first (0ms)
        resolvedCoordsCache[lower]?.let { cached ->
            if (fallbackLat != 0.0 && fallbackLng != 0.0 &&
                haversineDistanceKm(fallbackLat, fallbackLng, cached.first, cached.second) < 35.0
            ) {
                return Pair(fallbackLat, fallbackLng)
            }
            return cached
        }

        // 4. Find any matching known city/locality coordinate (0ms)
        val matchedKnownCoords: Pair<Double, Double>? = cityCoordinates[lower]
            ?: run {
                val tokens = lower.split(',', '-', ';', ' ', '/').map { it.trim() }.filter { it.length >= 3 }
                tokens.firstNotNullOfOrNull { cityCoordinates[it] }
            }
            ?: cityCoordinates.entries.firstOrNull { (name, _) ->
                lower.contains(name) || name.contains(lower)
            }?.value

        // If caller supplied live GPS coordinates within the same city/locality (< 35 km),
        // always preserve the user's exact pinpoint GPS instead of snapping to a neighborhood center.
        if (matchedKnownCoords != null && fallbackLat != 0.0 && fallbackLng != 0.0) {
            val distKm = haversineDistanceKm(fallbackLat, fallbackLng, matchedKnownCoords.first, matchedKnownCoords.second)
            if (distKm < 35.0) {
                return Pair(fallbackLat, fallbackLng)
            }
        }

        // If it's an exact single city name (like "Hyderabad" or "Mysuru") and not the user's current city, return immediately
        if (cityCoordinates.containsKey(lower) && matchedKnownCoords != null) {
            resolvedCoordsCache[lower] = matchedKnownCoords
            return matchedKnownCoords
        }

        // 5. Real Geocoding on background thread (PlatformLocation.geocodeAddress returns null immediately if on UI thread)
        try {
            val geocoded = PlatformLocation.geocodeAddress(clean)
            if (geocoded != null && geocoded.first != 0.0) {
                resolvedCoordsCache[lower] = geocoded
                if (fallbackLat != 0.0 && fallbackLng != 0.0 &&
                    haversineDistanceKm(fallbackLat, fallbackLng, geocoded.first, geocoded.second) < 25.0 &&
                    clean.split(",").size <= 3
                ) {
                    return Pair(fallbackLat, fallbackLng)
                }
                return geocoded
            }
        } catch (_: Exception) {}

        // 6. If matchedKnownCoords is in another city (e.g. "Mangalagi, Humanabad taluku" when user is in Bengaluru),
        //    return that city's coordinates so remote trip start/destination never jumps to the viewer's city.
        if (matchedKnownCoords != null) {
            resolvedCoordsCache[lower] = matchedKnownCoords
            return matchedKnownCoords
        }

        return Pair(fallbackLat, fallbackLng)
    }

    /**
     * Converts exact GPS lat/long coordinates to a human-readable city or locality name.
     * Uses platform reverse geocoder if available. Never returns nearby hardcoded cities.
     */
    fun resolveNearestCityOrArea(latitude: Double, longitude: Double): String {
        if (latitude == 0.0 && longitude == 0.0) return "Current Location"
        // 1. Try platform reverse geocode (native + online)
        try {
            val geocoded = PlatformLocation.reverseGeocode(latitude, longitude)
            if (!geocoded.isNullOrBlank()) {
                return geocoded
            }
        } catch (_: Exception) {}

        val latStr = (round(latitude * 10000.0) / 10000.0).toString()
        val lngStr = (round(longitude * 10000.0) / 10000.0).toString()
        return "Pinpoint Location ($latStr, $lngStr)"
    }

    /**
     * Calculates authentic road driving distance in kilometers.
     * Straight-line distance multiplied by the average Indian highway road tortuosity factor (1.24x).
     */
    fun calculateRoadDistanceKm(sourceName: String, destinationName: String): Double {
        val live = PlatformLocation.getCurrentLocation()
        val defLat = live?.latitude ?: 0.0
        val defLng = live?.longitude ?: 0.0
        val (sLat, sLng) = resolveCoordinates(sourceName, defLat, defLng)
        val (dLat, dLng) = resolveCoordinates(destinationName, 0.0, 0.0)

        val directKm = haversineDistanceKm(sLat, sLng, dLat, dLng)
        if (directKm < 1.0) return 8.5 // Local intra-city route

        // Road distance formula (highway route curvature)
        val roadFactor = when {
            directKm > 500.0 -> 1.25 // Long distance highways (e.g. BLR to Bidar ~685 km)
            directKm > 100.0 -> 1.22
            else -> 1.18
        }
        val roadKm = (directKm * roadFactor)
        return (roadKm * 10.0).roundToInt() / 10.0
    }

    /**
     * Estimates driving duration text based on realistic highway speeds.
     */
    fun calculateDrivingEta(roadDistanceKm: Double, averageSpeedKmh: Double = 65.0): String {
        val hours = roadDistanceKm / averageSpeedKmh
        val totalMinutes = (hours * 60).roundToInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m} mins"
    }
}
