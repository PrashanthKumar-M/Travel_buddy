package com.travelbuddy.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Renders a cover selected by the current platform's photo picker. */
@Composable
expect fun PlatformTripCoverImage(uri: String, modifier: Modifier = Modifier)
