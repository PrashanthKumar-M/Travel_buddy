package com.travelbuddy.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Universal, sleek vertical scrollbar modifier for LazyColumn.
 * Draws an interactive visual track and moving thumb on the right edge of any scrollable container.
 * Gives immediate scrollview identification across the app.
 */
fun Modifier.verticalScrollbar(
    state: LazyListState,
    thumbColor: Color = Color(0xFF2F80ED),
    trackColor: Color = Color(0x252F80ED),
    width: Dp = 5.dp,
    paddingEnd: Dp = 2.dp,
    paddingTop: Dp = 6.dp,
    paddingBottom: Dp = 6.dp,
    minThumbHeight: Dp = 32.dp
): Modifier = this.drawWithContent {
    drawContent()

    val totalItems = state.layoutInfo.totalItemsCount
    val visibleItems = state.layoutInfo.visibleItemsInfo
    if (totalItems <= 0 || visibleItems.isEmpty()) return@drawWithContent

    val firstVisibleItem = state.firstVisibleItemIndex
    val totalVisible = visibleItems.size
    if (totalVisible >= totalItems && firstVisibleItem == 0) return@drawWithContent

    val topPx = paddingTop.toPx()
    val bottomPx = paddingBottom.toPx()
    val availableHeight = size.height - topPx - bottomPx
    if (availableHeight <= 0f) return@drawWithContent

    val widthPx = width.toPx()
    val rightPaddingPx = paddingEnd.toPx()
    val x = size.width - widthPx - rightPaddingPx

    val cornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f)

    // Draw scroll track
    drawRoundRect(
        color = trackColor,
        topLeft = Offset(x, topPx),
        size = Size(widthPx, availableHeight),
        cornerRadius = cornerRadius
    )

    // Compute thumb size & position
    val visibleFraction = (totalVisible.toFloat() / totalItems.toFloat()).coerceIn(0.08f, 0.9f)
    val thumbHeight = (availableHeight * visibleFraction).coerceAtLeast(minThumbHeight.toPx())

    val scrollFraction = (firstVisibleItem.toFloat() / (totalItems - totalVisible).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val thumbY = topPx + (availableHeight - thumbHeight) * scrollFraction

    // Draw moving thumb
    drawRoundRect(
        color = thumbColor,
        topLeft = Offset(x, thumbY),
        size = Size(widthPx, thumbHeight),
        cornerRadius = cornerRadius
    )
}

/**
 * Universal, sleek vertical scrollbar modifier for Column with verticalScroll(ScrollState).
 */
fun Modifier.verticalScrollbar(
    state: ScrollState,
    thumbColor: Color = Color(0xFF2F80ED),
    trackColor: Color = Color(0x252F80ED),
    width: Dp = 5.dp,
    paddingEnd: Dp = 2.dp,
    paddingTop: Dp = 6.dp,
    paddingBottom: Dp = 6.dp,
    minThumbHeight: Dp = 32.dp
): Modifier = this.drawWithContent {
    drawContent()

    val maxValue = state.maxValue
    if (maxValue <= 0) return@drawWithContent

    val topPx = paddingTop.toPx()
    val bottomPx = paddingBottom.toPx()
    val availableHeight = size.height - topPx - bottomPx
    if (availableHeight <= 0f) return@drawWithContent

    val widthPx = width.toPx()
    val rightPaddingPx = paddingEnd.toPx()
    val x = size.width - widthPx - rightPaddingPx

    val cornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f)

    // Draw scroll track
    drawRoundRect(
        color = trackColor,
        topLeft = Offset(x, topPx),
        size = Size(widthPx, availableHeight),
        cornerRadius = cornerRadius
    )

    // Compute thumb size & position
    val contentHeight = availableHeight + maxValue.toFloat()
    val visibleFraction = (availableHeight / contentHeight).coerceIn(0.08f, 0.9f)
    val thumbHeight = (availableHeight * visibleFraction).coerceAtLeast(minThumbHeight.toPx())

    val scrollFraction = (state.value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
    val thumbY = topPx + (availableHeight - thumbHeight) * scrollFraction

    // Draw moving thumb
    drawRoundRect(
        color = thumbColor,
        topLeft = Offset(x, thumbY),
        size = Size(widthPx, thumbHeight),
        cornerRadius = cornerRadius
    )
}

/**
 * Visual badge indicating that a section/view has scrollable content.
 */
@Composable
fun ScrollIndicatorHint(
    text: String = "Scroll for more",
    darkMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (darkMode) Color(0xFF1E293B) else Color(0xFFEFF6FF),
        border = BorderStroke(1.dp, if (darkMode) Color(0xFF334155) else Color(0xFFBFDBFE)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = null,
                tint = Color(0xFF2F80ED),
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                color = Color(0xFF2F80ED),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
