# 🏛️ Dharohar Setu — AI-Powered Heritage Travel Companion

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android" />
  <img src="https://img.shields.io/badge/Backend-FastAPI-009688?style=for-the-badge&logo=fastapi" />
  <img src="https://img.shields.io/badge/ChatBot-GPT--4o--mini-blueviolet?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Database-PostgreSQL-336791?style=for-the-badge&logo=postgresql" />
  <img src="https://img.shields.io/badge/Auth-Firebase-FFCA28?style=for-the-badge&logo=firebase" />
  <img src="https://img.shields.io/badge/Version-1.0%20Prototype-orange?style=for-the-badge" />
</p>

> **Dharohar Setu** (धरोहरसेतु) bridges India's rich cultural heritage with the modern traveller through AI, voice technology, and location awareness — transforming every heritage site visit into an immersive, personalised experience.

---

## 📖 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Setup & Installation](#setup--installation)
  - [Backend (FastAPI)](#backend-fastapi)
  - [Android App](#android-app)
- [API Reference](#api-reference)
- [Database Schema](#database-schema)
- [Key Flows](#key-flows)
- [Environment Variables](#environment-variables)
- [Team](#team)

---

## Overview

Dharohar Setu is a research prototype developed at IIIT Sonepat. It uses GPS geofencing, QR code scanning, and AI to provide visitors of Indian heritage sites with context-aware audio guides, chatbot assistance, and trip tracking — all in English, Hindi, and Hinglish.

The system consists of two main parts:

- **Android App** — Jetpack Compose-based native app with MapLibre maps, Firebase Auth, and AI chat/voice interfaces.
- **FastAPI Backend** — Python server deployed on Render that handles site discovery, trip management, AI orchestration (STT → LLM → TTS), and user reviews.

---

## Features

### 🗺️ Location & Discovery
- **GPS Geofencing** — Automatically detects when a user enters/exits a heritage site boundary using OS-managed geofences and Google Play Services.
- **Activity Recognition** — Suppresses false geofence triggers when the user is driving past a site (requires ≥70% IN_VEHICLE confidence).
- **Nearby Sites Map** — Interactive MapLibre map showing heritage sites sorted by distance with live haversine calculations from the backend.

### 📱 Heritage Exploration
- **Site Detail Screen** — Gallery of site images, weather widget (Open-Meteo), 7-day forecast, history, overview, and fun facts with per-section TTS narration.
- **Buy Ticket** — In-app ticket tier selection (Free / Student / Adult / Foreign) with redirect to the ticketing portal.
- **Intro Video** — Direct video playback for sites that have an intro video URL seeded.

### 📷 QR-Based Trip System
- **Node QR Scan** — Camera-based QR scanner (ML Kit) reads node QR codes placed at spots within a heritage site.
- **King Node** — Scanning the main entrance QR starts a trip and records it on the server.
- **Trip Progress** — Tracks which nodes the user has visited, shown on a live directions map with colour-coded markers.
- **Trip Completion** — On trip end, shows personalised recommendations (monuments, hotels, restaurants) sorted by distance.

### 🤖 AI Guide — SHREE
- **Text Chat** — Conversational AI guide powered by GPT-4o-mini via OpenRouter. Uses a 2-level context system:
  - **Level 1** (geofence entered, no QR): site-wide context prompt.
  - **Level 2** (QR scanned): node-specific context prompt for that exact spot.
- **Voice Chat** — Full voice pipeline: WAV recording → Sarvam STT → GPT-4o-mini → Sarvam TTS → audio playback.
- **Multilingual** — English, Hindi (Devanagari), and Hinglish supported for both text and voice.

### 🔐 Authentication
- Email/Password sign-up and sign-in (Firebase Auth).
- Google Sign-In.
- Passwordless email link login.
- Anonymous / Guest access.

### ⭐ Reviews & Analytics
- Post-trip review form (star rating + 3 question scales).
- PostgreSQL triggers automatically compute aggregate analytics (`analyzed_responses` table) on every review submission.
- Per-user visit history with nodes visited, duration, and review status.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Android App                          │
│                                                         │
│  MapLibre Map ──► GPS/Geofence ──► LocationBasedSiteDetector
│       │                                    │            │
│  QR Scanner ─────────────────────────► ActiveSiteManager
│       │                                    │            │
│  ChatbotActivity ◄─── site_id / node_id ───┘            │
│  VoiceChatActivity                                      │
│       │                                                 │
│       └──── Retrofit + OkHttp ──────────────────────┐  │
└─────────────────────────────────────────────────────│──┘
                                                       │
                        HTTPS                          │
                                                       ▼
┌─────────────────────────────────────────────────────────┐
│              FastAPI Backend (Render)                   │
│                                                         │
│  /sites/nearby  /sites/{id}  /sites/scan/{qr}           │
│  /chat/         /voice-chat                             │
│  /trips/start   /trips/end                              │
│  /reviews/submit                                        │
│  /admin/seed-bulk  /admin/seed-prompt                   │
│                                                         │
│  ┌──────────┐  ┌───────────┐  ┌──────────────────────┐ │
│  │ Sarvam   │  │OpenRouter │  │    PostgreSQL DB      │ │
│  │ STT+TTS  │  │ GPT-4o    │  │  heritage_sites       │ │
│  └──────────┘  └───────────┘  │  nodes / node_images  │ │
│                                │  trips / reviews      │ │
│                                │  prompts              │ │
│                                └──────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Android UI | Kotlin + Jetpack Compose (Material 3) |
| Maps | MapLibre GL Android SDK 11.0.0 |
| Location | Google Play Services FusedLocationProvider + Geofencing API |
| Camera / QR | CameraX + ML Kit Barcode Scanning |
| Networking | Retrofit 2 + OkHttp 4 |
| Auth | Firebase Authentication |
| Media | ExoPlayer (Media3) + Android MediaPlayer / AudioRecord |
| Image Loading | Coil 2 |
| Backend | Python 3.11 + FastAPI 0.115 + Uvicorn |
| Database | PostgreSQL + SQLAlchemy 2.0 |
| AI LLM | GPT-4o-mini via OpenRouter |
| STT / TTS | Sarvam AI (saarika:v2.5 STT, bulbul:v3 TTS) |
| Hosting | Render.com |
| Weather | Open-Meteo (free, no API key) |

---

## Project Structure

```
dharohar-setu/
│
├── app/                          # FastAPI backend
│   ├── main.py                   # App entry point, CORS, router registration
│   ├── database.py               # SQLAlchemy engine + session
│   ├── models.py                 # ORM models (HeritageSite, Node, Trip, etc.)
│   ├── schemas.py                # Pydantic request/response models
│   ├── utils.py                  # Haversine distance helper
│   ├── db_triggers.py            # PostgreSQL triggers for review analytics
│   ├── routers/
│   │   ├── sites.py              # GET /sites/nearby, /sites/{id}, /sites/scan/{qr}
│   │   ├── trips.py              # POST /trips/start, /trips/end
│   │   ├── chat.py               # POST /chat/ — AI text guide
│   │   ├── voice.py              # POST /voice-chat — AI voice guide
│   │   ├── reviews.py            # POST /reviews/submit, GET /reviews/...
│   │   └── admin.py              # POST /admin/seed-bulk, /admin/seed-prompt
│   └── services/
│       ├── openrouter.py         # GPT-4o-mini via OpenRouter
│       ├── sarvam_stt.py         # Sarvam speech-to-text
│       ├── sarvam_tts.py         # Sarvam text-to-speech
│       └── voice_orchestrator.py # STT → LLM → TTS pipeline
│
├── app/src/main/java/com/example/humsafar/   # Android app
│   ├── MainActivity.kt           # Entry point, email link handling
│   ├── ChatbotActivity.kt        # Text AI chat screen
│   ├── VoiceChatActivity.kt      # Voice AI screen launcher
│   ├── auth/AuthManager.kt       # Firebase auth singleton
│   ├── data/
│   │   ├── ActiveSiteManager.kt  # Single source of truth: current site + node
│   │   ├── TripManager.kt        # Persistent trip state (SharedPrefs)
│   │   ├── HeritageRepository.kt # Static site list (fallback for geofences)
│   │   └── LocationBasedSiteDetector.kt  # GPS → /sites/nearby → ActiveSiteManager
│   ├── geofence/                 # OS geofence registration + transition handling
│   ├── location/                 # FusedLocationProvider wrapper
│   ├── audio/                    # AudioRecorder (WAV) + AudioPlayer (MediaPlayer)
│   ├── network/
│   │   ├── HumsafarApiService.kt # Main Retrofit interface
│   │   ├── HumsafarClient.kt     # OkHttp client + 307 redirect fix
│   │   └── VoiceRetrofitClient.kt # Separate client with long timeouts for voice
│   ├── models/                   # API data models + UI models
│   ├── navigation/AppNavigation.kt # Compose Nav graph
│   ├── prefs/LanguagePreferences.kt
│   └── ui/                       # All Compose screens + ViewModels
│       ├── MapScreen.kt
│       ├── HeritageDetailScreen.kt + HeritageDetailViewModel.kt
│       ├── NodeDetailScreen.kt + NodeDetailViewModel.kt
│       ├── QrScanScreen.kt + QrScanViewModel.kt
│       ├── VoiceChatScreen.kt + VoiceChatViewModel.kt
│       ├── DirectionsScreen.kt
│       ├── ReviewScreen.kt + ReviewViewModel.kt
│       ├── TripCompletionScreen.kt
│       ├── SiteInfoScreen.kt
│       ├── LoginScreen.kt / SignUpScreen.kt / ProfileScreen.kt
│       └── components/           # GlassCard, AnimatedOrbBackground, TripInfoButton…
│
├── Dockerfile
├── requirements.txt
└── runtime.txt
```

---

## Setup & Installation

### Backend (FastAPI)

**Prerequisites:** Python 3.11+, PostgreSQL

1. **Clone and install dependencies**
   ```bash
   git clone <repo-url>
   cd dharohar-setu
   pip install -r requirements.txt
   ```

2. **Configure environment variables** — create a `.env` file:
   ```env
   DATABASE_URL=postgre_external_connector_url
   OPENROUTER_API_KEY=your_openrouter_key
   SARVAM_API_KEY=your_sarvam_key
   ```

3. **Run the server**
   ```bash
   uvicorn app.main:app --host 0.0.0.0 --port 8080 --reload
   ```
   The server will automatically create all database tables on startup.

4. **Seed your first site** using the admin endpoint:
   ```bash
   curl -X POST http://localhost:8080/admin/seed-bulk \
     -H "Content-Type: application/json" \
     -d '{
       "site": {
         "name": "Qutub Minar Complex",
         "latitude": 28.5245,
         "longitude": 77.1855,
         "radius": 600,
         "summary": "...",
         "history": "..."
       },
       "nodes": [
         {
           "name": "Main Entrance",
           "latitude": 28.5240,
           "longitude": 77.1850,
           "sequence": 0,
           "qr": "qutub-main-entrance",
           "is_king": true
         }
       ]
     }'
   ```

5. **Seed the AI context prompt** for the site:
   ```bash
   curl -X POST http://localhost:8080/admin/seed-prompt \
     -H "Content-Type: application/json" \
     -d '{
       "site_id": 1,
       "prompt_text": "Detailed heritage context for SHREE to use..."
     }'
   ```

#### Docker deployment
```bash
docker build -t dharohar-backend .
docker run -p 8080:8080 --env-file .env dharohar-backend
```

---

### Android App

**Prerequisites:** Android Studio Hedgehog+, JDK 17, Android device/emulator API 29+

1. **Firebase setup**
   - Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
   - Enable Authentication (Email/Password, Google, Email Link)
   - Download `google-services.json` and place it in `app/`
   - In `AndroidManifest.xml`, replace `dharoharsetu` with your Firebase Project ID

2. **MapTiler API key**
   - Get a free key at [maptiler.com](https://maptiler.com)
   - Add to `local.properties`:
     ```
     MAPTILER_KEY=your_maptiler_key
     ```

3. **Backend URL**
   - In `app/src/main/java/com/example/humsafar/network/HumsafarClient.kt`, update:
     ```kotlin
     private const val BASE_URL = "https://your-backend-url.onrender.com/"
     ```

4. **Google Sign-In client ID**
   - In `AuthManager.kt`, replace the `requestIdToken` value with your OAuth 2.0 Web client ID from Firebase console.

5. **Build and run**
   ```
   ./gradlew assembleDebug
   ```
   Or open in Android Studio and click Run.

---

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/sites/nearby?lat=&lng=&max_range_km=` | Sites within range, sorted by distance |
| `GET` | `/sites/{site_id}` | Full site detail with nodes and images |
| `GET` | `/sites/{site_id}/nodes` | Node positions for the directions map |
| `GET` | `/sites/scan/{qr_value}` | Validate a QR code and return site/node IDs |
| `GET` | `/sites/{site_id}/recommendations` | Hotels, restaurants, monuments near a site |
| `POST` | `/chat/` | Text AI chat (JSON body with site_id, node_id, message, history) |
| `POST` | `/voice-chat` | Voice pipeline (multipart: audio WAV + site_id + node_id + language) |
| `POST` | `/trips/start?user_id=&qr_value=` | Start a trip from a King node QR scan |
| `POST` | `/trips/end?trip_id=` | End a trip, record visit history |
| `POST` | `/reviews/submit` | Submit star rating + 3-question review |
| `GET` | `/reviews/sites/{site_id}/summary` | Aggregate review analytics for a site |
| `GET` | `/reviews/users/{user_id}/history` | User's visit history |
| `POST` | `/admin/seed-bulk` | Seed a site with nodes, images, and landmarks |
| `POST` | `/admin/seed-prompt` | Add/update the AI context prompt for a site or node |

Interactive docs available at `http://localhost:8080/docs` (Swagger UI).

---

## Database Schema

```
heritage_sites          site_images
──────────────          ───────────
id (PK)                 id, site_id (FK), image_url, display_order
name, latitude, longitude
geofence_radius_meters  nodes
summary, history        ─────
fun_facts               id (PK), site_id (FK), name
helpline_number         latitude, longitude, sequence_order
intro_video_url         is_king, qr_code_value
rating, upvotes         description, video_url, image_url

node_images             prompts
───────────             ───────
id, node_id (FK)        id, site_id (FK), node_id (FK, nullable)
image_url               title, content
display_order

trips                   user_visit_history
─────                   ──────────────────
id (PK)                 id, user_id, site_id, trip_id
user_id, site_id (FK)   nodes_visited[], duration_mins
started_at, ended_at    entry_lat, entry_lng, review_submitted
is_active

site_ratings            trip_reviews
────────────            ────────────
id, site_id, user_id    id, trip_id (UNIQUE), site_id, user_id
rating                  q1_overall_experience (1-5)
                        q2_guide_helpfulness (1-5)
analyzed_responses      q3_recommend_to_others (1-5)
──────────────────      suggestion_text
id, site_id (UNIQUE)
avg_star_rating         recommendations
total_ratings           ───────────────
avg_overall_experience  id, site_id, type (monument/hotel/restaurant)
satisfy_label           name, description, latitude, longitude
```

PostgreSQL triggers automatically update `analyzed_responses` whenever `site_ratings` or `trip_reviews` are modified.

---

## Key Flows

### User Enters Heritage Site
```
GPS tick (every 3s)
  → LocationBasedSiteDetector.onLocationUpdate()
  → GET /sites/nearby?lat=&lng=&max_range_km=10
  → Backend haversine SQL → inside_geofence=true
  → ActiveSiteManager.onEnterSite(site)
  → OS Geofence DWELL trigger (after 30s inside boundary)
  → GeofenceTransitionReceiver checks CurrentActivityHolder
  → If NOT IN_VEHICLE → show notification + update UI
```

### QR Scan → Start Trip
```
Camera frame → ML Kit barcode detection
  → viewModel.onQrDetected(qrValue)
  → GET /sites/scan/{qr_value} → { site_id, node_id, is_king }
  → If is_king: POST /trips/start?user_id=&qr_value=
  → TripManager.activateTrip(tripId, siteId, nodeId)
  → ActiveSiteManager.onNodeScanned(nodeId)
  → Navigate to NodeDetailScreen
```

### Voice Chat Pipeline
```
User holds mic button
  → AudioRecorder.startRecording() (16kHz WAV)
User releases
  → AudioRecorder.stopRecording()
  → POST /voice-chat (multipart: audio + site_id + node_id + language)
  → Backend: Sarvam STT → transcript
  → Backend: GPT-4o-mini with heritage context
  → Backend: Sarvam TTS → WAV bytes → base64
  → App: AudioPlayer.play(base64)
```

### AI Context Hierarchy (Chat & Voice)
```
1. Node-specific Prompt row   (Prompt.node_id = scanned node)
       ↓ if not found
2. Site-level Prompt row      (Prompt.node_id = NULL)
       ↓ if not found
3. HeritageSite DB columns    (summary + history + fun_facts)
```

---

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `DATABASE_URL` | ✅ | PostgreSQL connection string |
| `OPENROUTER_API_KEY` | ✅ | OpenRouter API key for GPT-4o-mini |
| `SARVAM_API_KEY` | ✅ | Sarvam AI key for STT and TTS |
| `SARVAM_TTS_MODEL` | ❌ | TTS model (default: `bulbul:v3`) |
| `SARVAM_TTS_SPEAKER` | ❌ | TTS speaker voice (default: `ritu`) |

Android (`local.properties`):

| Variable | Required | Description |
|---|---|---|
| `MAPTILER_KEY` | ✅ | MapTiler API key for map tiles |

---

## Team

| Role | Person |
|---|---|
| Developer | Sameet Patro |
| Developer | Harsh Kaldoke |
| Mentor | Dr. Mukesh Mann, IIIT Sonepat |

---

## ⚠️ Prototype Notice

This is a research prototype developed by Sameet Patro and Harsh Kaldoke for academic purposes at IIIT Sonepat. Features may be incomplete. The backend runs on Render's free tier and may have cold-start latency of 30–60 seconds on the first request. Not intended for production use.

---

<p align="center">
  <img src="https://media.tenor.com/DtD4LZbctTIAAAAM/tamm-cat.gif" alt="Thank You" width="200" />
