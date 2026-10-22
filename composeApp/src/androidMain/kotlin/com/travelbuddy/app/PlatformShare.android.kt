package com.travelbuddy.app

import android.content.Context
import android.content.Intent

actual object PlatformShare {
    var appContext: Context? = null

    actual fun shareText(text: String, title: String) {
        val ctx = appContext ?: return
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, title)
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val shareIntent = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(shareIntent)
        } catch (_: Exception) {}
    }
}
