package com.travelbuddy.app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformTripCoverImage(uri: String, modifier: Modifier) {
    Text("Trip cover selected", modifier = modifier)
}
