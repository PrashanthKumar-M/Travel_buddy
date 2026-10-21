# Travel Buddy

Kotlin Multiplatform client for Android, iOS and web, built from the supplied product documentation and architecture. The shared Compose UI is in `composeApp/src/commonMain`; platform entry points are Android, iOS and Kotlin/Wasm web.

## Implemented product flows

- Live-trip home dashboard and member location-sharing state
- Trips, live-tracking map integration point, shared expenses and mathematically consistent settlement suggestions
- SOS state and privacy-focused location copy
- Vehicle/GPS repository contracts, including the four endpoints supplied in the Postman collection
- Local demo login: phone `9876543210`, OTP `123456`
- Local-first interactive trips, expenses, settlement, SOS contacts and vehicle telemetry matching the supplied mobile design; no backend is required to demo the flow
- Ktor HTTP client implementation for trips, live snapshot, location posting, expenses and settlements
- WebSocket location gateway and shared location-publish coordinator, ready for the exact endpoint/topic published by the backend

## Run

Install JDK 17+, Android Studio/IntelliJ with Kotlin Multiplatform support, Android SDK 36, and Xcode for iOS. The app opens in local demo mode by default. Enable remote API use only when the Spring Boot server is ready:

```properties
# ~/.gradle/gradle.properties or project gradle.properties
TRAVEL_BUDDY_API_URL=http://10.0.2.2:8080
```

Android emulators use `10.0.2.2` to reach a backend on the development machine. For a physical Android/iOS device, use an HTTPS URL or your computer's reachable LAN address. Enable it from the platform bootstrap with `AppConfig(useRemoteApi = true, apiBaseUrl = "...")`; otherwise all interactions stay local and functional.

The Ktor client uses the cross-platform CIO engine, avoiding an OkHttp dependency that requires Android API 37. Then use the platform run configuration or:

```sh
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

For iOS, expose `MainViewController()` from `composeApp/src/iosMain` in an Xcode host application. Supply the backend URL, map style URL, and authentication token at deployment time; no credentials are committed.

## Maps and real-time tracking

Use **MapLibre Native** on Android/iOS and **MapLibre GL JS** on web, with a MapTiler (or self-hosted) style/tile URL. Keep the style URL and API key outside source control. The app's `RealtimeLocationGateway` opens a Ktor WebSocket and converts location messages to `LiveLocationEvent`; configure its exact URL only after the Spring Boot service publishes the WebSocket endpoint and message topic. The supplied Postman collection confirms that location posts broadcast to WebSocket but does not define that address or payload envelope.

## Backend hand-off

The product document describes the intended REST endpoints for auth, trips, locations, expenses, settlement and SOS but gives no request/response schemas. `TravelRepository` is deliberately transport-independent until those schemas are available. `VehicleApiConfig` reflects the supplied Postman URLs and should be populated from a secure build-time configuration.
