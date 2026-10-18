# Travel Buddy Backend — Free Render Deployment Guide

This backend is a Spring Boot application featuring REST APIs and a real-time WebSocket broker (`/ws/live`) for live multi-user GPS tracking, vehicle telemetry, child bag tag safe zones, and instant expense splitting.

---

## 🚀 How to Host on Render for Free (Step-by-Step)

Render provides a free web service tier ideal for hosting this Dockerized Spring Boot service.

### Step 1: Push Project to GitHub / GitLab
Make sure your latest codebase (including `backend/` and `render.yaml`) is committed and pushed to your GitHub or GitLab repository:
```bash
git add .
git commit -m "Add Travel Buddy Spring Boot API and Render deployment setup"
git push origin main
```

---

### Step 2: Create a New Web Service on Render
1. Go to [https://dashboard.render.com](https://dashboard.render.com) and sign in (or sign up for a free account).
2. In the top navigation, click **New +** and select **Web Service**.
3. Under **Connect a repository**, find and select your `TravelBuddy` repository.

---

### Step 3: Configure Web Service Settings
Fill in the following details:

| Setting | Value |
|---|---|
| **Name** | `travelbuddy-api` (or any unique name) |
| **Region** | Singapore (or closest to your location) |
| **Branch** | `main` |
| **Root Directory** | Leave blank (or `.`) |
| **Runtime** | **Docker** |
| **Dockerfile Path** | `./backend/Dockerfile` |
| **Docker Context** | `.` |
| **Instance Type** | **Free** ($0/month) |

---

### Step 4: Add Environment Variables
Under **Environment Variables**, ensure the following keys are set:
- `PORT` = `8080` *(Render sets this automatically; application adapts dynamically)*
- `SPRING_PROFILES_ACTIVE` = `prod`

---

### Step 5: Deploy
1. Click **Deploy Web Service** at the bottom of the page.
2. Render will trigger the multi-stage Docker build, compile the distribution using JDK 21, and launch the service.
3. Once the build finishes, you will see a green **Live** status with your URL:
   ```
   https://travelbuddy-api.onrender.com
   ```
4. Verify by visiting:
   ```
   https://travelbuddy-api.onrender.com/api/health
   ```
   You will receive:
   ```json
   {"groupsCount":2,"uptimeSeconds":15,"tripsCount":1,"devicesCount":3,"status":"UP"}
   ```

---

## 📱 Connecting the App (Android, iOS & Web)

In [`AppConfig.kt`](file:///Users/prashanthkumar/Documents/Codex/2026-09-28/create-a-android-ios-web-for/composeApp/src/commonMain/kotlin/com/travelbuddy/app/AppConfig.kt), you can point the app to your live Render endpoint:

```kotlin
// Example in MainActivity or app bootstrap:
val config = AppConfig.forRender("https://travelbuddy-api.onrender.com")
TravelBuddyApp(config)
```

---

## 📡 Live API Endpoints Reference

### 1. Authentication
- `POST /api/auth/request-otp` — Request 6-digit OTP
- `POST /api/auth/verify-otp` — Verify OTP and receive session token

### 2. Live Trips & Code Joining
- `POST /api/trips` — Create new trip (generates 6-character code e.g. `TRIP-7489`)
- `GET /api/trips` — List all active trips
- `GET /api/trips/{id}/live` — Live snapshot of members and coordinates
- `POST /api/trips/join` — Join trip session using 6-character code

### 3. Live Tracking & WebSocket
- `POST /api/locations` — Broadcast user location update
- `GET /api/locations/{tripId}` — Snapshot of members in trip
- `WebSocket /ws/live` — Bi-directional real-time streaming of GPS coordinates, battery, and speed

### 4. Realtime Expense Splitting
- `GET /api/expenses/groups` — List split groups (both Trip Groups & Manual Personal Groups)
- `POST /api/expenses/groups` — Create manual personal group without a trip
- `POST /api/expenses` — Add expense with Equal or Custom split
- `GET /api/expenses/groups/{groupId}/settlements` — Realtime minimum cash flow debt settlements
- `POST /api/expenses/settle` — Mark debt as settled

### 5. Device Tracking (Vehicle & Child Bag Tags)
- `GET /api/devices` — List registered vehicle and child bag devices
- `POST /api/devices` — Register new IoT tracker / bag tag
- `POST /api/devices/{id}/telemetry` — Push hardware telemetry (speed, ignition, safe zone status)

### 6. Emergency Safety
- `POST /api/safety/sos` — Broadcast immediate emergency alert with GPS
