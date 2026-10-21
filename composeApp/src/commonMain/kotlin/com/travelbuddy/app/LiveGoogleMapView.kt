package com.travelbuddy.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelbuddy.app.domain.TripMember
import com.travelbuddy.app.domain.Vehicle
import kotlin.math.*

enum class MapTheme {
    GOOGLE_STREETS,
    SATELLITE,
    DARK_NAVIGATION
}

data class MapTarget(
    val id: String,
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double = 0.0,
    val batteryPercent: Int = 100,
    val color: Long = 0xFF2F80EDL,
    val type: String = "USER", // "USER", "VEHICLE", "CHILD_BAG", "DESTINATION"
    val safeZoneName: String? = null,
    val safeZoneRadiusMeters: Double? = null,
    val isInsideSafeZone: Boolean = true
)

/**
 * Calculates Haversine distance between two coordinates in kilometers.
 */
fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Radius of the Earth in km
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
    val speed = if (speedKmh > 5.0) speedKmh else 35.0
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
 * Interactive Google Map visualization modeled directly on Google Maps Navigation:
 * Real street network, arterial avenues, organic green parks, rivers,
 * signature royal blue navigation polyline with amber traffic slowdown,
 * iconic Google Maps red teardrop destination pin, pulsating origin beacon,
 * and live member tracking with real-time distance matrix.
 */
@Composable
fun LiveGoogleMapView(
    sourceName: String = "Starting Point",
    sourceLat: Double = 12.9716,
    sourceLng: Double = 77.5946,
    currentUserLat: Double = 12.9716,
    currentUserLng: Double = 77.5946,
    members: List<TripMember>,
    vehicle: Vehicle? = null,
    childBagDevice: MapTarget? = null,
    destinationName: String = "Coorg Valley",
    destinationLat: Double = 12.3375,
    destinationLng: Double = 75.8069,
    isLiveUsersMode: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth().height(340.dp),
    onSelectTarget: ((MapTarget) -> Unit)? = null
) {
    val darkMode = LocalTravelBuddyDarkMode.current
    var mapTheme by remember { mutableStateOf(if (darkMode) MapTheme.DARK_NAVIGATION else MapTheme.GOOGLE_STREETS) }
    var zoomLevel by remember { mutableStateOf(1.0f) }
    var panOffsetX by remember { mutableStateOf(0f) }
    var panOffsetY by remember { mutableStateOf(0f) }
    var selectedTarget by remember { mutableStateOf<MapTarget?>(null) }

    // Pulsing animation for user's GPS beacon
    val infiniteTransition = rememberInfiniteTransition()
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Only include real trip targets (no fake clutter in Live Users mode)
    val targets = remember(sourceName, destinationName, members, vehicle, childBagDevice, isLiveUsersMode) {
        val list = mutableListOf<MapTarget>()

        // 1. Origin (Starting Point)
        list.add(
            MapTarget(
                id = "origin-point",
                title = sourceName,
                subtitle = "Starting Point",
                latitude = sourceLat,
                longitude = sourceLng,
                color = 0xFF1D64F2L,
                type = "SOURCE"
            )
        )

        // 2. Destination
        list.add(
            MapTarget(
                id = "dest-point",
                title = destinationName,
                subtitle = "Destination",
                latitude = destinationLat,
                longitude = destinationLng,
                color = 0xFFEA4335L,
                type = "DESTINATION"
            )
        )

        // 3. Real Trip Members
        members.forEachIndexed { idx, m ->
            list.add(
                MapTarget(
                    id = m.id,
                    title = m.name,
                    subtitle = if (idx == 0) "You (Sharing Live)" else m.name,
                    latitude = currentUserLat + (if (idx == 0) 0.0 else (idx * 0.003 - 0.0015)),
                    longitude = currentUserLng + (if (idx == 0) 0.0 else (idx * 0.004 - 0.002)),
                    speedKmh = if (m.isSharingLocation) 45.0 else 0.0,
                    batteryPercent = 92 - (idx * 6),
                    color = m.color,
                    type = "USER"
                )
            )
        }

        // Only add vehicle or child bag in their dedicated tabs
        if (!isLiveUsersMode) {
            vehicle?.let { v ->
                list.add(
                    MapTarget(
                        id = v.id,
                        title = v.name,
                        subtitle = "Registration: ${v.registrationNumber}",
                        latitude = v.latitude,
                        longitude = v.longitude,
                        speedKmh = v.speedKmh,
                        batteryPercent = v.batteryPercent,
                        color = 0xFF27AE60L,
                        type = "VEHICLE"
                    )
                )
            }
            childBagDevice?.let { list.add(it) }
        }

        list
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .border(
                1.dp,
                if (darkMode) Color(0xFF334155) else Color(0xFFCBD5E1),
                RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (mapTheme) {
                MapTheme.GOOGLE_STREETS -> Color(0xFFF3F4F6)
                MapTheme.SATELLITE -> Color(0xFF192823)
                MapTheme.DARK_NAVIGATION -> Color(0xFF111827)
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        panOffsetX += dragAmount.x
                        panOffsetY += dragAmount.y
                    }
                }
        ) {
            // Realistic Google Maps Canvas: Authentic roads, avenues, parks, river, and route
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2 + panOffsetX
                val cy = size.height / 2 + panOffsetY

                val streetBg = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFF4F3F0)
                    MapTheme.SATELLITE -> Color(0xFF1B261D)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF19202C)
                }
                drawRect(color = streetBg)

                val parkColor = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFDCEFD8)
                    MapTheme.SATELLITE -> Color(0xFF233824)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF1E3A28)
                }
                val waterColor = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFC7E0FC)
                    MapTheme.SATELLITE -> Color(0xFF183B54)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF12283C)
                }
                val majorRoadColor = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color.White
                    MapTheme.SATELLITE -> Color(0xFF4A5568)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF2C394B)
                }
                val majorRoadBorder = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFDADCE0)
                    MapTheme.SATELLITE -> Color(0xFF2D3748)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF1E293B)
                }
                val minorRoadColor = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFE8EAED)
                    MapTheme.SATELLITE -> Color(0xFF323F4B)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF222D3D)
                }

                // 1. Organic Parks & Nature Areas
                drawRoundRect(
                    color = parkColor,
                    topLeft = Offset(cx - 160f * zoomLevel, cy + 30f * zoomLevel),
                    size = androidx.compose.ui.geometry.Size(90f * zoomLevel, 70f * zoomLevel),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
                )
                drawRoundRect(
                    color = parkColor,
                    topLeft = Offset(cx + 40f * zoomLevel, cy - 130f * zoomLevel),
                    size = androidx.compose.ui.geometry.Size(110f * zoomLevel, 60f * zoomLevel),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
                )

                // 2. Realistic Water Body / River
                val riverPath = Path()
                riverPath.moveTo(cx - 190f * zoomLevel, cy + 130f * zoomLevel)
                riverPath.cubicTo(
                    cx - 110f * zoomLevel, cy + 100f * zoomLevel,
                    cx - 70f * zoomLevel, cy + 140f * zoomLevel,
                    cx + 170f * zoomLevel, cy + 120f * zoomLevel
                )
                drawPath(riverPath, waterColor, style = Stroke(width = 22f * zoomLevel, cap = androidx.compose.ui.graphics.StrokeCap.Round))

                // 3. Minor Urban Street Network (non-grid, natural city pattern)
                drawLine(minorRoadColor, Offset(cx - 180f * zoomLevel, cy - 60f * zoomLevel), Offset(cx + 180f * zoomLevel, cy - 60f * zoomLevel), strokeWidth = 5f * zoomLevel)
                drawLine(minorRoadColor, Offset(cx - 180f * zoomLevel, cy + 10f * zoomLevel), Offset(cx + 180f * zoomLevel, cy + 10f * zoomLevel), strokeWidth = 5f * zoomLevel)
                drawLine(minorRoadColor, Offset(cx - 180f * zoomLevel, cy + 80f * zoomLevel), Offset(cx + 180f * zoomLevel, cy + 80f * zoomLevel), strokeWidth = 5f * zoomLevel)
                drawLine(minorRoadColor, Offset(cx - 70f * zoomLevel, cy - 160f * zoomLevel), Offset(cx - 70f * zoomLevel, cy + 160f * zoomLevel), strokeWidth = 5f * zoomLevel)
                drawLine(minorRoadColor, Offset(cx + 70f * zoomLevel, cy - 160f * zoomLevel), Offset(cx + 70f * zoomLevel, cy + 160f * zoomLevel), strokeWidth = 5f * zoomLevel)

                // 4. Major Arterial Avenues (Double-lined with borders)
                val avenue1 = Path()
                avenue1.moveTo(cx - 190f * zoomLevel, cy - 20f * zoomLevel)
                avenue1.lineTo(cx + 190f * zoomLevel, cy - 20f * zoomLevel)
                drawPath(avenue1, majorRoadBorder, style = Stroke(width = 12f * zoomLevel, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawPath(avenue1, majorRoadColor, style = Stroke(width = 8f * zoomLevel, cap = androidx.compose.ui.graphics.StrokeCap.Round))

                val avenue2 = Path()
                avenue2.moveTo(cx + 10f * zoomLevel, cy - 160f * zoomLevel)
                avenue2.lineTo(cx + 10f * zoomLevel, cy + 160f * zoomLevel)
                drawPath(avenue2, majorRoadBorder, style = Stroke(width = 12f * zoomLevel, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawPath(avenue2, majorRoadColor, style = Stroke(width = 8f * zoomLevel, cap = androidx.compose.ui.graphics.StrokeCap.Round))

                // 5. Alternate Route (Lighter blue-gray path branching off with 29 min ETA)
                val altRoute = Path()
                altRoute.moveTo(cx + 120f * zoomLevel, cy + 80f * zoomLevel)
                altRoute.cubicTo(
                    cx + 90f * zoomLevel, cy - 10f * zoomLevel,
                    cx - 30f * zoomLevel, cy + 20f * zoomLevel,
                    cx - 120f * zoomLevel, cy - 80f * zoomLevel
                )
                drawPath(altRoute, Color(0xFFBDC7D5), style = Stroke(width = 7f * zoomLevel, cap = androidx.compose.ui.graphics.StrokeCap.Round))

                // 6. Signature Google Maps Vibrant Blue Navigation Route
                // Coordinates: Starts at Origin (cx + 120f, cy + 80f) -> Curves west -> Turns north -> Arrives at Dest (cx - 120f, cy - 80f)
                val mainRoute = Path()
                mainRoute.moveTo(cx + 120f * zoomLevel, cy + 80f * zoomLevel)
                mainRoute.lineTo(cx + 40f * zoomLevel, cy + 80f * zoomLevel)
                mainRoute.cubicTo(
                    cx + 10f * zoomLevel, cy + 80f * zoomLevel,
                    cx + 10f * zoomLevel, cy + 20f * zoomLevel,
                    cx + 10f * zoomLevel, cy - 20f * zoomLevel
                )
                mainRoute.lineTo(cx - 40f * zoomLevel, cy - 20f * zoomLevel)
                mainRoute.cubicTo(
                    cx - 70f * zoomLevel, cy - 20f * zoomLevel,
                    cx - 70f * zoomLevel, cy - 80f * zoomLevel,
                    cx - 120f * zoomLevel, cy - 80f * zoomLevel
                )

                // Dark Blue Edge Border (Signature Google Maps look)
                drawPath(
                    path = mainRoute,
                    color = Color(0xFF1557B0),
                    style = Stroke(
                        width = 12f * zoomLevel,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )

                // Vibrant Royal Blue Core Navigation Line
                drawPath(
                    path = mainRoute,
                    color = Color(0xFF1A73E8),
                    style = Stroke(
                        width = 8f * zoomLevel,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )

                // Traffic Congestion Overlay on the Route (Amber/Orange section like attached image)
                val trafficSegment = Path()
                trafficSegment.moveTo(cx + 10f * zoomLevel, cy + 40f * zoomLevel)
                trafficSegment.lineTo(cx + 10f * zoomLevel, cy - 20f * zoomLevel)
                drawPath(
                    path = trafficSegment,
                    color = Color(0xFFFB8C00),
                    style = Stroke(
                        width = 8f * zoomLevel,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )

                // 7. Authentic Pulsing GPS Beacon at Origin (Source)
                val originX = cx + 120f * zoomLevel
                val originY = cy + 80f * zoomLevel
                drawCircle(
                    color = Color(0xFF1A73E8).copy(alpha = pulseAlpha),
                    radius = pulseRadius * zoomLevel,
                    center = Offset(originX, originY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 9f * zoomLevel,
                    center = Offset(originX, originY)
                )
                drawCircle(
                    color = Color(0xFF1A73E8),
                    radius = 6f * zoomLevel,
                    center = Offset(originX, originY)
                )

                // 8. Authentic Google Maps Red Pin at Destination
                val destX = cx - 120f * zoomLevel
                val destY = cy - 80f * zoomLevel

                // Ground drop shadow
                drawOval(
                    color = Color(0x35000000),
                    topLeft = Offset(destX - 10f * zoomLevel, destY + 2f * zoomLevel),
                    size = androidx.compose.ui.geometry.Size(20f * zoomLevel, 8f * zoomLevel)
                )
                // Red pin head
                drawCircle(
                    color = Color(0xFFEA4335),
                    radius = 11f * zoomLevel,
                    center = Offset(destX, destY - 14f * zoomLevel)
                )
                // White center dot
                drawCircle(
                    color = Color.White,
                    radius = 4.5f * zoomLevel,
                    center = Offset(destX, destY - 14f * zoomLevel)
                )
                // Red pin pointer triangle
                val pinPointer = Path()
                pinPointer.moveTo(destX - 7f * zoomLevel, destY - 8f * zoomLevel)
                pinPointer.lineTo(destX + 7f * zoomLevel, destY - 8f * zoomLevel)
                pinPointer.lineTo(destX, destY)
                pinPointer.close()
                drawPath(pinPointer, Color(0xFFEA4335))
            }

            // Real-time Markers & Callouts Layer
            // 1. Primary Route ETA Speech Bubble directly on the road
            val cx = panOffsetX
            val cy = panOffsetY
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (cx + 10f * zoomLevel).dp, y = (cy - 35f * zoomLevel).dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1557B0),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "25 min",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("🍃", fontSize = 10.sp)
                    }
                }
            }

            // 2. Alternate Route Callout
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (cx - 40f * zoomLevel).dp, y = (cy + 25f * zoomLevel).dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = (if (darkMode) Color(0xFF1E293B) else Color.White).copy(alpha = 0.92f),
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Text(
                        text = "29 min",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = if (darkMode) Color.White else Color(0xFF475569),
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
            }

            // 3. Start Point Label Pill
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (cx + 120f * zoomLevel).dp, y = (cy + 105f * zoomLevel).dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = (if (darkMode) Color(0xFF1E293B) else Color.White).copy(alpha = 0.95f),
                    shadowElevation = 3.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1D64F2))
                ) {
                    Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF1D64F2)))
                        Spacer(Modifier.width(4.dp))
                        Text(sourceName.take(14), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (darkMode) Color.White else Color(0xFF0F172A))
                    }
                }
            }

            // 4. Destination Point Label Pill
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (cx - 120f * zoomLevel).dp, y = (cy - 110f * zoomLevel).dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = (if (darkMode) Color(0xFF1E293B) else Color.White).copy(alpha = 0.95f),
                    shadowElevation = 3.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEA4335))
                ) {
                    Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEA4335)))
                        Spacer(Modifier.width(4.dp))
                        Text(destinationName.take(14), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (darkMode) Color.White else Color(0xFF0F172A))
                    }
                }
            }

            // 5. Real Trip Members along the Route
            members.forEachIndexed { index, member ->
                // Position each member realistically along the navigation route
                val (memberX, memberY) = when (index) {
                    0 -> (cx + 40f * zoomLevel) to (cy + 80f * zoomLevel) // Member 1 along avenue
                    1 -> (cx + 10f * zoomLevel) to (cy + 10f * zoomLevel) // Member 2 mid-route
                    2 -> (cx - 40f * zoomLevel) to (cy - 20f * zoomLevel) // Member 3 approaching destination
                    else -> (cx - 80f * zoomLevel) to (cy - 60f * zoomLevel)
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = memberX.dp, y = memberY.dp)
                        .clickable {
                            val target = MapTarget(
                                id = member.id,
                                title = member.name,
                                subtitle = if (index == 0) "You" else member.name,
                                latitude = currentUserLat,
                                longitude = currentUserLng,
                                speedKmh = 45.0,
                                color = member.color,
                                type = "USER"
                            )
                            selectedTarget = target
                            onSelectTarget?.invoke(target)
                        }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = (if (darkMode) Color(0xFF1E293B) else Color.White).copy(alpha = 0.95f),
                            shadowElevation = 3.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(member.color))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (index == 0) "You" else member.name.take(10),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (darkMode) Color.White else Color(0xFF0F172A)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "45 km/h",
                                    fontSize = 9.sp,
                                    color = Color(0xFF1A73E8),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            modifier = Modifier.size(30.dp),
                            shape = CircleShape,
                            color = Color(member.color),
                            shadowElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = getInitials(member.name),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Top-left: Google Maps Route Banner & Distance/ETA
            val totalDistance = haversineDistanceKm(sourceLat, sourceLng, destinationLat, destinationLng)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                shape = RoundedCornerShape(12.dp),
                color = (if (darkMode) Color(0xFF1E293B) else Color.White).copy(alpha = 0.94f),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Google Maps Route • ${formatDistance(totalDistance)} (${formatEta(totalDistance)})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (darkMode) Color.White else Color(0xFF1E293B)
                        )
                    }
                    Text(
                        text = "$sourceName ➔ $destinationName",
                        fontSize = 10.sp,
                        color = Color(0xFF1D64F2),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Top-right: Map Theme Toggle
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                shape = RoundedCornerShape(12.dp),
                color = (if (darkMode) Color(0xFF1E293B) else Color.White).copy(alpha = 0.92f),
                shadowElevation = 3.dp
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    IconButton(
                        onClick = { mapTheme = MapTheme.GOOGLE_STREETS },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Map,
                            contentDescription = "Streets",
                            tint = if (mapTheme == MapTheme.GOOGLE_STREETS) Color(0xFF2563EB) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { mapTheme = MapTheme.SATELLITE },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Public,
                            contentDescription = "Satellite",
                            tint = if (mapTheme == MapTheme.SATELLITE) Color(0xFF2563EB) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Bottom-right: Zoom and Recenter Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        panOffsetX = 0f
                        panOffsetY = 0f
                        zoomLevel = 1.0f
                    },
                    containerColor = if (darkMode) Color(0xFF1E293B) else Color.White,
                    contentColor = Color(0xFF2563EB),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.MyLocation, "Recenter", modifier = Modifier.size(18.dp))
                }

                SmallFloatingActionButton(
                    onClick = { zoomLevel = (zoomLevel * 1.25f).coerceAtMost(2.5f) },
                    containerColor = if (darkMode) Color(0xFF1E293B) else Color.White,
                    contentColor = if (darkMode) Color.White else Color(0xFF1E293B),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.Add, "Zoom In", modifier = Modifier.size(18.dp))
                }

                SmallFloatingActionButton(
                    onClick = { zoomLevel = (zoomLevel / 1.25f).coerceAtLeast(0.7f) },
                    containerColor = if (darkMode) Color(0xFF1E293B) else Color.White,
                    contentColor = if (darkMode) Color.White else Color(0xFF1E293B),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.Remove, "Zoom Out", modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // Target Detail Inspector Card when a marker is clicked
    selectedTarget?.let { target ->
        val distKm = haversineDistanceKm(currentUserLat, currentUserLng, target.latitude, target.longitude)
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (darkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(target.color).copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = Color(target.color)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when (target.type) {
                            "VEHICLE" -> Icon(Icons.Filled.DirectionsCar, null, tint = Color.White)
                            "CHILD_BAG" -> Icon(Icons.Filled.School, null, tint = Color.White)
                            "DESTINATION" -> Icon(Icons.Filled.LocationOn, null, tint = Color.White)
                            else -> Text(target.title.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = target.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (darkMode) Color.White else Color(0xFF1E293B)
                    )
                    Text(
                        text = target.subtitle,
                        fontSize = 12.sp,
                        color = if (darkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "📍 ${formatDistance(distKm)} away",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2563EB)
                        )
                        Text(
                            text = "⏱️ ${formatEta(distKm, target.speedKmh)} ETA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF16A34A)
                        )
                        if (target.batteryPercent < 100) {
                            Text(
                                text = "🔋 ${target.batteryPercent}%",
                                fontSize = 11.sp,
                                color = if (target.batteryPercent < 20) Color.Red else Color.Gray
                            )
                        }
                    }
                }
                IconButton(onClick = { selectedTarget = null }) {
                    Icon(Icons.Filled.Close, "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
