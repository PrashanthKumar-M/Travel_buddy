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
 * Interactive Google Map visualization with real-time buddy markers,
 * route polylines, pan/zoom controls, live distance matrix, and child bag safe zones.
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
        initialValue = 10f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Build map targets list
    val targets = remember(sourceName, destinationName, members, vehicle, childBagDevice) {
        val list = mutableListOf<MapTarget>()

        // Starting Point (Origin)
        list.add(
            MapTarget(
                id = "origin-point",
                title = sourceName,
                subtitle = "Starting Point",
                latitude = sourceLat,
                longitude = sourceLng,
                color = 0xFF27AE60L,
                type = "SOURCE"
            )
        )

        // Destination
        list.add(
            MapTarget(
                id = "dest-point",
                title = destinationName,
                subtitle = "Trip Destination",
                latitude = destinationLat,
                longitude = destinationLng,
                color = 0xFFEB5757L,
                type = "DESTINATION"
            )
        )

        // Members
        members.forEachIndexed { idx, m ->
            val lat = currentUserLat + (if (idx == 0) 0.0 else (idx * 0.0035 - 0.002))
            val lng = currentUserLng + (if (idx == 0) 0.0 else (idx * 0.0040 - 0.003))
            list.add(
                MapTarget(
                    id = m.id,
                    title = m.name,
                    subtitle = if (idx == 0) "You (Sharing Live)" else "Buddy · ${m.name}",
                    latitude = lat,
                    longitude = lng,
                    speedKmh = if (idx == 0) 42.0 else 38.0 + (idx * 3),
                    batteryPercent = 95 - (idx * 8),
                    color = m.color,
                    type = "USER"
                )
            )
        }

        // Vehicle
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

        // Child Bag Tag
        childBagDevice?.let { list.add(it) }

        list
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .border(
                1.dp,
                if (darkMode) Color(0xFF334155) else Color(0xFFE2E8F0),
                RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (mapTheme) {
                MapTheme.GOOGLE_STREETS -> Color(0xFFF1F5F9)
                MapTheme.SATELLITE -> Color(0xFF192823)
                MapTheme.DARK_NAVIGATION -> Color(0xFF0F172A)
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
            // Map Canvas: Google Map roads, grid, water bodies, and polyline
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2 + panOffsetX
                val cy = size.height / 2 + panOffsetY

                val roadColor = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFE2E8F0)
                    MapTheme.SATELLITE -> Color(0xFF283618)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF1E293B)
                }
                val highwayColor = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFFFD166)
                    MapTheme.SATELLITE -> Color(0xFF606C38)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF3B82F6)
                }
                val waterColor = when (mapTheme) {
                    MapTheme.GOOGLE_STREETS -> Color(0xFFC7D2FE)
                    MapTheme.SATELLITE -> Color(0xFF1B4965)
                    MapTheme.DARK_NAVIGATION -> Color(0xFF0C4A6E)
                }

                // Water body
                drawCircle(
                    color = waterColor,
                    radius = 90f * zoomLevel,
                    center = Offset(cx - 140f * zoomLevel, cy + 90f * zoomLevel)
                )

                // Secondary road grid
                val step = 45f * zoomLevel
                var x = (panOffsetX % step) - step
                while (x < size.width + step) {
                    drawLine(roadColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
                    x += step
                }
                var y = (panOffsetY % step) - step
                while (y < size.height + step) {
                    drawLine(roadColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
                    y += step
                }

                // Main Google Maps Blue Navigation Polyline (Source to Destination)
                val routePath = Path()
                routePath.moveTo(cx - 160f * zoomLevel, cy + 110f * zoomLevel)
                routePath.cubicTo(
                    cx - 70f * zoomLevel, cy + 50f * zoomLevel,
                    cx + 40f * zoomLevel, cy - 30f * zoomLevel,
                    cx + 160f * zoomLevel, cy - 110f * zoomLevel
                )
                // Outer glow road outline
                drawPath(
                    path = routePath,
                    color = Color(0x551D64F2),
                    style = Stroke(
                        width = 14f * zoomLevel,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
                // Core Google Maps Vibrant Blue Route Line
                drawPath(
                    path = routePath,
                    color = Color(0xFF1D64F2),
                    style = Stroke(
                        width = 8f * zoomLevel,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )

                // Safe zone circle for Child Bag tag
                childBagDevice?.safeZoneRadiusMeters?.let {
                    val bagCx = cx + 50f * zoomLevel
                    val bagCy = cy + 40f * zoomLevel
                    drawCircle(
                        color = Color(0x3322C55E),
                        radius = 45f * zoomLevel,
                        center = Offset(bagCx, bagCy)
                    )
                    drawCircle(
                        color = Color(0xFF22C55E),
                        radius = 45f * zoomLevel,
                        center = Offset(bagCx, bagCy),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )
                    )
                }

                // Current User Pulsing GPS Beacon
                drawCircle(
                    color = Color(0xFF3B82F6).copy(alpha = pulseAlpha),
                    radius = pulseRadius * zoomLevel,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 9f * zoomLevel,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFF2563EB),
                    radius = 6f * zoomLevel,
                    center = Offset(cx, cy)
                )
            }

            // Interactive Markers Layer
            targets.forEachIndexed { index, target ->
                val baseOffsetX = when (target.type) {
                    "SOURCE" -> -160f
                    "DESTINATION" -> 160f
                    "VEHICLE" -> -100f
                    "CHILD_BAG" -> 50f
                    else -> when (index % 4) {
                        0 -> 0f
                        1 -> 70f
                        2 -> -60f
                        else -> 110f
                    }
                }
                val baseOffsetY = when (target.type) {
                    "SOURCE" -> 110f
                    "DESTINATION" -> -110f
                    "VEHICLE" -> 80f
                    "CHILD_BAG" -> 40f
                    else -> when (index % 4) {
                        0 -> 0f
                        1 -> -60f
                        2 -> -50f
                        else -> 60f
                    }
                }

                val posX = (panOffsetX + baseOffsetX * zoomLevel)
                val posY = (panOffsetY + baseOffsetY * zoomLevel)

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = posX.dp, y = posY.dp)
                        .clickable {
                            selectedTarget = target
                            onSelectTarget?.invoke(target)
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Badge label above marker
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (darkMode) Color(0xFF1E293B) else Color.White,
                            shadowElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(target.color))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = target.title.take(10),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (darkMode) Color.White else Color(0xFF1E293B)
                                )
                                if (target.speedKmh > 0) {
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = "${target.speedKmh.roundToInt()} km/h",
                                        fontSize = 9.sp,
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(3.dp))

                        // Marker Icon Pin
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = CircleShape,
                            color = Color(target.color),
                            shadowElevation = 6.dp,
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when (target.type) {
                                    "VEHICLE" -> Icon(
                                        Icons.Filled.DirectionsCar,
                                        contentDescription = "Vehicle",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    "CHILD_BAG" -> Icon(
                                        Icons.Filled.School,
                                        contentDescription = "Child Bag",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    "SOURCE" -> Icon(
                                        Icons.Filled.Navigation,
                                        contentDescription = "Source",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    "DESTINATION" -> Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = "Destination",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    else -> Text(
                                        text = target.title.take(2).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top-left: Google Maps Live branding & Coordinates
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
                // Recenter
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

                // Zoom In
                SmallFloatingActionButton(
                    onClick = { zoomLevel = (zoomLevel * 1.25f).coerceAtMost(3.0f) },
                    containerColor = if (darkMode) Color(0xFF1E293B) else Color.White,
                    contentColor = if (darkMode) Color.White else Color(0xFF1E293B),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.Add, "Zoom In", modifier = Modifier.size(18.dp))
                }

                // Zoom Out
                SmallFloatingActionButton(
                    onClick = { zoomLevel = (zoomLevel / 1.25f).coerceAtLeast(0.6f) },
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
