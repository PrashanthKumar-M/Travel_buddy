# Backend contract needed for a fully live Travel Buddy release

The supplied Spring Boot Postman collection supports these client calls:

- `POST /api/auth/verify-otp`
- `POST /api/trips`
- `GET /api/trips/{tripId}/live`
- `POST /api/locations`
- `POST /api/expenses`
- `GET /api/expenses/trips/{tripId}/settlements`

The Kotlin client now calls those routes. The following product features cannot be connected safely until the Spring Boot service publishes their exact REST/WebSocket contract:

| Product feature | Required backend contract |
| --- | --- |
| Send OTP / authenticated session | `POST /api/auth/request-otp`, verify response schema, JWT/refresh-token policy |
| Join/invite/manage trip members | trip list, detail, invite and join endpoints |
| Live updates | WebSocket URL, authentication, subscription topic, reconnect behavior, message envelope, and error events |
| SOS | SOS route, recipient rules, acknowledgement response and emergency-service disclaimer |
| Chat and notifications | message history/send endpoints, WebSocket topics, push-token registration |
| Vehicle telemetry | device registry, current location/history endpoints and device credential policy |
| Maps | server-side authorization for route, geocoding and member/vehicle location access; no map key should be returned to untrusted users unless restricted |

## Map decision

Use MapLibre Native (Android/iOS) and MapLibre GL JS (web) with a restricted MapTiler or self-hosted style URL. The mobile app should expose a platform-specific map surface behind a shared KMP interface. The server remains the authority for which trip members may receive a location, and it should include a location freshness timestamp in every snapshot/event.

## Environment values

- Android emulator backend: `http://10.0.2.2:8080`
- iOS simulator/web development backend: `http://localhost:8080`
- Device/staging/production: an HTTPS URL reachable from the device
- WebSocket URL: deployment-provided `wss://...` value; do not infer it from an HTTP route
