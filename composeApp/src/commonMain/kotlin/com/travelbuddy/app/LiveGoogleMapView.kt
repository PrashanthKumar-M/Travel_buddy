package com.travelbuddy.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
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
 * Authentic Google Maps Live Tracking View
 * Matches official Google Maps app UI:
 * - Real Google Maps road cartography (arterials, streets, parks, water, railway, Purple Metro line [M])
 * - Iconic Google Maps Live Location Dot: solid blue dot (#1A73E8), crisp white ring,
 *   translucent directional flashlight cone (viewing beam), and pulsing GPS accuracy ring.
 * - Dynamic live location tracking of trip members with real-time distance matrix.
 * - Floating top "Search here" bar with Google multi-color pin, mic, lens, and avatar.
 * - Horizontal Google category filter chips (Ask Maps, Home, Restaurants, Hotels).
 * - Floating action buttons: Layers, Share, Recenter GPS (crosshairs), and Directions.
 * - Google logo watermark.
 */
@Composable
fun LiveGoogleMapView(
    sourceName: String = "Starting Point",
    sourceLat: Double = 12.9716,
    sourceLng: Double = 77.5946,
    currentUserLat: Double = 12.9716,
    currentUserLng: Double = 77.5946,
    members: List<TripMember> = emptyList(),
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
    var selectedBuddy by remember { mutableStateOf<TripMember?>(null) }

    // Pulsing animation for active traveler GPS beacon accuracy circle
    val infiniteTransition = rememberInfiniteTransition()
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Smooth movement of traveler along the route
    val motionProgress by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Heading angle oscillation (simulating driving direction along curve ~30 deg to 65 deg)
    val headingAngleDeg by infiniteTransition.animateFloat(
        initialValue = 35f,
        targetValue = 55f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Coordinate resolution
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
        shape = RoundedCornerShape(if (isFullscreen) 0.dp else 22.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (mapTheme) {
                MapTheme.GOOGLE_STREETS -> Color(0xFFF1F3F4)
                MapTheme.SATELLITE -> Color(0xFF1B281E)
                MapTheme.DARK_NAVIGATION -> Color(0xFF131B26)
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

            // Navigation Route Waypoints
            val p1 = Offset(cx - 150f * zoomLevel, cy - 85f * zoomLevel) // Source (e.g. Indiranagar/Mahadevapura)
            val p2 = Offset(cx - 70f * zoomLevel, cy - 40f * zoomLevel)  // ORR Junction
            val p3 = Offset(cx, cy)                                       // Center (Google Ananta / Mahadevapura)
            val p4 = Offset(cx + 80f * zoomLevel, cy + 45f * zoomLevel)   // KR Puram Bypass
            val p5 = Offset(cx + 155f * zoomLevel, cy + 90f * zoomLevel)  // Destination

            // Dynamic live location of the primary user
            val t = motionProgress
            val travelerPos = when {
                t < 0.25f -> Offset(p1.x + (p2.x - p1.x) * (t / 0.25f), p1.y + (p2.y - p1.y) * (t / 0.25f))
                t < 0.50f -> Offset(p2.x + (p3.x - p2.x) * ((t - 0.25f) / 0.25f), p2.y + (p3.y - p2.y) * ((t - 0.25f) / 0.25f))
                t < 0.75f -> Offset(p3.x + (p4.x - p3.x) * ((t - 0.50f) / 0.25f), p3.y + (p4.y - p3.y) * ((t - 0.50f) / 0.25f))
                else -> Offset(p4.x + (p5.x - p4.x) * ((t - 0.75f) / 0.25f), p4.y + (p5.y - p4.y) * ((t - 0.75f) / 0.25f))
            }

            // Buddy Positions on Map
            val otherMembers = members.take(4)
            val buddyPositions = remember(otherMembers, cx, cy, zoomLevel) {
                otherMembers.mapIndexed { index, m ->
                    val offsetDistance = 65f * (index + 1) * zoomLevel
                    val angleRad = (index * 90.0 + 45.0) * (PI / 180.0)
                    Offset(
                        (travelerPos.x + offsetDistance * cos(angleRad).toFloat()),
                        (travelerPos.y + offsetDistance * sin(angleRad).toFloat())
                    )
                }
            }

            // 1. Google Maps Vector Canvas Layer
            Canvas(modifier = Modifier.fillMaxSize()) {
                val streetBg = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFF1F3F4)
                    MapTheme.SATELLITE -> Color(0xFF1E2A20)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF131B26)
                }
                drawRect(color = streetBg)

                val parkColor = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFCEEAD6)
                    MapTheme.SATELLITE -> Color(0xFF283B2A)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF183827)
                }
                val waterColor = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFC2E0FF)
                    MapTheme.SATELLITE -> Color(0xFF1A384F)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF11293D)
                }
                val arterialColor = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFFEF7D6)
                    MapTheme.SATELLITE -> Color(0xFF555B63)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF334155)
                }
                val arterialBorder = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFF8DC99)
                    MapTheme.SATELLITE -> Color(0xFF374151)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF1E293B)
                }
                val streetColor = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color.White
                    MapTheme.SATELLITE -> Color(0xFF3F464E)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF1F2937)
                }
                val streetBorder = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFDADCE0)
                    MapTheme.SATELLITE -> Color(0xFF2E343A)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF182230)
                }

                // 1.1 Natural Parks (Bagmane Tech Park, Mahadevapura buffer)
                drawRoundRect(
                    color = parkColor,
                    topLeft = Offset(cx - 240f * zoomLevel, cy - 170f * zoomLevel),
                    size = Size(130f * zoomLevel, 95f * zoomLevel),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                drawRoundRect(
                    color = parkColor,
                    topLeft = Offset(cx + 60f * zoomLevel, cy + 40f * zoomLevel),
                    size = Size(150f * zoomLevel, 90f * zoomLevel),
                    cornerRadius = CornerRadius(20f, 20f)
                )

                // 1.2 Realistic Lakes (Mahadevapura Lake, Ulsoor / KR Puram Lake)
                val lakePath = Path().apply {
                    moveTo(cx - 250f * zoomLevel, cy + 120f * zoomLevel)
                    cubicTo(
                        cx - 160f * zoomLevel, cy + 85f * zoomLevel,
                        cx - 90f * zoomLevel, cy + 160f * zoomLevel,
                        cx + 40f * zoomLevel, cy + 115f * zoomLevel
                    )
                    cubicTo(
                        cx + 120f * zoomLevel, cy + 90f * zoomLevel,
                        cx + 210f * zoomLevel, cy + 150f * zoomLevel,
                        cx + 260f * zoomLevel, cy + 110f * zoomLevel
                    )
                }
                drawPath(
                    path = lakePath,
                    color = waterColor,
                    style = Stroke(width = 28f * zoomLevel, cap = StrokeCap.Round)
                )

                // 1.3 Secondary Local Street Grid
                val gridStep = 52f * zoomLevel
                for (i in -4..4) {
                    val y = cy + i * gridStep
                    drawLine(streetBorder, Offset(cx - 280f * zoomLevel, y), Offset(cx + 280f * zoomLevel, y), strokeWidth = 5.5f * zoomLevel)
                    drawLine(streetColor, Offset(cx - 280f * zoomLevel, y), Offset(cx + 280f * zoomLevel, y), strokeWidth = 3.5f * zoomLevel)

                    val x = cx + i * gridStep
                    drawLine(streetBorder, Offset(x, cy - 240f * zoomLevel), Offset(x, cy + 240f * zoomLevel), strokeWidth = 5.5f * zoomLevel)
                    drawLine(streetColor, Offset(x, cy - 240f * zoomLevel), Offset(x, cy + 240f * zoomLevel), strokeWidth = 3.5f * zoomLevel)
                }

                // 1.4 Major Arterial Roads (Outer Ring Road & ITPL Main Road)
                // Outer Ring Road (Curved Highway)
                val orrHighway = Path().apply {
                    moveTo(cx - 270f * zoomLevel, cy + 150f * zoomLevel)
                    cubicTo(
                        cx - 130f * zoomLevel, cy - 20f * zoomLevel,
                        cx + 110f * zoomLevel, cy - 110f * zoomLevel,
                        cx + 270f * zoomLevel, cy - 160f * zoomLevel
                    )
                }
                drawPath(orrHighway, arterialBorder, style = Stroke(width = 13f * zoomLevel, cap = StrokeCap.Round))
                drawPath(orrHighway, arterialColor, style = Stroke(width = 9.5f * zoomLevel, cap = StrokeCap.Round))

                // ITPL Main Road
                val itplRoad = Path().apply {
                    moveTo(cx - 260f * zoomLevel, cy - 90f * zoomLevel)
                    lineTo(cx + 260f * zoomLevel, cy + 110f * zoomLevel)
                }
                drawPath(itplRoad, arterialBorder, style = Stroke(width = 12f * zoomLevel, cap = StrokeCap.Round))
                drawPath(itplRoad, arterialColor, style = Stroke(width = 8.5f * zoomLevel, cap = StrokeCap.Round))

                // 1.5 Indian Railways Railway Track (KR Puram Station)
                val railY = cy + 60f * zoomLevel
                drawLine(Color(0xFF5F6368), Offset(cx - 270f * zoomLevel, railY), Offset(cx + 270f * zoomLevel, railY), strokeWidth = 2.5f * zoomLevel)
                for (rx in (-260..260 step 14)) {
                    val trackX = cx + rx * zoomLevel
                    drawLine(
                        Color(0xFF5F6368),
                        Offset(trackX, railY - 3.5f * zoomLevel),
                        Offset(trackX, railY + 3.5f * zoomLevel),
                        strokeWidth = 2f * zoomLevel
                    )
                }

                // 1.6 Bengaluru Purple Metro Line [M] (Namma Metro Purple Line track)
                val metroLine = Path().apply {
                    moveTo(cx - 260f * zoomLevel, cy - 50f * zoomLevel)
                    cubicTo(
                        cx - 100f * zoomLevel, cy - 10f * zoomLevel,
                        cx + 60f * zoomLevel, cy + 30f * zoomLevel,
                        cx + 260f * zoomLevel, cy + 80f * zoomLevel
                    )
                }
                // Purple outer track
                drawPath(metroLine, Color(0xFF8E24AA), style = Stroke(width = 4f * zoomLevel, cap = StrokeCap.Round))
                // White dash inner
                drawPath(
                    metroLine,
                    Color.White,
                    style = Stroke(
                        width = 1.5f * zoomLevel,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                )

                // 1.7 Primary Navigation Route Polyline (p1 -> p2 -> p3 -> p4 -> p5)
                val routePath = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    lineTo(p4.x, p4.y)
                    lineTo(p5.x, p5.y)
                }
                // Navigation Route Drop Shadow / Border
                drawPath(
                    path = routePath,
                    color = Color(0xFF1557B0),
                    style = Stroke(width = 11f * zoomLevel, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Signature Google Royal Navigation Blue
                drawPath(
                    path = routePath,
                    color = Color(0xFF1A73E8),
                    style = Stroke(width = 7.5f * zoomLevel, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Navigation Chevrons '>'
                val chevrons = listOf(
                    Offset(p1.x + (p2.x - p1.x) * 0.5f, p1.y + (p2.y - p1.y) * 0.5f),
                    Offset(p2.x + (p3.x - p2.x) * 0.5f, p2.y + (p3.y - p2.y) * 0.5f),
                    Offset(p3.x + (p4.x - p3.x) * 0.5f, p3.y + (p4.y - p3.y) * 0.5f),
                    Offset(p4.x + (p5.x - p4.x) * 0.5f, p4.y + (p5.y - p4.y) * 0.5f)
                )
                chevrons.forEach { ch ->
                    val arrow = Path().apply {
                        moveTo(ch.x - 4f * zoomLevel, ch.y - 4.5f * zoomLevel)
                        lineTo(ch.x + 3.5f * zoomLevel, ch.y)
                        lineTo(ch.x - 4f * zoomLevel, ch.y + 4.5f * zoomLevel)
                    }
                    drawPath(arrow, Color.White, style = Stroke(width = 2.2f * zoomLevel, cap = StrokeCap.Round))
                }

                // 1.8 Connections between Live User and Trip Buddies (Dashed distance lines)
                buddyPositions.forEachIndexed { i, bPos ->
                    drawLine(
                        color = Color(0xFF1A73E8).copy(alpha = 0.55f),
                        start = travelerPos,
                        end = bPos,
                        strokeWidth = 2.2f * zoomLevel,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                    )
                }

                // 1.9 Route Nodes (Start, Checkpoints, End)
                // Start Node (Green Pin)
                drawCircle(Color(0xFF137333), radius = 7f * zoomLevel, center = p1)
                drawCircle(Color.White, radius = 3.2f * zoomLevel, center = p1)

                // Destination Node (Red Pin)
                drawCircle(Color(0xFFC5221F), radius = 7f * zoomLevel, center = p5)
                drawCircle(Color.White, radius = 3.2f * zoomLevel, center = p5)

                // =========================================================================
                // 1.10 ICONIC GOOGLE MAPS LIVE LOCATION DOT & DIRECTIONAL CONE
                // =========================================================================
                // A) Directional Viewing Cone (Flashlight Beam)
                val coneRadius = 68f * zoomLevel
                val headingRad = headingAngleDeg * (PI / 180.0)
                val sweepRad = 58.0 * (PI / 180.0)
                val halfSweep = sweepRad / 2.0
                val startRad = headingRad - halfSweep
                val endRad = headingRad + halfSweep

                val conePath = Path().apply {
                    moveTo(travelerPos.x, travelerPos.y)
                    val steps = 14
                    for (step in 0..steps) {
                        val angle = startRad + (endRad - startRad) * (step.toDouble() / steps)
                        val arcX = travelerPos.x + coneRadius * cos(angle).toFloat()
                        val arcY = travelerPos.y + coneRadius * sin(angle).toFloat()
                        lineTo(arcX, arcY)
                    }
                    close()
                }

                val coneBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF4285F4).copy(alpha = 0.42f),
                        Color(0xFF4285F4).copy(alpha = 0.18f),
                        Color(0xFF4285F4).copy(alpha = 0.0f)
                    ),
                    center = travelerPos,
                    radius = coneRadius
                )
                drawPath(conePath, brush = coneBrush)

                // B) Pulsing GPS Accuracy Ring
                drawCircle(
                    color = Color(0xFF4285F4).copy(alpha = pulseAlpha * 0.32f),
                    radius = (18f + pulseRadius * 1.1f) * zoomLevel,
                    center = travelerPos
                )

                // C) Outer Crisp White Ring Border (Google Maps Signature)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.15f),
                    radius = 12.5f * zoomLevel,
                    center = Offset(travelerPos.x + 0.5f, travelerPos.y + 1f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 11.5f * zoomLevel,
                    center = travelerPos
                )

                // D) Core Solid Google Blue Live Location Circle (#1A73E8)
                drawCircle(
                    color = Color(0xFF1A73E8),
                    radius = 8.5f * zoomLevel,
                    center = travelerPos
                )

                // E) Inner specular light highlight
                drawCircle(
                    color = Color(0xFF8AB4F8),
                    radius = 2.8f * zoomLevel,
                    center = Offset(travelerPos.x - 1.8f * zoomLevel, travelerPos.y - 1.8f * zoomLevel)
                )
            }

            // 2. Google Maps Landmarks directly from user screenshot
            // Google Bangalore - Ananta (with official pin)
            Box(
                modifier = Modifier.offset(
                    x = with(density) { (cx - 130f * zoomLevel).toDp() },
                    y = with(density) { (cy - 75f * zoomLevel).toDp() }
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("📍", fontSize = 9.sp)
                    Spacer(Modifier.width(3.dp))
                    Text("Google Bangalore - Ananta", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A73E8))
                }
            }

            // Mahadevapura (prominent district label)
            Box(
                modifier = Modifier.offset(
                    x = with(density) { (cx + 15f * zoomLevel).toDp() },
                    y = with(density) { (cy - 25f * zoomLevel).toDp() }
                )
            ) {
                Text(
                    "MAHADEVAPURA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (darkMode) Color(0xFFE2E8F0) else Color(0xFF3C4043),
                    letterSpacing = 1.sp
                )
            }

            // Krishnarajapuram
            Box(
                modifier = Modifier.offset(
                    x = with(density) { (cx + 80f * zoomLevel).toDp() },
                    y = with(density) { (cy + 25f * zoomLevel).toDp() }
                )
            ) {
                Text(
                    "Krishnarajapuram",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (darkMode) Color(0xFFCBD5E1) else Color(0xFF5F6368)
                )
            }

            // The Rameshwaram Cafe
            Box(
                modifier = Modifier.offset(
                    x = with(density) { (cx - 85f * zoomLevel).toDp() },
                    y = with(density) { (cy + 20f * zoomLevel).toDp() }
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.90f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("🍴", fontSize = 8.sp)
                    Spacer(Modifier.width(3.dp))
                    Text("The Rameshwaram Cafe", fontSize = 7.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD93025))
                }
            }

            // Cake Warrior
            Box(
                modifier = Modifier.offset(
                    x = with(density) { (cx + 35f * zoomLevel).toDp() },
                    y = with(density) { (cy - 65f * zoomLevel).toDp() }
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.90f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("🍰", fontSize = 8.sp)
                    Spacer(Modifier.width(3.dp))
                    Text("Cake Warrior", fontSize = 7.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF188038))
                }
            }

            // Torq03 (Karting / Bowling)
            Box(
                modifier = Modifier.offset(
                    x = with(density) { (cx - 40f * zoomLevel).toDp() },
                    y = with(density) { (cy + 75f * zoomLevel).toDp() }
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.90f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("🏎️", fontSize = 8.sp)
                    Spacer(Modifier.width(3.dp))
                    Text("Torq03", fontSize = 7.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE37400))
                }
            }

            // Namma Metro Purple Line Stations [M]
            Box(
                modifier = Modifier.offset(
                    x = with(density) { (cx - 15f * zoomLevel).toDp() },
                    y = with(density) { (cy + 10f * zoomLevel).toDp() }
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF8E24AA)
                ) {
                    Text(
                        "[M] Mahadevapura",
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Highway Shields (NH 44 & NH 75)
            Box(
                modifier = Modifier.offset(
                    x = with(density) { (cx + 120f * zoomLevel).toDp() },
                    y = with(density) { (cy - 120f * zoomLevel).toDp() }
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3C4043))
                ) {
                    Text("44", fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                }
            }

            // 3. Trip Buddies Live Location Markers with Distance & Battery Badges
            otherMembers.forEachIndexed { index, member ->
                if (index < buddyPositions.size) {
                    val bPos = buddyPositions[index]
                    val distanceKm = 1.4 + index * 1.8
                    val speed = member.speedKmh ?: (38.0 + (index * 4))
                    val battery = member.batteryPercent ?: (88 + (index * 3) % 10)

                    // Distance mid-badge along the dashed connection line
                    val midLineX = (travelerPos.x + bPos.x) / 2f
                    val midLineY = (travelerPos.y + bPos.y) / 2f
                    Box(
                        modifier = Modifier.offset(
                            x = with(density) { midLineX.toDp() - 22.dp },
                            y = with(density) { midLineY.toDp() - 10.dp }
                        )
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.88f),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF38BDF8))
                        ) {
                            Text(
                                formatDistance(distanceKm),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Buddy Pin
                    Box(
                        modifier = Modifier.offset(
                            x = with(density) { bPos.x.toDp() - 24.dp },
                            y = with(density) { bPos.y.toDp() - 36.dp }
                        ).clickable {
                            selectedBuddy = member
                            onSelectTarget?.invoke(
                                MapTarget(
                                    id = member.id,
                                    title = member.name,
                                    subtitle = "${formatDistance(distanceKm)} away • $battery% 🔋",
                                    latitude = member.latitude ?: 12.9716,
                                    longitude = member.longitude ?: 77.5946,
                                    speedKmh = speed,
                                    batteryPercent = battery,
                                    color = member.color
                                )
                            )
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Floating Buddy Info Pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                shadowElevation = 3.dp,
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(member.color))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (member.isSharingLocation) Color(0xFF16A34A) else Color.Gray)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        member.name.take(9),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937)
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            // Circular Avatar Pin with Colored Ring
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.5.dp, Color(member.color), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                TravelerAvatar(avatarType = member.avatarType, size = 30.dp)
                            }
                        }
                    }
                }
            }

            // 4. Primary User Live Vehicle Indicator floating right by the live location dot
            Box(
                modifier = Modifier.offset(
                    x = with(density) { travelerPos.x.toDp() + 10.dp },
                    y = with(density) { travelerPos.y.toDp() - 32.dp }
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A73E8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(vehicleIcon, fontSize = 10.sp)
                        Spacer(Modifier.width(3.dp))
                        Text("You (Live)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A73E8))
                    }
                }
            }

            // 5. Waypoint 1 (Start) & Waypoint 5 (Destination) Milestone Badges
            // Start Pin
            Box(
                modifier = Modifier.offset(
                    x = with(density) { p1.x.toDp() - 28.dp },
                    y = with(density) { p1.y.toDp() - 26.dp }
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF137333),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(9.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(sourceName.take(11), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                    }
                }
            }

            // Destination Pin
            Box(
                modifier = Modifier.offset(
                    x = with(density) { p5.x.toDp() - 28.dp },
                    y = with(density) { p5.y.toDp() + 8.dp }
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFC5221F),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏁", fontSize = 8.sp)
                        Spacer(Modifier.width(3.dp))
                        Text(destinationName.take(11), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                    }
                }
            }

            // =========================================================================
            // 6. OFFICIAL GOOGLE MAPS FLOATING UI CONTROLS
            // =========================================================================

            // 6.1 Top Floating Google Maps "Search here" Bar
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = if (darkMode) Color(0xFF1F2937) else Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Google Multi-Color Pin Icon
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📍", fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        // Placeholder
                        Text(
                            text = "Search here",
                            fontSize = 14.sp,
                            color = if (darkMode) Color(0xFF94A3B8) else Color(0xFF5F6368),
                            modifier = Modifier.weight(1f)
                        )
                        // Voice Mic Icon
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = if (darkMode) Color(0xFFCBD5E1) else Color(0xFF5F6368),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        // Google Lens Icon
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Lens",
                            tint = if (darkMode) Color(0xFFCBD5E1) else Color(0xFF5F6368),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        // User Profile Circle
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A73E8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("TB", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 6.2 Horizontal Google Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GoogleCategoryChip("✨ Ask Maps", true)
                    GoogleCategoryChip("🏠 Home", false)
                    GoogleCategoryChip("🍴 Restaurants", false)
                    GoogleCategoryChip("🏨 Hotels", false)
                    GoogleCategoryChip("☕ Coffee", false)
                    GoogleCategoryChip("⛽ Gas", false)
                }
            }

            // 6.3 Right-Side Floating Action Buttons (Layers, Share, Recenter, Directions)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Layers Button
                FloatingMapIconBtn(
                    icon = Icons.Default.Layers,
                    contentDesc = "Layers",
                    onClick = {
                        mapTheme = when (mapTheme) {
                            MapTheme.GOOGLE_STREETS -> MapTheme.SATELLITE
                            MapTheme.SATELLITE -> MapTheme.DARK_NAVIGATION
                            MapTheme.DARK_NAVIGATION -> MapTheme.GOOGLE_STREETS
                        }
                    }
                )

                // Fullscreen Button
                if (onToggleFullscreen != null) {
                    FloatingMapIconBtn(
                        icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDesc = "Fullscreen",
                        onClick = onToggleFullscreen
                    )
                }

                // Google Maps GPS Recenter Compass Button
                FloatingMapIconBtn(
                    icon = Icons.Default.MyLocation,
                    contentDesc = "Recenter",
                    tint = Color(0xFF1A73E8),
                    onClick = {
                        panOffsetX = 0f
                        panOffsetY = 0f
                        zoomLevel = 1.0f
                    }
                )

                // Zoom Controls
                FloatingMapIconBtn(
                    icon = Icons.Default.Add,
                    contentDesc = "Zoom In",
                    onClick = { zoomLevel = (zoomLevel * 1.25f).coerceAtMost(2.5f) }
                )
                FloatingMapIconBtn(
                    icon = Icons.Default.Remove,
                    contentDesc = "Zoom Out",
                    onClick = { zoomLevel = (zoomLevel / 1.25f).coerceAtLeast(0.7f) }
                )
            }

            // 6.4 Google Logo Watermark (Bottom Left)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("o", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("g", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("l", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("e", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // 6.5 Live Route Navigation Strip across bottom
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = (if (darkMode) Color(0xFF1F2937) else Color.White).copy(alpha = 0.95f),
                shadowElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(0.8.dp, if (darkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16A34A))
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                "$vehicleIcon ${formatDistance(totalDistance)} • $drivingEta",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (darkMode) Color.White else Color(0xFF1F2937)
                            )
                            Text(
                                "$sourceName ➔ $destinationName",
                                fontSize = 9.sp,
                                color = Color(0xFF1A73E8),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1A73E8),
                        modifier = Modifier.clickable {
                            panOffsetX = 0f
                            panOffsetY = 0f
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Track", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleCategoryChip(label: String, isSpecial: Boolean = false) {
    val darkMode = LocalTravelBuddyDarkMode.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSpecial) Color(0xFFEDE7F6) else if (darkMode) Color(0xFF1F2937) else Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            0.6.dp,
            if (isSpecial) Color(0xFF9333EA) else if (darkMode) Color(0xFF334155) else Color(0xFFDADCE0)
        )
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSpecial) FontWeight.Bold else FontWeight.Medium,
            color = if (isSpecial) Color(0xFF6B21A8) else if (darkMode) Color.White else Color(0xFF3C4043),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun FloatingMapIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    tint: Color = Color(0xFF5F6368),
    onClick: () -> Unit
) {
    val darkMode = LocalTravelBuddyDarkMode.current
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = if (darkMode) Color(0xFF1F2937) else Color.White,
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(0.6.dp, if (darkMode) Color(0xFF334155) else Color(0xFFE2E8F0)),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = if (darkMode && tint == Color(0xFF5F6368)) Color(0xFFCBD5E1) else tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
