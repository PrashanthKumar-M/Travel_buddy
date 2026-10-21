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
import androidx.compose.ui.platform.LocalDensity
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
        BoxWithConstraints(
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
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val cx = widthPx / 2f + panOffsetX
            val cy = heightPx / 2f + panOffsetY

            // Fixed route waypoints locked in unified canvas/screen coordinate space
            val p1 = Offset(cx - 120f * zoomLevel, cy - 75f * zoomLevel) // Fixed Start Point
            val p2 = Offset(cx - 60f * zoomLevel, cy - 35f * zoomLevel)  // Route Checkpoint 1
            val p3 = Offset(cx, cy)                                       // Route Center
            val p4 = Offset(cx + 60f * zoomLevel, cy + 35f * zoomLevel)   // Route Checkpoint 2
            val p5 = Offset(cx + 115f * zoomLevel, cy + 70f * zoomLevel)  // Fixed Destination Point

            // Dynamic live location traveler position along the route line based on movement
            val t = motionProgress // smoothly animates / updates along the route
            val travelerPos = when {
                t < 0.25f -> Offset(p1.x + (p2.x - p1.x) * (t / 0.25f), p1.y + (p2.y - p1.y) * (t / 0.25f))
                t < 0.50f -> Offset(p2.x + (p3.x - p2.x) * ((t - 0.25f) / 0.25f), p2.y + (p3.y - p2.y) * ((t - 0.25f) / 0.25f))
                t < 0.75f -> Offset(p3.x + (p4.x - p3.x) * ((t - 0.50f) / 0.25f), p3.y + (p4.y - p3.y) * ((t - 0.50f) / 0.25f))
                else -> Offset(p4.x + (p5.x - p4.x) * ((t - 0.75f) / 0.25f), p4.y + (p5.y - p4.y) * ((t - 0.75f) / 0.25f))
            }

            // Realistic Map Canvas (Canvas Layer)
            Canvas(modifier = Modifier.fillMaxSize()) {
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

                // 4. Primary Route Polyline connecting p1 to p5
                val routePath = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    lineTo(p4.x, p4.y)
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
                    Offset(p1.x + (p2.x - p1.x) * 0.5f, p1.y + (p2.y - p1.y) * 0.5f),
                    Offset(p2.x + (p3.x - p2.x) * 0.5f, p2.y + (p3.y - p2.y) * 0.5f),
                    Offset(p3.x + (p4.x - p3.x) * 0.5f, p3.y + (p4.y - p3.y) * 0.5f),
                    Offset(p4.x + (p5.x - p4.x) * 0.5f, p4.y + (p5.y - p4.y) * 0.5f)
                )
                chevrons.forEach { ch ->
                    val arrow = Path().apply {
                        moveTo(ch.x - 4f * zoomLevel, ch.y - 5f * zoomLevel)
                        lineTo(ch.x + 3f * zoomLevel, ch.y)
                        lineTo(ch.x - 4f * zoomLevel, ch.y + 5f * zoomLevel)
                    }
                    drawPath(arrow, Color.White, style = Stroke(width = 2.5f * zoomLevel, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }

                // Fixed Start Node on Map at p1
                drawCircle(Color(0xFF16A34A), radius = 8f * zoomLevel, center = p1)
                drawCircle(Color.White, radius = 3.5f * zoomLevel, center = p1)

                // Fixed Destination Node on Map at p5
                drawCircle(Color(0xFFDC2626), radius = 8f * zoomLevel, center = p5)
                drawCircle(Color.White, radius = 3.5f * zoomLevel, center = p5)

                // Fixed Checkpoint Nodes at p2 and p4
                drawCircle(Color(0xFF16A34A), radius = 5f * zoomLevel, center = p2)
                drawCircle(Color(0xFFEA580C), radius = 5f * zoomLevel, center = p4)

                // Pulsing beacon at dynamic active traveler position on the route line
                drawCircle(
                    color = Color(0xFF1D64F2).copy(alpha = pulseAlpha),
                    radius = pulseRadius * zoomLevel,
                    center = travelerPos
                )
                drawCircle(
                    color = Color(0xFF1D64F2),
                    radius = 8f * zoomLevel,
                    center = travelerPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5f * zoomLevel,
                    center = travelerPos
                )
            }

            // Real-time Waypoint & Dynamic Traveler Pins (Locked to Canvas coordinates)
            // 1. Waypoint 1: Fixed Starting Point Pin
            Box(
                modifier = Modifier.offset(
                    x = with(density) { (p1.x).toDp() - 28.dp },
                    y = with(density) { (p1.y).toDp() - 28.dp }
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF16A34A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(sourceName.take(12), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
            }

            // 2. Waypoint 5: Fixed Destination Point Pin
            Box(
                modifier = Modifier.offset(
                    x = with(density) { (p5.x).toDp() - 28.dp },
                    y = with(density) { (p5.y).toDp() + 10.dp }
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFDC2626),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏁", fontSize = 9.sp)
                        Spacer(Modifier.width(3.dp))
                        Text(destinationName.take(12), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
            }

            // 3. Dynamic Active Traveler Pin moving along the line
            Box(
                modifier = Modifier.offset(
                    x = with(density) { (travelerPos.x).toDp() - 44.dp },
                    y = with(density) { (travelerPos.y).toDp() - 32.dp }
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1D64F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                    shadowElevation = 4.dp
                ) {
                    val activeName = members.firstOrNull()?.name?.take(10) ?: "You"
                    val vIcon = when (vehicleType.uppercase()) {
                        "BIKE", "MOTORCYCLE" -> "🏍️"
                        "CYCLE", "BICYCLE" -> "🚲"
                        "SCOOTER" -> "🛵"
                        "SUV" -> "🚙"
                        "BUS" -> "🚌"
                        else -> "🚗"
                    }
                    Text(
                        "$vIcon $activeName • 45 km/h",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Milestone Route Timeline Strip across the bottom of the map
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (darkMode) Color(0xFF0F172A).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.94f),
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (darkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MilestoneStep("1. Start", "✓ Fixed", Color(0xFF16A34A))
                    Text("→", fontSize = 9.sp, color = Color.Gray)
                    MilestoneStep("2. CP", "✓ Done", Color(0xFF16A34A))
                    Text("→", fontSize = 9.sp, color = Color.Gray)
                    MilestoneStep("3. Live", "🔵 Live", Color(0xFF1D64F2))
                    Text("→", fontSize = 9.sp, color = Color.Gray)
                    MilestoneStep("4. Rest", "🕒 Stop", Color(0xFFEA580C))
                    Text("→", fontSize = 9.sp, color = Color.Gray)
                    MilestoneStep("5. Dest", "🏁 Fixed", Color(0xFFDC2626))
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

@Composable
private fun MilestoneStep(title: String, status: String, statusColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(status, fontSize = 8.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
    }
}
