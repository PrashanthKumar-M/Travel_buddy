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

        // 2. Query Official Google Places API if available
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

        // 3. Resilient fallback: India-restricted Nominatim geocoder (strictly dynamic, live geocoding)
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
                    // Filter out non-Indian / East-Asian scripts
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

        onlineMatches.distinctBy { it.name }.take(6)
    }
}
