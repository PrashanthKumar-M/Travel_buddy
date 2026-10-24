package com.travelbuddy.app

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

data class PlaceSuggestion(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double
)

object PlaceSearchService {

    private val httpClient = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private const val GOOGLE_MAPS_API_KEY = "AIzaSyBajIytr__LvhPpGz4Wlc3l9Rpn3YP-PqY"

    // Popular destinations with pre-verified precise coordinates for instant auto-suggestions
    private val verifiedDestinations = listOf(
        PlaceSuggestion("Bengaluru", "Bengaluru, Karnataka, India", 12.9716, 77.5946),
        PlaceSuggestion("Coorg (Madikeri)", "Coorg, Kodagu, Karnataka, India", 12.4244, 75.7382),
        PlaceSuggestion("Mysuru (Mysore)", "Mysuru, Karnataka, India", 12.2958, 76.6394),
        PlaceSuggestion("Ooty", "Udhagamandalam, Nilgiris, Tamil Nadu, India", 11.4102, 76.6950),
        PlaceSuggestion("Goa (Panaji)", "Panaji, North Goa, Goa, India", 15.4909, 73.8278),
        PlaceSuggestion("Mumbai", "Mumbai, Maharashtra, India", 19.0760, 72.8777),
        PlaceSuggestion("Pune", "Pune, Maharashtra, India", 18.5204, 73.8567),
        PlaceSuggestion("Hyderabad", "Hyderabad, Telangana, India", 17.3850, 78.4867),
        PlaceSuggestion("Chennai", "Chennai, Tamil Nadu, India", 13.0827, 80.2707),
        PlaceSuggestion("Kochi (Cochin)", "Kochi, Ernakulam, Kerala, India", 9.9312, 76.2673),
        PlaceSuggestion("Munnar", "Munnar, Idukki, Kerala, India", 10.0889, 77.0595),
        PlaceSuggestion("Wayanad", "Wayanad, Kerala, India", 11.6854, 76.1320),
        PlaceSuggestion("Manali", "Manali, Kullu, Himachal Pradesh, India", 32.2432, 77.1892),
        PlaceSuggestion("Shimla", "Shimla, Himachal Pradesh, India", 31.1048, 77.1734),
        PlaceSuggestion("Leh Ladakh", "Leh, Ladakh, India", 34.1526, 77.5771),
        PlaceSuggestion("Jaipur", "Jaipur, Rajasthan, India", 26.9124, 75.7873),
        PlaceSuggestion("Udaipur", "Udaipur, Rajasthan, India", 24.5854, 73.7125),
        PlaceSuggestion("New Delhi", "New Delhi, Delhi, India", 28.6139, 77.2090),
        PlaceSuggestion("Agra", "Agra, Uttar Pradesh, India", 27.1767, 78.0081),
        PlaceSuggestion("Varanasi", "Varanasi, Uttar Pradesh, India", 25.3176, 82.9739),
        PlaceSuggestion("Pondicherry", "Puducherry, India", 11.9416, 79.8083),
        PlaceSuggestion("Chikmagalur", "Chikkamagaluru, Karnataka, India", 13.3161, 75.7720),
        PlaceSuggestion("Hampi", "Hampi, Vijayanagara, Karnataka, India", 15.3350, 76.4600),
        PlaceSuggestion("MM Hills (Male Mahadeshwara Betta)", "Hanur Taluk, Chamarajanagar, Karnataka, India", 11.9961, 77.5583),
        PlaceSuggestion("Nandi Hills", "Chikkaballapur, Karnataka, India", 13.3702, 77.6835),
        PlaceSuggestion("BR Hills (Biligirirangan Hills)", "Chamarajanagar, Karnataka, India", 11.9904, 77.1407),
        PlaceSuggestion("Skandagiri", "Chikkaballapur, Karnataka, India", 13.4180, 77.6830),
        PlaceSuggestion("Gokarna", "Gokarna, Uttara Kannada, Karnataka, India", 14.5479, 74.3188),
        PlaceSuggestion("Rishikesh", "Rishikesh, Dehradun, Uttarakhand, India", 30.0869, 78.2676),
        PlaceSuggestion("Darjeeling", "Darjeeling, West Bengal, India", 27.0410, 88.2663),
        PlaceSuggestion("Shillong", "Shillong, East Khasi Hills, Meghalaya, India", 25.5788, 91.8933)
    )

    suspend fun searchPlaces(query: String): List<PlaceSuggestion> = withContext(Dispatchers.Default) {
        val clean = query.trim()
        if (clean.length < 2) return@withContext emptyList()

        // 1. Primary: Query Platform Native Geocoder (Google Play Services Geocoder on Android)
        val platformMatches = try {
            PlatformLocation.searchAddressSuggestions(clean)
        } catch (_: Exception) {
            emptyList()
        }
        if (platformMatches.isNotEmpty()) {
            return@withContext platformMatches.take(6)
        }

        // 2. Query Official Google Places API with the configured Google Key
        val googleMatches = if (GOOGLE_MAPS_API_KEY.isNotBlank()) {
            try {
                val googleUrl = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=${clean.encodeURLParameter()}&components=country:in&key=$GOOGLE_MAPS_API_KEY"
                val response: HttpResponse = httpClient.get(googleUrl)
                if (response.status == HttpStatusCode.OK) {
                    val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
                    val status = root["status"]?.jsonPrimitive?.contentOrNull
                    if (status == "OK") {
                        val preds = root["predictions"]?.jsonArray ?: JsonArray(emptyList())
                        preds.mapNotNull { p ->
                            val pObj = p.jsonObject
                            val desc = pObj["description"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                            val structured = pObj["structured_formatting"]?.jsonObject
                            val mainText = structured?.get("main_text")?.jsonPrimitive?.contentOrNull ?: desc.split(",").firstOrNull() ?: desc
                            PlaceSuggestion(
                                name = mainText,
                                description = desc,
                                latitude = 0.0,
                                longitude = 0.0
                            )
                        }
                    } else emptyList()
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        if (googleMatches.isNotEmpty()) {
            return@withContext googleMatches.take(6)
        }

        // 3. Fast local verified matching
        val localMatches = verifiedDestinations.filter {
            it.name.contains(clean, ignoreCase = true) ||
            it.description.contains(clean, ignoreCase = true)
        }

        // 3. Resilient fallback: India-restricted Nominatim geocoder (restricted strictly to India & English)
        val onlineMatches = try {
            val url = "https://nominatim.openstreetmap.org/search?q=${clean.encodeURLParameter()}&countrycodes=in&accept-language=en&format=json&addressdetails=1&limit=8"
            val response: HttpResponse = httpClient.get(url) {
                headers {
                    append(HttpHeaders.UserAgent, "TravelBuddy-App/1.0")
                }
            }
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val array = json.parseToJsonElement(body).jsonArray
                array.mapNotNull { elem ->
                    val obj = elem.jsonObject
                    val displayName = obj["display_name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    // Filter out any non-Indian / East-Asian characters (Japanese Kanji, Hiragana, Katakana, Chinese)
                    val hasEastAsianScript = displayName.any { it.code in 0x4E00..0x9FFF || it.code in 0x3040..0x30FF || it.code in 0xAC00..0xD7AF }
                    if (hasEastAsianScript) return@mapNotNull null

                    val lat = obj["lat"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: return@mapNotNull null
                    val lon = obj["lon"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: return@mapNotNull null
                    val title = displayName.split(",").take(2).joinToString(",").trim()
                    PlaceSuggestion(
                        name = title,
                        description = displayName,
                        latitude = lat,
                        longitude = lon
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }

        // Combine local fast results with online results, deduplicating
        val combined = (localMatches + onlineMatches).distinctBy { it.name }
        combined.take(6)
    }
}
