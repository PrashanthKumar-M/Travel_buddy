package com.travelbuddy.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.widget.Toast
import androidx.core.view.WindowCompat

actual object PlatformShare {
    var appContext: Context? = null
    var currentWindow: Window? = null

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

    actual fun openDialer(phoneNumber: String) {
        val ctx = appContext ?: return
        try {
            val clean = phoneNumber.filter { it.isDigit() || it == '+' }
            val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$clean")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (_: Exception) {}
    }

    actual fun copyToClipboard(text: String, label: String) {
        val ctx = appContext ?: return
        try {
            val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(ctx, "$label copied: $text", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {}
    }

    actual fun isNetworkAvailable(): Boolean {
        val ctx = appContext ?: return true
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            hasInternet && hasTransport
        } catch (_: Exception) {
            true
        }
    }

    actual fun setLightStatusBar(isLight: Boolean) {
        val win = currentWindow ?: return
        Handler(Looper.getMainLooper()).post {
            try {
                @Suppress("DEPRECATION")
                win.statusBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(win, win.decorView).isAppearanceLightStatusBars = isLight
            } catch (_: Exception) {}
        }
    }
}
