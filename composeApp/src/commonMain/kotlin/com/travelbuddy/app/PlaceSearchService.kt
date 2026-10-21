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
        PlaceSuggestion("Gokarna", "Gokarna, Uttara Kannada, Karnataka, India", 14.5479, 74.3188),
        PlaceSuggestion("Rishikesh", "Rishikesh, Dehradun, Uttarakhand, India", 30.0869, 78.2676),
        PlaceSuggestion("Darjeeling", "Darjeeling, West Bengal, India", 27.0410, 88.2663),
        PlaceSuggestion("Shillong", "Shillong, East Khasi Hills, Meghalaya, India", 25.5788, 91.8933)
    )

    suspend fun searchPlaces(query: String): List<PlaceSuggestion> = withContext(Dispatchers.Default) {
        val clean = query.trim()
        if (clean.length < 2) return@withContext emptyList()

        // 1. Check local fast-match
        val localMatches = verifiedDestinations.filter {
            it.name.contains(clean, ignoreCase = true) ||
            it.description.contains(clean, ignoreCase = true)
        }

        // 2. Fetch live online search from Nominatim geocoder
        val onlineMatches = try {
            val url = "https://nominatim.openstreetmap.org/search?q=${clean.encodeURLParameter()}&format=json&addressdetails=1&limit=5"
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
