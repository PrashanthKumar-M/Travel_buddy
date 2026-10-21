# Travel Buddy — Complete Spring Boot API Guide & Codebase

This document contains the complete Spring Boot 3.3.4 (JDK 21) API architecture, all controller routes, models, and real-time WebSocket broker code to deploy directly to **Render** on the **100% Free Tier ($0/month)**.

---

## 1. Project Directory Structure

```text
backend/
├── Dockerfile                     <-- Multi-stage build for Render Alpine container
├── build.gradle.kts               <-- Gradle build script with Spring Boot 3.3.4 dependencies
├── src/
│   ├── main/
│   │   ├── java/com/travelbuddy/api/
│   │   │   ├── TravelBuddyBackendApplication.java   <-- Application entry point with @EnableScheduling
│   │   │   ├── config/
│   │   │   │   ├── WebConfig.java                  <-- CORS configuration for mobile and web
│   │   │   │   └── WebSocketConfig.java            <-- /ws/live WebSocket endpoint configuration
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java             <-- /api/auth (OTP, login, verification)
│   │   │   │   ├── ChatController.java             <-- /api/chat (trip chat, messaging)
│   │   │   │   ├── DeviceController.java           <-- /api/devices (vehicle & tag telemetry)
│   │   │   │   ├── ExpenseController.java          <-- /api/expenses (trip split & personal split)
│   │   │   │   ├── HealthController.java           <-- /api/health (Render healthcheck)
│   │   │   │   ├── LocationController.java         <-- /api/locations (live GPS sync)
│   │   │   │   ├── ProfileController.java          <-- /api/profile & /api/auth/logout
│   │   │   │   ├── SafetyController.java           <-- /api/safety (emergency contacts & SOS)
│   │   │   │   └── TripController.java             <-- /api/trips (CRUD, covers, members)
│   │   │   ├── dto/
│   │   │   │   └── Dto.java                        <-- Request and response DTO records
│   │   │   ├── model/
│   │   │   │   └── Models.java                     <-- Domain models (Trip, Member, Expense, etc.)
│   │   │   ├── service/
│   │   │   │   └── TravelBuddyStateService.java    <-- State manager with 4-second GPS simulator
│   │   │   └── websocket/
│   │   │       └── LiveWebSocketHandler.java       <-- WebSocket session manager & broadcaster
│   │   └── resources/
│   │       └── application.properties              <-- Port ${PORT:8080}, Jackson, logging
render.yaml                                         <-- Render 1-click free web service specification
```

---

## 2. All REST API Endpoints

### Authentication & Profile (`/api/auth`, `/api/profile`)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/request-otp` | Request OTP for mobile number (`{"phone": "+91 98765 43210"}`) |
| `POST` | `/api/auth/verify-otp` | Verify OTP code (`{"phone": "+91...", "otp": "1234"}`) |
| `GET` | `/api/profile` | Get logged-in user profile (name, email, emergency contact, stats) |
| `PUT` | `/api/profile` | Update user profile info |
| `POST` | `/api/auth/logout` | Invalidate user session and logout |

### Trips (`/api/trips`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/trips` | Get list of all trips |
| `POST` | `/api/trips` | Create new trip with destination, dates, members, vehicle |
| `GET` | `/api/trips/{tripId}` | Get trip details |
| `GET` | `/api/trips/{tripId}/live` | Get live trip snapshot with real-time member locations |
| `POST` | `/api/trips/join` | Join trip using 6-digit trip code (`TRIP-XXXX`) |
| `POST` | `/api/trips/{tripId}/image` | Upload and set trip cover image |

### Live GPS Tracking & Locations (`/api/locations`, `/ws/live`)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/locations` | Push GPS location update (`latitude`, `longitude`, `speed`, `battery`) |
| `GET` | `/api/locations/{tripId}` | Get all latest member locations for a trip |
| `WS` | `/ws/live` | Real-time WebSocket stream of GPS location events every 4s |

### Group Split & Expenses (`/api/expenses`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/expenses?tripId={id}` | List all expenses for a trip |
| `GET` | `/api/expenses?groupId={id}` | List expenses for an expense group |
| `POST` | `/api/expenses` | Add a new expense (equal split, percentage, or custom) |
| `GET` | `/api/expenses/trips/{id}/settlements` | Calculate optimized settlement transfers (`who owes whom`) |
| `GET` | `/api/expenses/groups` | List expense groups (both Trip groups & Personal split groups) |
| `POST` | `/api/expenses/groups` | Create an expense group |

### Trip Chat (`/api/chat`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/chat/{tripId}` | Get chat message history for trip |
| `POST` | `/api/chat/{tripId}` | Send a message to the trip group |

### Vehicle & Tag Tracking (`/api/devices`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/devices` | List all tracked devices (Vehicle GPS, Child bag tags, Pet collars) |
| `GET` | `/api/devices/{id}` | Get telemetry for a specific device |
| `POST` | `/api/devices/{id}/telemetry` | Update telemetry (speed, fuel, battery, geofence status) |
| `POST` | `/api/devices` | Register a new tracking tag/device |

### Safety & Emergency SOS (`/api/safety`)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/safety/sos` | Trigger emergency SOS alert with current coordinates |
| `GET` | `/api/safety/sos` | Get active SOS emergency alerts |

### Healthcheck (`/api/health`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | Returns service status, uptime, and active trip count (used by Render) |

---

## 3. How to Deploy to Render for Free (Step-by-Step)

Render provides a **free web service tier** with free SSL, automated builds, and public URLs (`https://<service-name>.onrender.com`).

### Option A: 1-Click Git Push to Render (Recommended)
1. Push this project repository to **GitHub** or **GitLab**.
2. Go to [Render Dashboard](https://dashboard.render.com/) and click **New +** > **Blueprint**.
3. Select your repository. Render will automatically detect [`render.yaml`](file:///Users/prashanthkumar/Documents/Codex/2026-09-28/create-a-android-ios-web-for/render.yaml).
4. Click **Apply**. Render will:
   - Build the container using the provided multi-stage [`Dockerfile`](file:///Users/prashanthkumar/Documents/Codex/2026-09-28/create-a-android-ios-web-for/backend/Dockerfile).
   - Expose the port dynamically using the `$PORT` environment variable.
   - Run health checks against `/api/health`.
   - Provide your live HTTPS URL (e.g. `https://travelbuddy-api.onrender.com`).

### Option B: Manual Web Service on Render
1. In Render Dashboard, click **New +** > **Web Service**.
2. Connect your Git repository.
3. Choose **Docker** as the runtime.
4. Set **Docker Context** to `backend` (or root if building from repository root).
5. Set **Instance Type** to **Free**.
6. Click **Create Web Service**.

---

## 4. Testing Your Live API with cURL

Once deployed, test with these commands:

```bash
# 1. Healthcheck
curl https://your-service.onrender.com/api/health

# 2. Get All Trips
curl https://your-service.onrender.com/api/trips

# 3. Get Trip Live Snapshot
curl https://your-service.onrender.com/api/trips/trip-coorg-expedition/live

# 4. Get Dynamic Expenses
curl "https://your-service.onrender.com/api/expenses?tripId=trip-coorg-expedition"

# 5. Get Settlements
curl https://your-service.onrender.com/api/expenses/trips/trip-coorg-expedition/settlements

# 6. Send Chat Message
curl -X POST https://your-service.onrender.com/api/chat/trip-coorg-expedition \
  -H "Content-Type: application/json" \
  -d '{"senderId":"usr-1","senderName":"Prashant","message":"Live GPS tracking active!"}'

# 7. Test WebSocket
# Connect using wscat or any WebSocket client to:
# wss://your-service.onrender.com/ws/live
```
