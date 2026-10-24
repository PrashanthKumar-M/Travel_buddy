import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "TravelBuddy"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("travelbuddy")
        browser {
            commonWebpackConfig {
                outputFileName = "travelbuddy.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation("com.google.android.gms:play-services-maps:19.0.0")
            implementation("com.google.android.gms:play-services-location:21.3.0")
        }
    }
}

android {
    namespace = "com.travelbuddy.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.travelbuddy.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("travelbuddy.keystore")
            storePassword = "travelbuddy123"
            keyAlias = "travelbuddy"
            keyPassword = "travelbuddy123"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    val apiBaseUrl = providers.gradleProperty("TRAVEL_BUDDY_API_URL")
        .getOrElse("https://travel-buddy-3vz4.onrender.com")
    val wsUrl = providers.gradleProperty("TRAVEL_BUDDY_WS_URL")
        .getOrElse("wss://travel-buddy-3vz4.onrender.com/ws/live")
    val mapsApiKey = providers.gradleProperty("MAPS_API_KEY")
        .orElse(providers.environmentVariable("MAPS_API_KEY"))
        .getOrElse("AIzaSyBajIytr__LvhPpGz4Wlc3l9Rpn3YP-PqY")

    defaultConfig {
        buildConfigField("String", "TRAVEL_BUDDY_API_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "TRAVEL_BUDDY_WS_URL", "\"$wsUrl\"")
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }
}
