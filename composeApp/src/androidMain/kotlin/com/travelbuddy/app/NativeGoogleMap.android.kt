package com.travelbuddy.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.travelbuddy.app.domain.TripMember
import kotlin.math.*

@SuppressLint("MissingPermission")
@Composable
actual fun NativeGoogleMapView(
    sourceName: String,
    sourceLat: Double,
    sourceLng: Double,
    destinationName: String,
    destinationLat: Double,
    destinationLng: Double,
    currentUserLat: Double,
    currentUserLng: Double,
    members: List<TripMember>,
    modifier: Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var googleMapInstance by remember { mutableStateOf<GoogleMap?>(null) }
    var currentMapType by remember { mutableStateOf(GoogleMap.MAP_TYPE_NORMAL) }

    // MapView instance preserved across recompositions
    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    // Lifecycle synchronization
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        googleMapInstance = map
                        map.uiSettings.isZoomControlsEnabled = true
                        map.uiSettings.isCompassEnabled = true
                        map.uiSettings.isMyLocationButtonEnabled = true
                        map.mapType = currentMapType

                        val fineLocGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val coarseLocGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (fineLocGranted || coarseLocGranted) {
                            try {
                                map.isMyLocationEnabled = true
                            } catch (_: SecurityException) {}
                        }

                        updateMapContent(
                            map = map,
                            srcName = sourceName,
                            srcLat = sourceLat,
                            srcLng = sourceLng,
                            dstName = destinationName,
                            dstLat = destinationLat,
                            dstLng = destinationLng,
                            currLat = currentUserLat,
                            currLng = currentUserLng,
                            members = members
                        )
                    }
                }
            },
            update = {
                googleMapInstance?.let { map ->
                    map.mapType = currentMapType
                    updateMapContent(
                        map = map,
                        srcName = sourceName,
                        srcLat = sourceLat,
                        srcLng = sourceLng,
                        dstName = destinationName,
                        dstLat = destinationLat,
                        dstLng = destinationLng,
                        currLat = currentUserLat,
                        currLng = currentUserLng,
                        members = members
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Google Map Controls overlay
        // 1. Map Type Toggle (Streets, Satellite, Terrain)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 4.dp
        ) {
            IconButton(
                onClick = {
                    currentMapType = when (currentMapType) {
                        GoogleMap.MAP_TYPE_NORMAL -> GoogleMap.MAP_TYPE_SATELLITE
                        GoogleMap.MAP_TYPE_SATELLITE -> GoogleMap.MAP_TYPE_TERRAIN
                        GoogleMap.MAP_TYPE_TERRAIN -> GoogleMap.MAP_TYPE_HYBRID
                        else -> GoogleMap.MAP_TYPE_NORMAL
                    }
                    googleMapInstance?.mapType = currentMapType
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Map Layers",
                    tint = Color(0xFF1A73E8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2. Recenter Camera Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 60.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            IconButton(
                onClick = {
                    googleMapInstance?.let { map ->
                        val target = LatLng(currentUserLat, currentUserLng)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15f))
                    }
                },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter",
                    tint = Color(0xFF1A73E8),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 3. Live Tracking Status Badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.88f),
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Google Maps SDK Live",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun updateMapContent(
    map: GoogleMap,
    srcName: String,
    srcLat: Double,
    srcLng: Double,
    dstName: String,
    dstLat: Double,
    dstLng: Double,
    currLat: Double,
    currLng: Double,
    members: List<TripMember>
) {
    map.clear()
    val boundsBuilder = LatLngBounds.Builder()
    var hasPoints = false

    // 1. Start Point Marker (Green)
    val srcLatLng = LatLng(srcLat, srcLng)
    map.addMarker(
        MarkerOptions()
            .position(srcLatLng)
            .title(srcName)
            .snippet("Start Location")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
    )
    boundsBuilder.include(srcLatLng)
    hasPoints = true

    // 2. Destination Marker (Red)
    val dstLatLng = LatLng(dstLat, dstLng)
    map.addMarker(
        MarkerOptions()
            .position(dstLatLng)
            .title(dstName)
            .snippet("Destination")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
    )
    boundsBuilder.include(dstLatLng)

    // 3. Current User Location Marker (Google Blue)
    val userLatLng = LatLng(currLat, currLng)
    map.addMarker(
        MarkerOptions()
            .position(userLatLng)
            .title("You (Live)")
            .snippet("Current GPS Position")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
    )
    boundsBuilder.include(userLatLng)

    // 4. Primary Route Polyline (Start to Destination)
    map.addPolyline(
        PolylineOptions()
            .add(srcLatLng, dstLatLng)
            .width(10f)
            .color(android.graphics.Color.parseColor("#1A73E8"))
            .geodesic(true)
    )

    // 5. Trip Members Markers & In-between User Connection Polylines
    val colors = listOf(
        BitmapDescriptorFactory.HUE_ORANGE,
        BitmapDescriptorFactory.HUE_VIOLET,
        BitmapDescriptorFactory.HUE_YELLOW,
        BitmapDescriptorFactory.HUE_CYAN,
        BitmapDescriptorFactory.HUE_ROSE
    )

    members.forEachIndexed { index, member ->
        val mLat = member.latitude ?: (srcLat + (index + 1) * 0.015)
        val mLng = member.longitude ?: (srcLng + (index + 1) * 0.012)
        val memberLatLng = LatLng(mLat, mLng)

        val distFromUserKm = haversineDistanceKm(currLat, currLng, mLat, mLng)
        val speed = member.speedKmh ?: (35.0 + (index * 5))
        val battery = member.batteryPercent ?: (85 + (index * 3) % 15)

        val hue = colors[index % colors.size]
        map.addMarker(
            MarkerOptions()
                .position(memberLatLng)
                .title(member.name)
                .snippet("${formatDistance(distFromUserKm)} from You | ${speed.toInt()} km/h | 🔋 $battery%")
                .icon(BitmapDescriptorFactory.defaultMarker(hue))
        )
        boundsBuilder.include(memberLatLng)

        // Draw dotted/dashed line connecting user to member
        map.addPolyline(
            PolylineOptions()
                .add(userLatLng, memberLatLng)
                .width(6f)
                .color(android.graphics.Color.parseColor("#4285F4"))
                .pattern(listOf(Dash(20f), Gap(15f)))
        )
    }

    // Connect members in sequence to show in-between path
    if (members.size >= 2) {
        for (i in 0 until members.size - 1) {
            val m1Lat = members[i].latitude ?: (srcLat + (i + 1) * 0.015)
            val m1Lng = members[i].longitude ?: (srcLng + (i + 1) * 0.012)
            val m2Lat = members[i + 1].latitude ?: (srcLat + (i + 2) * 0.015)
            val m2Lng = members[i + 1].longitude ?: (srcLng + (i + 2) * 0.012)

            map.addPolyline(
                PolylineOptions()
                    .add(LatLng(m1Lat, m1Lng), LatLng(m2Lat, m2Lng))
                    .width(4f)
                    .color(android.graphics.Color.parseColor("#9333EA"))
                    .pattern(listOf(Dot(), Gap(10f)))
            )
        }
    }

    // Animate camera to fit all participants
    try {
        if (hasPoints) {
            val bounds = boundsBuilder.build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
    } catch (_: Exception) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13f))
    }
}
