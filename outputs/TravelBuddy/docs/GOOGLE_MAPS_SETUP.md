# Google Maps API Setup & Integration Guide (100% Free Tier)

Travel Buddy integrates real-time Google Maps live tracking, route calculation, Haversine distance, and dynamic ETA estimation for users, vehicles, and tags.

Google Maps Platform provides **$200 USD of free monthly credit** for every account, which renews automatically each month. This gives you **up to 28,500 dynamic map loads and 40,000 directions requests every month at $0 cost**.

---

## 1. Create Your Free Google Cloud & Google Maps Account

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Sign in with your Google account.
3. Click **Select a project** > **New Project**, name it `TravelBuddy`, and click **Create**.
4. Go to **Billing** and link a credit/debit card (Google does not charge you unless you explicitly exceed the \$200 monthly free tier).

---

## 2. Enable Google Maps APIs

In Google Cloud Console, navigate to **APIs & Services** > **Library**, search for and **Enable** each of the following APIs:

1. **Maps SDK for Android** (enables native Google Maps on Android phones)
2. **Maps JavaScript API** (enables interactive Google Maps on Web)
3. **Directions API** (enables turn-by-turn route calculation and traffic status)
4. **Distance Matrix API** (computes exact road distances and live ETAs)
5. **Geocoding API** (converts addresses like "Bengaluru, Karnataka" to lat/lng)

---

## 3. Generate Your Google Maps API Key

1. Navigate to **APIs & Services** > **Credentials**.
2. Click **+ Create Credentials** > **API Key**.
3. Copy your newly created API key (e.g., `AIzaSy...`).
4. *(Recommended for Production)* Click **Edit API Key**:
   - Under **API restrictions**, select **Restrict key** and check:
     - *Maps SDK for Android*
     - *Maps JavaScript API*
     - *Directions API*
     - *Distance Matrix API*
   - Under **Application restrictions**, add your Android package name:
     - Package name: `com.travelbuddy.app`
     - SHA-1 certificate fingerprint (from debug keystore or Play Console).

---

## 4. Plug the API Key into Travel Buddy

### A. Android App (`AndroidManifest.xml`)
Open `composeApp/src/androidMain/AndroidManifest.xml` and insert your API key inside `<application>`:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_GOOGLE_MAPS_API_KEY_HERE" />
```

### B. App Configuration (`AppConfig.kt`)
In `composeApp/src/commonMain/kotlin/com/travelbuddy/app/AppConfig.kt`, set your key:

```kotlin
data class AppConfig(
    val apiBaseUrl: String = "https://travelbuddy-api.onrender.com",
    val webSocketUrl: String? = "wss://travelbuddy-api.onrender.com/ws/live",
    val googleMapsApiKey: String = "YOUR_GOOGLE_MAPS_API_KEY_HERE"
)
```

### C. Backend API (`backend/src/main/resources/application.yml`)
Set the key in the Spring Boot backend environment or `application.yml`:

```yaml
google:
  maps:
    api-key: ${GOOGLE_MAPS_API_KEY:YOUR_GOOGLE_MAPS_API_KEY_HERE}
```
Or on **Render**: Add `GOOGLE_MAPS_API_KEY` under your Web Service's **Environment Variables**.

---

## 5. Live Tracking Features in Travel Buddy

Once configured, the app provides full real-time navigation and tracking:

| Feature | How It Works |
|---|---|
| **Google Maps View Modes** | Switch seamlessly between **Google Streets**, **Satellite Aerial**, and **Dark Navigation HUD** |
| **Live User Tracking** | Track trip members in real time, see their avatars, battery %, speed, and sharing status |
| **Share & Join with Code** | Users share a 6-digit code (e.g. `TRK-8821`) so friends or family can join and track live |
| **Vehicle GPS Tracker** | Live GPS telemetry (speedometer, engine status, battery 12.6V, geofence, engine lock) |
| **Tag Tracking** | Real-time tracking for child backpacks, luggage tags, and pet collars with safe-zone alerts |
| **Dynamic Haversine & ETA** | Accurate road distance calculation in km/m and estimated arrival time based on vehicle speed |
| **Real-time WebSocket Sync** | Continuous bidirectional location stream broadcasted over `/ws/live` every 4 seconds |
