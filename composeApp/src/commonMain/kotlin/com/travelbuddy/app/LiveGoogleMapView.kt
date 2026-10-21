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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
 * Realistic Live Google Map Navigation matching professional field tracking apps:
 * - Clean urban road network and waterway
 * - Signature blue navigation polyline with directional chevrons '>'
 * - Waypoint milestone badges (Completed ✓, In Progress 🔵 with pulse/avatar, Pending 🕒)
 * - Animated traveler endpoint movement
 * - Vehicle indicator (Car 🚗, Bike 🏍️, Cycle 🚲)
 * - Fullscreen toggle and interactive zoom
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
    vehicleType: String = "CAR",
    childBagDevice: MapTarget? = null,
    destinationName: String = "Coorg Valley",
    destinationLat: Double = 12.3375,
    destinationLng: Double = 75.8069,
    isLiveUsersMode: Boolean = true,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxWidth().height(340.dp),
    onSelectTarget: ((MapTarget) -> Unit)? = null
) {
    val darkMode = LocalTravelBuddyDarkMode.current
    var mapTheme by remember { mutableStateOf(if (darkMode) MapTheme.DARK_NAVIGATION else MapTheme.GOOGLE_STREETS) }
    var zoomLevel by remember { mutableStateOf(1.0f) }
    var panOffsetX by remember { mutableStateOf(0f) }
    var panOffsetY by remember { mutableStateOf(0f) }
    var selectedTarget by remember { mutableStateOf<MapTarget?>(null) }

    // Pulsing animation for active traveler GPS beacon
    val infiniteTransition = rememberInfiniteTransition()
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 36f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Smooth subtle movement of the endpoint along the route
    val motionProgress by infiniteTransition.animateFloat(
        initialValue = 0.44f,
        targetValue = 0.54f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Dynamic Geographic Coordinate Resolution
    val (resolvedSrcLat, resolvedSrcLng) = remember(sourceName, sourceLat, sourceLng) {
        LocationCoordinateResolver.resolveCoordinates(sourceName, sourceLat, sourceLng)
    }
    val (resolvedDstLat, resolvedDstLng) = remember(destinationName, destinationLat, destinationLng) {
        LocationCoordinateResolver.resolveCoordinates(destinationName, destinationLat, destinationLng)
    }

    val totalDistance = remember(sourceName, destinationName, resolvedSrcLat, resolvedDstLat) {
        LocationCoordinateResolver.calculateRoadDistanceKm(sourceName, destinationName)
    }
    val drivingEta = remember(totalDistance) {
        LocationCoordinateResolver.calculateDrivingEta(totalDistance)
    }

    val vehicleIcon = when (vehicleType.uppercase()) {
        "BIKE" -> "🏍️"
        "CYCLE" -> "🚲"
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
        shape = RoundedCornerShape(if (isFullscreen) 0.dp else 22.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (mapTheme) {
                MapTheme.GOOGLE_STREETS -> Color(0xFFF4F6F8)
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
            // Realistic Map Canvas (Canvas Layer)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2 + panOffsetX
                val cy = size.height / 2 + panOffsetY

                val streetBg = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFF3F5F7)
                    MapTheme.SATELLITE -> Color(0xFF1B261D)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF131B26)
                }
                drawRect(color = streetBg)

                val parkColor = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFE2F4E0)
                    MapTheme.SATELLITE -> Color(0xFF233824)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF1B3828)
                }
                val waterColor = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFD3E7FA)
                    MapTheme.SATELLITE -> Color(0xFF183B54)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF142E46)
                }
                val roadColor = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color.White
                    MapTheme.SATELLITE -> Color(0xFF4A5568)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF253342)
                }
                val roadBorder = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFE2E8F0)
                    MapTheme.SATELLITE -> Color(0xFF2D3748)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF1E293B)
                }

                // 1. Natural Parks & Green zones
                drawRoundRect(
                    color = parkColor,
                    topLeft = Offset(cx - 200f * zoomLevel, cy - 140f * zoomLevel),
                    size = Size(110f * zoomLevel, 90f * zoomLevel),
                    cornerRadius = CornerRadius(20f, 20f)
                )
                drawRoundRect(
                    color = parkColor,
                    topLeft = Offset(cx + 40f * zoomLevel, cy + 30f * zoomLevel),
                    size = Size(130f * zoomLevel, 80f * zoomLevel),
                    cornerRadius = CornerRadius(24f, 24f)
                )

                // 2. Realistic River / Canal running diagonally
                val river = Path().apply {
                    moveTo(cx - 220f * zoomLevel, cy + 130f * zoomLevel)
                    cubicTo(
                        cx - 80f * zoomLevel, cy + 80f * zoomLevel,
                        cx + 30f * zoomLevel, cy + 160f * zoomLevel,
                        cx + 220f * zoomLevel, cy + 110f * zoomLevel
                    )
                }
                drawPath(river, waterColor, style = Stroke(width = 24f * zoomLevel, cap = androidx.compose.ui.graphics.StrokeCap.Round))

                // 3. Grid street network
                val step = 55f * zoomLevel
                for (i in -4..4) {
                    drawLine(roadBorder, Offset(cx - 240f * zoomLevel, cy + i * step), Offset(cx + 240f * zoomLevel, cy + i * step), strokeWidth = 5f * zoomLevel)
                    drawLine(roadColor, Offset(cx - 240f * zoomLevel, cy + i * step), Offset(cx + 240f * zoomLevel, cy + i * step), strokeWidth = 3f * zoomLevel)
                    drawLine(roadBorder, Offset(cx + i * step, cy - 200f * zoomLevel), Offset(cx + i * step, cy + 200f * zoomLevel), strokeWidth = 5f * zoomLevel)
                    drawLine(roadColor, Offset(cx + i * step, cy - 200f * zoomLevel), Offset(cx + i * step, cy + 200f * zoomLevel), strokeWidth = 3f * zoomLevel)
                }

                // 4. Primary Route Polyline (From Top-Left to Bottom-Right matching attached image)
                val p1 = Offset(cx - 130f * zoomLevel, cy - 110f * zoomLevel)
                val p2 = Offset(cx - 70f * zoomLevel, cy - 50f * zoomLevel)
                val p3 = Offset(cx - 10f * zoomLevel, cy + 10f * zoomLevel)
                val p4 = Offset(cx + 40f * zoomLevel, cy + 60f * zoomLevel)
                val p5 = Offset(cx + 90f * zoomLevel, cy + 120f * zoomLevel)

                val routePath = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p1.x + 20f * zoomLevel, p1.y + 40f * zoomLevel)
                    lineTo(p2.x, p2.y)
                    lineTo(p2.x + 25f * zoomLevel, p2.y + 35f * zoomLevel)
                    lineTo(p3.x, p3.y)
                    lineTo(p3.x + 20f * zoomLevel, p3.y + 30f * zoomLevel)
                    lineTo(p4.x, p4.y)
                    lineTo(p4.x + 20f * zoomLevel, p4.y + 35f * zoomLevel)
                    lineTo(p5.x, p5.y)
                }

                // Outer border of navigation line
                drawPath(
                    path = routePath,
                    color = Color(0xFF1557B0),
                    style = Stroke(width = 10f * zoomLevel, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                )
                // Vibrant Royal Blue core line
                drawPath(
                    path = routePath,
                    color = Color(0xFF1D64F2),
                    style = Stroke(width = 6f * zoomLevel, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                )

                // Directional Chevrons '>' along the route
                val chevrons = listOf(
                    Offset(cx - 105f * zoomLevel, cy - 80f * zoomLevel),
                    Offset(cx - 45f * zoomLevel, cy - 20f * zoomLevel),
                    Offset(cx + 15f * zoomLevel, cy + 35f * zoomLevel),
                    Offset(cx + 65f * zoomLevel, cy + 90f * zoomLevel)
                )
                chevrons.forEach { ch ->
                    val arrow = Path().apply {
                        moveTo(ch.x - 4f * zoomLevel, ch.y - 5f * zoomLevel)
                        lineTo(ch.x + 3f * zoomLevel, ch.y)
                        lineTo(ch.x - 4f * zoomLevel, ch.y + 5f * zoomLevel)
                    }
                    drawPath(arrow, Color.White, style = Stroke(width = 2.5f * zoomLevel, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }

                // Pulsing beacon at active traveler position
                val travelerX = cx + (-10f + (motionProgress - 0.5f) * 30f) * zoomLevel
                val travelerY = cy + (10f + (motionProgress - 0.5f) * 30f) * zoomLevel
                drawCircle(
                    color = Color(0xFF1D64F2).copy(alpha = pulseAlpha),
                    radius = pulseRadius * zoomLevel,
                    center = Offset(travelerX, travelerY)
                )
            }

            // Real-time Waypoint & Traveler Badges (UI Layer matching attached image)
            val cx = panOffsetX
            val cy = panOffsetY

            // 1. Waypoint 1: Starting Point (Completed ✓)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (cx - 130f * zoomLevel).dp, y = (cy - 110f * zoomLevel).dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF16A34A),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("1. $sourceName", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                            Text("10:30 AM", fontSize = 9.sp, color = Color(0xFF64748B))
                            Text("Completed", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                    }
                }
            }

            // 2. Waypoint 2: Highway Checkpoint (Completed ✓)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (cx - 70f * zoomLevel).dp, y = (cy - 50f * zoomLevel).dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF16A34A),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("2. Route Checkpoint", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                            Text("11:30 AM", fontSize = 9.sp, color = Color(0xFF64748B))
                            Text("Completed", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                    }
                }
            }

            // 3. Waypoint 3: Active Traveler (You) (In Progress 🔵 with Avatar)
            val travelerOffset = (motionProgress - 0.5f) * 30f
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (cx - 10f * zoomLevel + travelerOffset).dp, y = (cy + 10f * zoomLevel + travelerOffset).dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1D64F2),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(34.dp)
                        ) {
                            TravelerAvatar(avatarType = "MALE_1", size = 30.dp)
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1D64F2))
                    ) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            val activeName = members.firstOrNull()?.name ?: "You"
                            Text("3. $activeName", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1D64F2))
                            Text("45 km/h • 01:18 PM", fontSize = 9.sp, color = Color(0xFF64748B))
                            Text("In Progress", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D64F2))
                        }
                    }
                }
            }

            // 4. Waypoint 4: Rest / Fuel Stop (Pending 🕒)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (cx + 40f * zoomLevel).dp, y = (cy + 60f * zoomLevel).dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEA580C),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("4. Rest Stop & Refuel", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                            Text("02:30 PM", fontSize = 9.sp, color = Color(0xFF64748B))
                            Text("Pending", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                        }
                    }
                }
            }

            // 5. Waypoint 5: Final Destination (Pending 🕒)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (cx + 90f * zoomLevel).dp, y = (cy + 120f * zoomLevel).dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEA580C),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("5. $destinationName", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                            Text("03:30 PM", fontSize = 9.sp, color = Color(0xFF64748B))
                            Text("Pending", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                        }
                    }
                }
            }

            // Top-left: Google Maps Route Banner & Vehicle Indicator
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
                            text = "$vehicleIcon Route • ${formatDistance(totalDistance)} ($drivingEta)",
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

            // Top-right: Map Controls (Fullscreen + Satellite)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                shape = RoundedCornerShape(12.dp),
                color = (if (darkMode) Color(0xFF1E293B) else Color.White).copy(alpha = 0.92f),
                shadowElevation = 3.dp
            ) {
                Row(modifier = Modifier.padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onToggleFullscreen != null) {
                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            mapTheme = if (mapTheme == MapTheme.GOOGLE_STREETS) MapTheme.SATELLITE else MapTheme.GOOGLE_STREETS
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (mapTheme == MapTheme.SATELLITE) Icons.Filled.Public else Icons.Filled.Map,
                            contentDescription = "Theme",
                            tint = Color(0xFF2563EB),
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
}
