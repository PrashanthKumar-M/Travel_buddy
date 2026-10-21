package com.travelbuddy.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AvatarProfile(val id: String, val title: String, val badgeColor: Color) {
    MALE_TRAVELER("MALE_1", "Male Adventurer", Color(0xFF2563EB)),
    FEMALE_TRAVELER("FEMALE_1", "Female Adventurer", Color(0xFFEC4899)),
    MALE_EXPLORER("MALE_2", "Safari Explorer", Color(0xFF059669)),
    FEMALE_EXPLORER("FEMALE_2", "Mountain Trekker", Color(0xFFF97316)),
    BIKER("BIKER", "Motorcycle Rider", Color(0xFF7C3AED)),
    CYCLIST("CYCLIST", "Bicycle Cyclist", Color(0xFF0D9488))
}

/**
 * 100% Original, Copyright-Free vector-drawn traveler avatars rendered dynamically via Compose Canvas.
 * Safe for Google Play Store upload without any copyrighted third-party emoji assets.
 */
@Composable
fun TravelerAvatar(
    avatarType: String = "MALE_1",
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val normType = avatarType.uppercase().trim()
    val bgColor = when {
        normType.contains("FEMALE_1") -> Color(0xFFFCE7F3)
        normType.contains("FEMALE_2") -> Color(0xFFFFEDD5)
        normType.contains("MALE_2") -> Color(0xFFDCFCE7)
        normType.contains("BIKER") -> Color(0xFFEDE9FE)
        normType.contains("CYCLIST") -> Color(0xFFCCFBF1)
        else -> Color(0xFFDBEAFE) // MALE_1 default
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            when (normType) {
                "FEMALE_1" -> {
                    // Female Adventurer: Headband, stylish ponytail, warm skin, coral adventure hoodie
                    // Shoulders / Hoodie
                    drawRoundRect(
                        color = Color(0xFFF43F5E),
                        topLeft = Offset(cx - w * 0.42f, h * 0.62f),
                        size = Size(w * 0.84f, h * 0.45f),
                        cornerRadius = CornerRadius(w * 0.25f, w * 0.25f)
                    )
                    // Neck
                    drawRect(
                        color = Color(0xFFFBBF24),
                        topLeft = Offset(cx - w * 0.10f, h * 0.48f),
                        size = Size(w * 0.20f, h * 0.18f)
                    )
                    // Hair back
                    drawCircle(color = Color(0xFF451A03), radius = w * 0.30f, center = Offset(cx, cy * 0.78f))
                    // Face
                    drawCircle(color = Color(0xFFFCD34D), radius = w * 0.22f, center = Offset(cx, cy * 0.84f))
                    // Headband (Teal)
                    drawRoundRect(
                        color = Color(0xFF06B6D4),
                        topLeft = Offset(cx - w * 0.23f, h * 0.22f),
                        size = Size(w * 0.46f, h * 0.08f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    // Friendly Eyes & Smile
                    drawCircle(Color(0xFF1E293B), radius = w * 0.025f, center = Offset(cx - w * 0.08f, cy * 0.84f))
                    drawCircle(Color(0xFF1E293B), radius = w * 0.025f, center = Offset(cx + w * 0.08f, cy * 0.84f))
                    val smile = Path().apply {
                        moveTo(cx - w * 0.06f, cy * 0.96f)
                        quadraticTo(cx, cy * 1.05f, cx + w * 0.06f, cy * 0.96f)
                    }
                    drawPath(smile, Color(0xFFE11D48), style = Stroke(width = w * 0.03f))
                }

                "MALE_2" -> {
                    // Safari / Jungle Explorer: Khaki explorer hat, olive safari jacket
                    // Olive Safari Jacket
                    drawRoundRect(
                        color = Color(0xFF059669),
                        topLeft = Offset(cx - w * 0.44f, h * 0.60f),
                        size = Size(w * 0.88f, h * 0.45f),
                        cornerRadius = CornerRadius(w * 0.26f, w * 0.26f)
                    )
                    // Face
                    drawCircle(color = Color(0xFFFCD34D), radius = w * 0.23f, center = Offset(cx, cy * 0.82f))
                    // Explorer Hat Brim
                    drawRoundRect(
                        color = Color(0xFFD97706),
                        topLeft = Offset(cx - w * 0.38f, h * 0.26f),
                        size = Size(w * 0.76f, h * 0.09f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    // Explorer Hat Crown
                    drawRoundRect(
                        color = Color(0xFFB45309),
                        topLeft = Offset(cx - w * 0.22f, h * 0.12f),
                        size = Size(w * 0.44f, h * 0.16f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    // Eyes & Smile
                    drawCircle(Color(0xFF1E293B), radius = w * 0.026f, center = Offset(cx - w * 0.08f, cy * 0.82f))
                    drawCircle(Color(0xFF1E293B), radius = w * 0.026f, center = Offset(cx + w * 0.08f, cy * 0.82f))
                    val smile = Path().apply {
                        moveTo(cx - w * 0.06f, cy * 0.96f)
                        quadraticTo(cx, cy * 1.06f, cx + w * 0.06f, cy * 0.96f)
                    }
                    drawPath(smile, Color(0xFFB45309), style = Stroke(width = w * 0.035f))
                }

                "FEMALE_2" -> {
                    // Mountain Trekker: Orange puff jacket, adventurer cap, camera strap
                    drawRoundRect(
                        color = Color(0xFFEA580C),
                        topLeft = Offset(cx - w * 0.44f, h * 0.60f),
                        size = Size(w * 0.88f, h * 0.45f),
                        cornerRadius = CornerRadius(w * 0.26f, w * 0.26f)
                    )
                    // Camera strap
                    drawLine(Color(0xFF1E293B), Offset(cx - w * 0.25f, h * 0.60f), Offset(cx + w * 0.25f, h * 0.95f), strokeWidth = w * 0.04f)
                    // Face
                    drawCircle(color = Color(0xFFFDE68A), radius = w * 0.22f, center = Offset(cx, cy * 0.82f))
                    // Cap
                    drawRoundRect(
                        color = Color(0xFF0284C7),
                        topLeft = Offset(cx - w * 0.24f, h * 0.14f),
                        size = Size(w * 0.48f, h * 0.17f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    drawRoundRect(
                        color = Color(0xFF0369A1),
                        topLeft = Offset(cx - w * 0.32f, h * 0.27f),
                        size = Size(w * 0.64f, h * 0.06f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    // Eyes
                    drawCircle(Color(0xFF1E293B), radius = w * 0.025f, center = Offset(cx - w * 0.08f, cy * 0.82f))
                    drawCircle(Color(0xFF1E293B), radius = w * 0.025f, center = Offset(cx + w * 0.08f, cy * 0.82f))
                }

                "BIKER" -> {
                    // Motorcycle Biker: Helmet with glossy tinted visor, leather jacket
                    // Leather Jacket
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(cx - w * 0.44f, h * 0.62f),
                        size = Size(w * 0.88f, h * 0.45f),
                        cornerRadius = CornerRadius(w * 0.25f, w * 0.25f)
                    )
                    // Helmet outer
                    drawCircle(color = Color(0xFF6D28D9), radius = w * 0.34f, center = Offset(cx, cy * 0.74f))
                    // Glossy Visor
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(cx - w * 0.24f, h * 0.30f),
                        size = Size(w * 0.48f, h * 0.18f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    // Visor sheen/highlight
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.5f),
                        topLeft = Offset(cx - w * 0.18f, h * 0.32f),
                        size = Size(w * 0.22f, h * 0.04f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    // Chin guard
                    drawRoundRect(
                        color = Color(0xFF5B21B6),
                        topLeft = Offset(cx - w * 0.18f, h * 0.50f),
                        size = Size(w * 0.36f, h * 0.08f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

                "CYCLIST" -> {
                    // Cyclist: Aerodynamic cycling helmet with cooling vents, wrap-around sports shades
                    // Cycling jersey (Teal)
                    drawRoundRect(
                        color = Color(0xFF0D9488),
                        topLeft = Offset(cx - w * 0.44f, h * 0.62f),
                        size = Size(w * 0.88f, h * 0.45f),
                        cornerRadius = CornerRadius(w * 0.25f, w * 0.25f)
                    )
                    // Face
                    drawCircle(color = Color(0xFFFCD34D), radius = w * 0.22f, center = Offset(cx, cy * 0.82f))
                    // Aerodynamic Helmet
                    drawRoundRect(
                        color = Color(0xFF14B8A6),
                        topLeft = Offset(cx - w * 0.28f, h * 0.12f),
                        size = Size(w * 0.56f, h * 0.22f),
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                    // Vents
                    drawRoundRect(Color(0xFF042F2E), Offset(cx - w * 0.14f, h * 0.17f), Size(w * 0.08f, h * 0.08f), CornerRadius(4f, 4f))
                    drawRoundRect(Color(0xFF042F2E), Offset(cx + w * 0.06f, h * 0.17f), Size(w * 0.08f, h * 0.08f), CornerRadius(4f, 4f))
                    // Sports Sunglasses
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(cx - w * 0.20f, h * 0.34f),
                        size = Size(w * 0.40f, h * 0.08f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }

                else -> {
                    // MALE_1: Classic Male Adventurer: Sapphire blue jacket, styled hair, friendly glasses
                    // Blue Jacket
                    drawRoundRect(
                        color = Color(0xFF2563EB),
                        topLeft = Offset(cx - w * 0.44f, h * 0.62f),
                        size = Size(w * 0.88f, h * 0.45f),
                        cornerRadius = CornerRadius(w * 0.25f, w * 0.25f)
                    )
                    // Face
                    drawCircle(color = Color(0xFFFCD34D), radius = w * 0.23f, center = Offset(cx, cy * 0.82f))
                    // Modern styled dark hair
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(cx - w * 0.25f, h * 0.16f),
                        size = Size(w * 0.50f, h * 0.18f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    // Cool Glasses Frame
                    drawCircle(Color.Transparent, radius = w * 0.06f, center = Offset(cx - w * 0.09f, cy * 0.82f))
                    drawCircle(Color(0xFF1E293B), radius = w * 0.05f, center = Offset(cx - w * 0.09f, cy * 0.82f), style = Stroke(w * 0.02f))
                    drawCircle(Color(0xFF1E293B), radius = w * 0.05f, center = Offset(cx + w * 0.09f, cy * 0.82f), style = Stroke(w * 0.02f))
                    drawLine(Color(0xFF1E293B), Offset(cx - w * 0.04f, cy * 0.82f), Offset(cx + w * 0.04f, cy * 0.82f), strokeWidth = w * 0.025f)
                    // Friendly Smile
                    val smile = Path().apply {
                        moveTo(cx - w * 0.06f, cy * 0.96f)
                        quadraticTo(cx, cy * 1.05f, cx + w * 0.06f, cy * 0.96f)
                    }
                    drawPath(smile, Color(0xFFB45309), style = Stroke(width = w * 0.03f))
                }
            }
        }
    }
}

/**
 * Avatar Selector for Profile or Trip Settings.
 */
@Composable
fun AvatarSelector(
    selectedAvatar: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(AvatarProfile.values()) { profile ->
                val isSelected = selectedAvatar.equals(profile.id, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) profile.badgeColor.copy(alpha = 0.15f) else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) profile.badgeColor else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.clickable { onSelect(profile.id) }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            TravelerAvatar(avatarType = profile.id, size = 48.dp)
                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = profile.badgeColor,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.padding(2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            profile.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) profile.badgeColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
