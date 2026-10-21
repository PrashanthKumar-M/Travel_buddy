package com.travelbuddy.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Official vector brand logo for Travel Buddy matching the screenshot:
 * Blue gradient squircle with white location pin.
 */
@Composable
fun TravelBuddyPinLogo(
    modifier: Modifier = Modifier,
    size: Dp = 68.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val scale = w / 100f

        // 1. Blue squircle background with smooth vertical gradient
        val corner = 26f * scale
        val bgBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0284C7), // Vibrant sky blue
                Color(0xFF0284C7),
                Color(0xFF0369A1)  // Deep ocean blue
            ),
            startY = 0f,
            endY = h
        )
        drawRoundRect(
            brush = bgBrush,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner)
        )

        // 2. White location pin
        val cx = 50f * scale
        val cy = 43f * scale
        val r = 18f * scale
        val tipY = 74f * scale

        val pinPath = Path().apply {
            // Arc for the circular head of the pin
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r),
                startAngleDegrees = 145f,
                sweepAngleDegrees = 250f,
                forceMoveTo = true
            )
            // Lines to the bottom tip
            lineTo(cx, tipY)
            close()
        }
        drawPath(
            path = pinPath,
            color = Color.White
        )

        // 3. Circular cutout hole in the pin head
        val holeRadius = 7.5f * scale
        val holeCenter = Offset(cx, cy)
        drawCircle(
            brush = bgBrush,
            radius = holeRadius,
            center = holeCenter
        )
    }
}

/**
 * Official vector brand logo for Travel Buddy.
 * Renders on Android, iOS, and Web (Wasm) with zero external asset dependencies.
 */
@Composable
fun TravelBuddyLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val scale = w / 100f

        // 1. Squircle background with rich travel gradient
        val corner = 22f * scale
        val bgBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E3A8A), // Deep navy blue
                Color(0xFF2563EB), // Royal azure blue
                Color(0xFF0284C7)  // Vibrant sky cyan
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        drawRoundRect(
            brush = bgBrush,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner)
        )

        // 2. Rising Sun at horizon
        val sunCenter = Offset(50f * scale, 38f * scale)
        val sunRadius = 14f * scale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B)),
                center = sunCenter,
                radius = sunRadius
            ),
            radius = sunRadius,
            center = sunCenter
        )

        // 3. Mountain peaks
        val mountainPath = Path().apply {
            moveTo(12f * scale, 65f * scale)
            lineTo(38f * scale, 42f * scale)
            lineTo(58f * scale, 65f * scale)
            close()
        }
        drawPath(
            path = mountainPath,
            color = Color(0x99475569)
        )
        val mountainPath2 = Path().apply {
            moveTo(45f * scale, 65f * scale)
            lineTo(66f * scale, 45f * scale)
            lineTo(88f * scale, 65f * scale)
            close()
        }
        drawPath(
            path = mountainPath2,
            color = Color(0x99334155)
        )

        // 4. Winding highway road
        val roadPath = Path().apply {
            moveTo(47f * scale, 48f * scale)
            cubicTo(
                45f * scale, 60f * scale,
                38f * scale, 75f * scale,
                24f * scale, 98f * scale
            )
            lineTo(76f * scale, 98f * scale)
            cubicTo(
                62f * scale, 75f * scale,
                55f * scale, 60f * scale,
                53f * scale, 48f * scale
            )
            close()
        }
        drawPath(
            path = roadPath,
            color = Color(0xFF1E293B)
        )

        // Green emerald road curbs
        val leftCurb = Path().apply {
            moveTo(47f * scale, 48f * scale)
            cubicTo(
                45f * scale, 60f * scale,
                38f * scale, 75f * scale,
                24f * scale, 98f * scale
            )
        }
        drawPath(
            path = leftCurb,
            color = Color(0xFF22C55E),
            style = Stroke(width = 1.2f * scale, cap = StrokeCap.Round)
        )
        val rightCurb = Path().apply {
            moveTo(53f * scale, 48f * scale)
            cubicTo(
                55f * scale, 60f * scale,
                62f * scale, 75f * scale,
                76f * scale, 98f * scale
            )
        }
        drawPath(
            path = rightCurb,
            color = Color(0xFF22C55E),
            style = Stroke(width = 1.2f * scale, cap = StrokeCap.Round)
        )

        // Yellow dashed center line
        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(50f * scale, 52f * scale),
            end = Offset(50f * scale, 58f * scale),
            strokeWidth = 1.5f * scale,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(49.5f * scale, 64f * scale),
            end = Offset(49.5f * scale, 73f * scale),
            strokeWidth = 2.2f * scale,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(49.2f * scale, 80f * scale),
            end = Offset(49.2f * scale, 92f * scale),
            strokeWidth = 3f * scale,
            cap = StrokeCap.Round
        )

        // 5. Red destination waypoint pin
        val pinHeadCenter = Offset(50f * scale, 30f * scale)
        val pinHeadRadius = 7.5f * scale
        val pinPath = Path().apply {
            moveTo(50f * scale - pinHeadRadius, 30f * scale)
            cubicTo(
                50f * scale - pinHeadRadius, 24f * scale,
                50f * scale + pinHeadRadius, 24f * scale,
                50f * scale + pinHeadRadius, 30f * scale
            )
            lineTo(50f * scale, 45f * scale)
            close()
        }
        drawPath(
            path = pinPath,
            color = Color(0xFFEF4444)
        )
        drawCircle(
            color = Color(0xFFEF4444),
            radius = pinHeadRadius,
            center = pinHeadCenter
        )

        // White dot inside pin
        drawCircle(
            color = Color.White,
            radius = 3f * scale,
            center = pinHeadCenter
        )
    }
}
