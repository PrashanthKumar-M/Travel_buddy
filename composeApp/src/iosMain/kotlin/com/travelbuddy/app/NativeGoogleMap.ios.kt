package com.travelbuddy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.travelbuddy.app.domain.TripMember

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
    Box(
        modifier = modifier.background(Color(0xFFF1F5F9)),
        contentAlignment = Alignment.Center
    ) {
        Text("Map View (iOS Native)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
