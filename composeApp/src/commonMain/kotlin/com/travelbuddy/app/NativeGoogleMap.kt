package com.travelbuddy.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.travelbuddy.app.domain.TripMember

/**
 * Real Google Maps SDK Integration.
 * On Android, embeds official com.google.android.gms.maps.MapView with:
 * - Real Google vector tiles, 3D buildings, zoom, tilt, rotation
 * - googleMap.isMyLocationEnabled = true (Real Google Maps Live Location Beacon)
 * - Real Google Map Markers for all joined trip members with custom badges
 * - Polylines between users and route
 * - Camera bounds fitting all members
 */
@Composable
expect fun NativeGoogleMapView(
    sourceName: String,
    sourceLat: Double,
    sourceLng: Double,
    destinationName: String,
    destinationLat: Double,
    destinationLng: Double,
    currentUserLat: Double,
    currentUserLng: Double,
    members: List<TripMember>,
    modifier: Modifier = Modifier
)
