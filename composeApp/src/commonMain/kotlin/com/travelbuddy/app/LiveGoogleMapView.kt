package com.travelbuddy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelbuddy.app.domain.TripMember
import com.travelbuddy.app.domain.Vehicle
import kotlin.math.*

data class MapTarget(
    val id: String,
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double = 0.0,
    val batteryPercent: Int = 100,
    val color: Long = 0xFF2F80EDL,
    val type: String = "USER",
    val safeZoneName: String? = null,
    val safeZoneRadiusMeters: Double? = null,
    val isInsideSafeZone: Boolean = true
)

fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = (lat2 - lat1) * (PI / 180.0)
    val dLon = (lon2 - lon1) * (PI / 180.0)
    val a = sin(dLat / 2).pow(2.0) +
            cos(lat1 * (PI / 180.0)) * cos(lat2 * (PI / 180.0)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

fun formatDistance(km: Double): String {
    return if (km < 1.0) {
        "${(km * 1000).roundToInt()} m"
    } else {
        "${(km * 10).roundToInt() / 10.0} km"
    }
}

fun formatEta(km: Double, speedKmh: Double = 40.0): String {
    val speed = if (speedKmh > 5.0) speedKmh else 40.0
    val totalHours = km / speed
    val totalMinutes = (totalHours * 60).roundToInt()
    return if (totalMinutes < 60) {
        "$totalMinutes mins"
    } else {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        "${h}h ${m}m"
    }
}

/**
 * Real Google Maps Live Tracking View.
 * Uses official Google Maps SDK (MapView / GoogleMap):
 * - Live Google Maps vector cartography & satellite tiles
 * - Official My-Location blue dot beacon with directional heading
 * - Real GPS location of joined users via Trip ID
 * - Distance matrix between each user
 */
@Composable
fun LiveGoogleMapView(
    sourceName: String = "Starting Point",
    sourceLat: Double = 0.0,
    sourceLng: Double = 0.0,
    currentUserLat: Double = 0.0,
    currentUserLng: Double = 0.0,
    members: List<TripMember> = emptyList(),
    vehicle: Vehicle? = null,
    vehicleType: String = "CAR",
    childBagDevice: MapTarget? = null,
    destinationName: String = "",
    destinationLat: Double = 0.0,
    destinationLng: Double = 0.0,
    isLiveUsersMode: Boolean = true,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxWidth().height(340.dp),
    onSelectTarget: ((MapTarget) -> Unit)? = null
) {
    val darkMode = LocalTravelBuddyDarkMode.current

    // Live GPS coordinates from platform location provider
    val realLocationState = remember { mutableStateOf(PlatformLocation.getCurrentLocation()) }
    DisposableEffect(Unit) {
        PlatformLocation.startLocationUpdates { updated ->
            realLocationState.value = updated
        }
        onDispose {
            PlatformLocation.stopLocationUpdates()
        }
    }

    val activeUserLat = when {
        currentUserLat != 0.0 && currentUserLat != 12.9716 -> currentUserLat
        realLocationState.value != null && realLocationState.value!!.latitude != 0.0 && realLocationState.value!!.latitude != 12.9716 -> realLocationState.value!!.latitude
        PlatformLocation.getCurrentLocation() != null && PlatformLocation.getCurrentLocation()!!.latitude != 0.0 && PlatformLocation.getCurrentLocation()!!.latitude != 12.9716 -> PlatformLocation.getCurrentLocation()!!.latitude
        currentUserLat != 0.0 -> currentUserLat
        realLocationState.value != null -> realLocationState.value!!.latitude
        else -> 0.0
    }
    val activeUserLng = when {
        currentUserLng != 0.0 && currentUserLng != 77.5946 -> currentUserLng
        realLocationState.value != null && realLocationState.value!!.longitude != 0.0 && realLocationState.value!!.longitude != 77.5946 -> realLocationState.value!!.longitude
        PlatformLocation.getCurrentLocation() != null && PlatformLocation.getCurrentLocation()!!.longitude != 0.0 && PlatformLocation.getCurrentLocation()!!.longitude != 77.5946 -> PlatformLocation.getCurrentLocation()!!.longitude
        currentUserLng != 0.0 -> currentUserLng
        realLocationState.value != null -> realLocationState.value!!.longitude
        else -> 0.0
    }

    // Coordinate resolution
    val (resolvedSrcLat, resolvedSrcLng) = remember(sourceName, sourceLat, sourceLng, activeUserLat, activeUserLng) {
        if (sourceLat != 0.0 && sourceLng != 0.0 && sourceLat != 12.9716) {
            Pair(sourceLat, sourceLng)
        } else if (sourceName.isBlank() || sourceName == "Starting Point" || sourceName.contains("12.9716")) {
            if (activeUserLat != 0.0 && activeUserLng != 0.0) {
                Pair(activeUserLat, activeUserLng)
            } else {
                LocationCoordinateResolver.resolveCoordinates(sourceName)
            }
        } else {
            LocationCoordinateResolver.resolveCoordinates(sourceName, activeUserLat, activeUserLng)
        }
    }

    val hasValidDestination = remember(destinationName) {
        destinationName.isNotBlank() &&
            !destinationName.equals("Destination", ignoreCase = true) &&
            !destinationName.equals("Trip Destination", ignoreCase = true) &&
            !destinationName.equals("Coorg Valley", ignoreCase = true) &&
            !destinationName.equals(sourceName, ignoreCase = true)
    }

    val (resolvedDstLat, resolvedDstLng) = remember(destinationName, destinationLat, destinationLng, hasValidDestination) {
        if (hasValidDestination) {
            LocationCoordinateResolver.resolveCoordinates(destinationName, destinationLat, destinationLng)
        } else {
            Pair(0.0, 0.0)
        }
    }

    val totalDistance = remember(sourceName, destinationName, resolvedSrcLat, resolvedDstLat, hasValidDestination) {
        if (hasValidDestination) {
            LocationCoordinateResolver.calculateRoadDistanceKm(sourceName, destinationName)
        } else 0.0
    }
    val drivingEta = remember(totalDistance, hasValidDestination) {
        if (hasValidDestination) {
            LocationCoordinateResolver.calculateDrivingEta(totalDistance)
        } else ""
    }

    val vehicleIcon = when (vehicleType.uppercase()) {
        "BIKE", "MOTORCYCLE" -> "🏍️"
        "CYCLE", "BICYCLE" -> "🚲"
        "SCOOTER" -> "🛵"
        "BUS" -> "🚌"
        else -> "🚗"
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(if (isFullscreen) 0.dp else 22.dp))
            .border(
                if (isFullscreen) 0.dp else 1.dp,
                if (darkMode) Color(0xFF334155) else Color(0xFFCBD5E1),
                RoundedCornerShape(if (isFullscreen) 0.dp else 22.dp)
            ),
        shape = RoundedCornerShape(if (isFullscreen) 0.dp else 22.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Real Google Maps SDK Integration
            NativeGoogleMapView(
                sourceName = sourceName,
                sourceLat = resolvedSrcLat,
                sourceLng = resolvedSrcLng,
                destinationName = if (hasValidDestination) destinationName else "",
                destinationLat = resolvedDstLat,
                destinationLng = resolvedDstLng,
                currentUserLat = activeUserLat,
                currentUserLng = activeUserLng,
                members = members,
                modifier = Modifier.fillMaxSize()
            )

            // Overlaid UI Chrome
            // 1. Top HUD: Vehicle Route Summary & Fullscreen Toggle (Solid background prevents map markers shining through)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .then(if (isFullscreen) Modifier.statusBarsPadding().padding(top = 28.dp) else Modifier.padding(top = 8.dp))
                    .padding(horizontal = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (darkMode) Color(0xFF1E293B) else Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF16A34A))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (hasValidDestination) {
                                    "$vehicleIcon Route: ${formatDistance(totalDistance)} ($drivingEta)"
                                } else {
                                    "📍 Live GPS Location"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (darkMode) Color.White else Color(0xFF1E293B),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        val cleanSrc = sourceName.substringBefore("(").trim().ifBlank { sourceName.trim() }
                        val cleanDst = destinationName.substringBefore("(").trim().ifBlank { destinationName.trim() }
                        Text(
                            text = if (hasValidDestination) {
                                "$cleanSrc ➔ $cleanDst"
                            } else {
                                val locAddr = realLocationState.value?.address
                                if (!locAddr.isNullOrBlank()) locAddr
                                else if (cleanSrc.isNotBlank() && cleanSrc != "Starting Point") cleanSrc
                                else "Bengaluru, India"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF1A73E8),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))

                    if (onToggleFullscreen != null) {
                        if (isFullscreen) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (darkMode) Color(0xFF334155) else Color(0xFF1E293B),
                                modifier = Modifier.clickable(onClick = onToggleFullscreen)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.FullscreenExit,
                                        contentDescription = "Minimize",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Minimize",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color(0xFF1A73E8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
