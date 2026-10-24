package com.travelbuddy.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlatformStorage.init(applicationContext)
        PlatformBattery.appContext = applicationContext
        PlatformLocation.init(applicationContext)
        PlatformShare.appContext = applicationContext
        PlatformShare.currentWindow = window
        AndroidNotificationHelper.appContext = applicationContext

        // Make status bar transparent and draw edge-to-edge so time/SIM/battery icons are clearly visible
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }

        // Request real GPS and system permissions
        val permissions = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 101)
        }

        setContent {
            var pickedCoverUri by remember { mutableStateOf<String?>(null) }
            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    try {
                        val destFile = File(filesDir, "trip_cover_${System.currentTimeMillis()}.jpg")
                        contentResolver.openInputStream(uri)?.use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (destFile.exists() && destFile.length() > 0L) {
                            pickedCoverUri = Uri.fromFile(destFile).toString()
                        } else {
                            pickedCoverUri = uri.toString()
                        }
                    } catch (_: Exception) {
                        pickedCoverUri = uri.toString()
                    }
                }
            }

            TravelBuddyApp(
                config = AppConfig(
                    apiBaseUrl = BuildConfig.TRAVEL_BUDDY_API_URL,
                    webSocketUrl = BuildConfig.TRAVEL_BUDDY_WS_URL,
                    renderBackendUrl = BuildConfig.TRAVEL_BUDDY_API_URL,
                    useRemoteApi = true
                ),
                pendingCoverImageUri = pickedCoverUri,
                onPickTripCoverImage = {
                    try {
                        imagePickerLauncher.launch("image/*")
                    } catch (_: Exception) {}
                }
            )
        }
    }

    override fun onDestroy() {
        if (PlatformShare.currentWindow === window) {
            PlatformShare.currentWindow = null
        }
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            PlatformLocation.startContinuousTracking()
        }
    }
}
