package com.travelbuddy.app

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
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
            if (imageView.tag == uri) return@AndroidView
            imageView.tag = uri
            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                Thread {
                    try {
                        val stream = URL(uri).openStream()
                        val bitmap = BitmapFactory.decodeStream(stream)
                        stream.close()
                        if (bitmap != null) {
                            imageView.post { imageView.setImageBitmap(bitmap) }
                        }
                    } catch (_: Throwable) {
                        // Keep previous or fallback placeholder
                    }
                }.start()
            } else {
                Thread {
                    try {
                        val parsed = Uri.parse(uri)
                        val bitmap = if (uri.startsWith("file://")) {
                            val file = File(parsed.path ?: uri.removePrefix("file://"))
                            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                        } else {
                            imageView.context.contentResolver.openInputStream(parsed)?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        }
                        if (bitmap != null) {
                            imageView.post { imageView.setImageBitmap(bitmap) }
                        } else {
                            imageView.post { imageView.setImageURI(parsed) }
                        }
                    } catch (_: Throwable) {
                        // Handled gracefully
                    }
                }.start()
            }
        }
    )
}
