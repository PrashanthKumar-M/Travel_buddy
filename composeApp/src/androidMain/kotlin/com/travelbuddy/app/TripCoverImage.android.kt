package com.travelbuddy.app

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URL

@Composable
actual fun PlatformTripCoverImage(uri: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                Thread {
                    try {
                        val stream = URL(uri).openStream()
                        val bitmap = BitmapFactory.decodeStream(stream)
                        imageView.post { imageView.setImageBitmap(bitmap) }
                    } catch (_: Throwable) {
                        // Keep previous or fallback placeholder
                    }
                }.start()
            } else {
                try {
                    imageView.setImageURI(Uri.parse(uri))
                } catch (_: Throwable) {
                    // Handled gracefully
                }
            }
        }
    )
}
