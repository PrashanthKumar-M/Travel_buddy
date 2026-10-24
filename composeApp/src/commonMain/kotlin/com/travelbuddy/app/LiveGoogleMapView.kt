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
        val listener: (RealLocation) -> Unit = { updated ->
            realLocationState.value = updated
        }
        PlatformLocation.startLocationUpdates(listener)
        onDispose {
            PlatformLocation.removeLocationListener(listener)
        }
    }

    val activeUserLat = when {
        realLocationState.value != null && realLocationState.value!!.latitude != 0.0 -> realLocationState.value!!.latitude
        currentUserLat != 0.0 -> currentUserLat
        PlatformLocation.getCurrentLocation() != null && PlatformLocation.getCurrentLocation()!!.latitude != 0.0 -> PlatformLocation.getCurrentLocation()!!.latitude
        else -> 0.0
    }
    val activeUserLng = when {
        realLocationState.value != null && realLocationState.value!!.longitude != 0.0 -> realLocationState.value!!.longitude
        currentUserLng != 0.0 -> currentUserLng
        PlatformLocation.getCurrentLocation() != null && PlatformLocation.getCurrentLocation()!!.longitude != 0.0 -> PlatformLocation.getCurrentLocation()!!.longitude
        else -> 0.0
    }

    // Coordinate resolution (fast initial + async background refinement so UI never blocks)
    var resolvedSrc by remember(sourceName, sourceLat, sourceLng, activeUserLat, activeUserLng) {
        mutableStateOf(
            if (sourceLat != 0.0 && sourceLng != 0.0) {
                Pair(sourceLat, sourceLng)
            } else if (activeUserLat != 0.0 && activeUserLng != 0.0) {
                Pair(activeUserLat, activeUserLng)
            } else {
                LocationCoordinateResolver.resolveCoordinates(sourceName)
            }
        )
    }
    LaunchedEffect(sourceName, sourceLat, sourceLng, activeUserLat, activeUserLng) {
        if (sourceLat != 0.0 && sourceLng != 0.0) {
            resolvedSrc = Pair(sourceLat, sourceLng)
        } else if (sourceName.isNotBlank() && sourceName != "Starting Point" && !sourceName.equals("Current Location", ignoreCase = true)) {
            val asyncCoords = LocationCoordinateResolver.resolveCoordinatesAsync(sourceName, activeUserLat, activeUserLng)
            if (asyncCoords.first != 0.0 && asyncCoords.second != 0.0) {
                resolvedSrc = asyncCoords
            }
        } else if (activeUserLat != 0.0 && activeUserLng != 0.0) {
            resolvedSrc = Pair(activeUserLat, activeUserLng)
        }
    }
    val (resolvedSrcLat, resolvedSrcLng) = resolvedSrc

    val hasValidDestination = remember(destinationName) {
        destinationName.isNotBlank() &&
            !destinationName.equals("Destination", ignoreCase = true) &&
            !destinationName.equals("Trip Destination", ignoreCase = true) &&
            !destinationName.equals("Coorg Valley", ignoreCase = true) &&
            !destinationName.equals(sourceName, ignoreCase = true)
    }

    var resolvedDst by remember(destinationName, destinationLat, destinationLng, hasValidDestination) {
        mutableStateOf(
            if (!hasValidDestination) Pair(0.0, 0.0)
            else if (destinationLat != 0.0 && destinationLng != 0.0) Pair(destinationLat, destinationLng)
            else LocationCoordinateResolver.resolveCoordinates(destinationName, destinationLat, destinationLng)
        )
    }
    LaunchedEffect(destinationName, destinationLat, destinationLng, hasValidDestination) {
        if (!hasValidDestination) {
            resolvedDst = Pair(0.0, 0.0)
        } else if (destinationLat != 0.0 && destinationLng != 0.0) {
            resolvedDst = Pair(destinationLat, destinationLng)
        } else {
            val asyncDst = LocationCoordinateResolver.resolveCoordinatesAsync(destinationName, destinationLat, destinationLng)
            if (asyncDst.first != 0.0 && asyncDst.second != 0.0) {
                resolvedDst = asyncDst
            }
        }
    }
    val (resolvedDstLat, resolvedDstLng) = resolvedDst

    val totalDistance = remember(resolvedSrcLat, resolvedSrcLng, resolvedDstLat, resolvedDstLng, hasValidDestination) {
        if (hasValidDestination && resolvedSrcLat != 0.0 && resolvedDstLat != 0.0) {
            val directKm = haversineDistanceKm(resolvedSrcLat, resolvedSrcLng, resolvedDstLat, resolvedDstLng)
            val factor = when {
                directKm > 500.0 -> 1.25
                directKm > 100.0 -> 1.22
                else -> 1.18
            }
            ((directKm * factor) * 10.0).roundToInt() / 10.0
        } else 0.0
    }
    val drivingEta = remember(totalDistance, hasValidDestination) {
        if (hasValidDestination && totalDistance > 0.0) {
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
                        val liveAddr = realLocationState.value?.address
                        val rawSrc = sourceName.substringBefore("(").trim().ifBlank { sourceName.trim() }
                        val cleanSrc = if (rawSrc.isBlank() || rawSrc.equals("Starting Point", ignoreCase = true) || rawSrc.equals("Current Location", ignoreCase = true)) {
                            liveAddr?.substringBefore(",")?.trim()?.ifBlank { "Live Location" } ?: "Live Location"
                        } else rawSrc
                        val cleanDst = destinationName.substringBefore("(").trim().ifBlank { destinationName.trim() }
                        Text(
                            text = if (hasValidDestination) {
                                "$cleanSrc ➔ $cleanDst"
                            } else {
                                if (!liveAddr.isNullOrBlank()) liveAddr
                                else if (cleanSrc.isNotBlank() && cleanSrc != "Starting Point") cleanSrc
                                else "Live Location Active"
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
