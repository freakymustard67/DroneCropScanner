# 🛩️ Drone Crop Scanner — Team Report

**Project:** AI-Powered Autonomous Drone Crop Health Scanner  
**Date:** July 1, 2026  
**Prepared by:** Abhishek K  
**Status:** Research & Design Phase (Pre-Development)

---

## 1. Vision & Problem Statement

### What We're Building

An Android application that automates the entire workflow of agricultural field scanning using a drone:

1. Farmer opens the app → sees a high-resolution satellite view of their location
2. App **automatically detects** the field boundary from satellite imagery
3. Farmer confirms or adjusts the boundary (can also walk the perimeter with GPS)
4. App generates an optimal **lawnmower scan path** within the boundary
5. Drone receives the mission and **flies autonomously**, scanning the entire field
6. **AI analyzes** the captured footage in real-time and post-flight
7. Drone performs a **second pass** — revisiting flagged problem areas for close-up inspection
8. Farmer sees a **visual dashboard** with disease maps, pest alerts, and treatment advice

### Why This Matters

- Manual field inspection is slow, inconsistent, and misses problems until it's too late
- Existing drone solutions (DJI Agriculture, etc.) cost ₹5–15 lakhs+
- We're building this with a **₹3,000–5,000 budget drone + ₹800 in added hardware**
- The AI layer is the real differentiator — not the hardware

### Target Users

- Small and medium-scale farmers in the Chennai region (and eventually wider Tamil Nadu/India)
- Crop types: Rice, vegetables, sugarcane, and other crops common in the Red Hills / semi-agricultural outskirts

---

## 2. System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│                        ANDROID APP (Kotlin + Jetpack Compose)                │
│                                                                             │
│  ┌───────────────┐   ┌─────────────────┐   ┌────────────────────────────┐   │
│  │  MAP MODULE    │   │  BOUNDARY       │   │  FLIGHT PATH GENERATOR     │   │
│  │               │   │  DETECTION      │   │                            │   │
│  │  ESRI tiles   │   │  CV auto-detect │   │  Boustrophedon algorithm   │   │
│  │  Google Maps  │   │  Manual adjust  │   │  Altitude & overlap config │   │
│  │  Mapbox       │   │  GPS perimeter  │   │  Battery-aware splitting   │   │
│  │  Sentinel-2   │   │  walk           │   │  Waypoint serialization    │   │
│  │  (NDVI overlay│   │                 │   │                            │   │
│  └───────────────┘   └─────────────────┘   └────────────────────────────┘   │
│                                                                             │
│  ┌───────────────┐   ┌─────────────────┐   ┌────────────────────────────┐   │
│  │  DRONE COMM   │   │  AI ENGINE      │   │  DASHBOARD & REPORTS       │   │
│  │  (Pluggable)  │   │                 │   │                            │   │
│  │               │   │  TFLite         │   │  Field health heatmap      │   │
│  │  Mock / Sim   │   │  (on-device)    │   │  Disease classification    │   │
│  │  ESP32 WiFi   │   │                 │   │  Historical trends         │   │
│  │  MAVLink      │   │  Cloud APIs     │   │  Treatment recommendations │   │
│  │  DJI SDK      │   │  (Gemini, Groq) │   │  Alert notifications       │   │
│  │  (future)     │   │                 │   │                            │   │
│  └───────────────┘   └─────────────────┘   └────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                              │ Wi-Fi │
                    ┌─────────┴───────┴──────────┐
                    │      ESP32 ON DRONE         │
                    │                             │
                    │  GPS (NEO-6M)               │
                    │  Mission Executor            │
                    │  Telemetry Broadcaster       │
                    │  Safety / Return-to-Home     │
                    └─────────────────────────────┘
```

> [!NOTE]
> The **Drone Communication module** is designed as a pluggable adapter. We'll build the entire app with a simulated drone interface first, and plug in the real protocol later once we identify it from the drone's existing smartphone app.

### What's Left / Room for Improvement
- The adapter pattern needs a clean interface definition (e.g., `DroneController` interface with methods like `uploadMission()`, `arm()`, `getStatus()`, `getVideoStream()`)
- Need to decide on a local database (Room? SQLite?) for storing field data, scan history, and AI results
- No backend/server has been planned yet — decide if we need one (for multi-user, cloud sync, etc.) or keep it fully on-device

---

## 3. Drone Hardware

### Current Hardware

We're working with a budget consumer quadcopter with the following specs:

| Spec | Value |
|---|---|
| **Cameras** | Dual 720p HD (front + adjustable bottom) |
| **Motors** | Brushless 1503 (quiet) |
| **Stabilization** | Optical-flow positioning + 6-axis gyro |
| **Battery** | 3.7V 1800mAh LiPo |
| **Flight Time** | 12–15 minutes |
| **Wi-Fi Range** | 20–30 meters (video + control) |
| **Radio Range** | 150–200 feet / ~45–60m (control only, no video) |
| **Features** | One-key takeoff/landing, headless mode, altitude hold, waypoint flight, gesture control |
| **GPS** | ❌ **Not present** (uses optical flow only) |

### Critical Limitation

The phone can **only** communicate with the drone via Wi-Fi (20–30m range). The 2.4GHz radio is a separate hardware link in the physical remote controller — the phone has no access to it. This means:

- Real-time phone control is limited to a ~20–30m radius
- For autonomous field scanning, we need onboard GPS navigation

### Planned Hardware Addition: ESP32 + GPS Module

| Component | Model | Approx. Cost | Purpose |
|---|---|---|---|
| ESP32 Dev Board | ESP32-WROOM-32 | ~₹300 | Main brain — Wi-Fi + BLE + compute |
| GPS Module | NEO-6M or BN-880 | ~₹200–400 | Continuous position tracking |
| Micro SD Module | SPI SD Card Adapter | ~₹50–100 | Store mission plan + log GPS-stamped data |
| Voltage Regulator | AMS1117 3.3V | ~₹20 | Power from drone's 3.7V LiPo |
| **Total** | | **~₹570–820** | |

### ESP32 Integration Options

| Approach | Description | Difficulty | Risk |
|---|---|---|---|
| **ESP32 as Pilot** | Sits between receiver and flight controller. Sends "joystick" commands (forward/left/right) to the existing FC. Original FC still handles motor mixing and stabilization. | Medium | Low — preserves existing stability |
| **ESP32 replaces FC** | Directly controls all 4 motors. Must implement PID stabilization, gyro fusion, motor mixing from scratch. | Very Hard | High — one bug = crash |

> [!IMPORTANT]
> **Decision pending.** We need to physically inspect the drone's flight controller board to determine which approach is feasible. The "Pilot" approach is strongly preferred.

### ESP32 Firmware Modules (Planned)

```
ESP32 Firmware
├── wifi_ap.cpp          — Creates Wi-Fi AP for phone connection
├── mission_receiver.cpp — Receives waypoint list from phone (JSON)
├── gps_reader.cpp       — Parses NMEA sentences from NEO-6M via UART
├── navigator.cpp        — PID controller: current GPS → target waypoint → heading/speed
├── fc_interface.cpp     — Sends PWM/PPM signals to flight controller
├── telemetry.cpp        — Broadcasts GPS + battery + status back to phone
├── sd_logger.cpp        — Logs GPS coordinates synced to timestamps
└── safety.cpp           — Geofence, low-battery RTH, signal-loss RTH
```

### What's Left / Room for Improvement
- **Drone app identification**: We need to find the name of the smartphone app that currently controls this drone. This will help us understand (or reverse-engineer) its Wi-Fi protocol
- **Weight budget**: The ESP32 + GPS + SD card module weighs ~15–20g. Need to verify the drone can handle the added payload without drastically reducing flight time
- **Camera trigger sync**: We need a way to timestamp video frames with GPS coordinates. Options: (a) ESP32 logs GPS at high frequency and we correlate via timestamps, or (b) ESP32 triggers the camera shutter directly
- **Antenna placement**: GPS antenna needs a clear view of the sky — placement on the drone matters for accuracy
- **Power draw**: ESP32 draws ~100–240mA. On a 1800mAh battery, this could reduce flight time by 1–2 minutes. Consider a separate small LiPo cell for the ESP32
- **Multiple batteries / mission resume**: With only 12–15 min of flight time, a 1-acre field may need 2–3 flights. The app should support "resume scan from where we stopped after battery swap"

---

## 4. Satellite Imagery & Map Display

### Strategy: Multi-Provider, Resolution-First

We use **multiple satellite imagery providers** in a tiered approach:

**Default View (highest resolution, may be months old):**

| Provider | Resolution | Cost | Notes |
|---|---|---|---|
| **ESRI World Imagery** | ~0.3–0.6m | Free tile server, no API key needed | Best free option — uses Maxar/Airbus data |
| **Google Maps SDK** | ~0.3–0.6m | Free: 28,500 map loads/month | Best documented, largest user base |
| **Mapbox Satellite** | ~0.5m | Free: 25k MAU | Best offline/caching support |

**Fresher Imagery (if farmer flags the image as outdated):**

| Provider | Resolution | Cost | Freshness |
|---|---|---|---|
| **Sentinel-2 (Copernicus)** | 10m | Free | 5-day revisit — good for NDVI overlay, not boundary marking |
| **Planet SkySat** | 0.5m | ~$8/km² | Days–weeks old — best balance of fresh + high-res |
| **Airbus Pléiades Neo** | 0.3m | Expensive | Days old — highest quality commercial option |

> [!TIP]
> At **0.3–0.6m resolution**, individual field bunds (raised earth ridges), irrigation channels, tree lines, paths, and even individual trees are clearly visible. This is more than sufficient for boundary marking. Sentinel-2 at 10m/pixel is far too coarse for boundaries but excellent for NDVI crop health overlays.

### How It Works in the App

```
1. App opens → fetches user's GPS location
2. Displays ESRI/Google high-res satellite tile as base layer
3. Farmer can optionally toggle a Sentinel-2 NDVI overlay (color-coded crop health)
4. If farmer says image is old → app offers to pull Planet SkySat (paid) or latest Sentinel
5. Boundary detection runs on whatever imagery is currently displayed
```

### What's Left / Room for Improvement
- **Offline tile caching**: Farmers may be in rural areas with poor connectivity. We should pre-download and cache satellite tiles for the area around the field. Mapbox has the best built-in offline support for this
- **Sentinel-2 integration**: We already have a working Python script ([sentinel_monitor.py](file:///home/abhishek/crop%20monitoring/sentinel_monitor.py)) that fetches Sentinel-2 imagery. This needs to be adapted into a backend API or on-device library for the Android app
- **Image recency indicator**: Display the capture date of the satellite imagery so the farmer knows how old the image is
- **Night/low-light**: Satellite imagery won't help for pre-dawn/dusk flights — may need an alternative positioning method
- **Map provider failover**: If one tile provider is down, auto-switch to another

---

## 5. Boundary Detection

### Three Methods (All Available in App)

#### Method A: Automatic Detection from Satellite Imagery (Primary)

The app analyzes the satellite tile and automatically detects field boundaries using computer vision:

```
Satellite Image → Edge Detection (Canny/Sobel)
                → Contour Extraction
                → Polygon Simplification
                → Boundary Polygon Overlay on Map
                → Farmer confirms or adjusts
```

**Algorithms to evaluate:**
- Classical CV: Canny edge detection + contour finding (OpenCV)
- Deep learning: U-Net or DeepLab segmentation model trained on agricultural parcels
- Hybrid: NDVI thresholding + edge detection (vegetation boundary = field boundary)

#### Method B: Manual Drawing (Fallback)

Farmer taps corners of the field on the satellite map. The app connects the taps into a polygon.

#### Method C: GPS Perimeter Walk (Backup)

Farmer physically walks around the field boundary while the app records GPS coordinates every few seconds. The recorded track becomes the boundary polygon.

### What's Left / Room for Improvement
- **Auto-detection model**: We haven't selected or trained a boundary detection model yet. Options:
  - **OpenCV classical approach** — simplest, works for well-defined boundaries (clear bunds, water channels), but struggles with subtle boundaries
  - **Pre-trained segmentation model** — Facebook's SAM (Segment Anything Model) could work as a zero-shot boundary detector on satellite tiles
  - **Fine-tuned model** — Train a U-Net on agricultural parcel datasets (e.g., [AI4Boundaries](https://data.jrc.ec.europa.eu/dataset/9d0587b6-b40c-47cb-9edf-8b67c1cde883) from EU Joint Research Centre)
- **Accuracy validation**: How do we measure if the auto-detected boundary is accurate? Compare against GPS perimeter walk?
- **Complex field shapes**: L-shaped fields, fields with trees/ponds inside, fields adjacent to roads — these are common in Chennai and challenging for auto-detection
- **Boundary editing UX**: The manual adjustment interface needs careful design — dragging polygon vertices on a phone screen is fiddly. Consider: snap-to-edge, undo/redo, vertex add/delete

---

## 6. Flight Path Generation

### Algorithm: Boustrophedon (Lawnmower) Decomposition

Once the boundary polygon is defined, the app generates a flight path:

```
Boundary Polygon
    ↓
Compute bounding rectangle
    ↓
Generate parallel scan lines (spacing = image footprint width × (1 - overlap%))
    ↓
Clip scan lines to boundary polygon
    ↓
Connect lines in boustrophedon (alternating direction) pattern
    ↓
Add takeoff point, landing point, and return-to-home waypoint
    ↓
Output: Ordered list of GPS waypoints
```

**Parameters:**

| Parameter | Default | Notes |
|---|---|---|
| Altitude | 10–15m | Lower = better image quality, shorter coverage per pass |
| Overlap | 70% forward, 30% side | Ensures full coverage with no gaps |
| Speed | 2–3 m/s | Slow enough for 720p to capture usable frames |
| Scan line direction | Automatic (along longest axis) | Minimizes turns |

### Example Calculation

For a **1-acre field** (~4,047 m² ≈ 63m × 63m):
- At 15m altitude, a 720p camera with ~70° FOV covers ~18m width per pass
- With 30% side overlap: effective strip width = 12.6m
- Number of passes: 63m ÷ 12.6m ≈ **5 passes**
- Total flight distance: ~5 × 63m + turns ≈ **~400m**
- At 2 m/s: flight time ≈ **~3.5 minutes** (well within the 12–15 min battery)

### What's Left / Room for Improvement
- **Obstacle avoidance**: The drone has no obstacle sensors. We should at minimum check for known obstacles (trees, power lines) from satellite imagery and add avoidance waypoints
- **No-fly zones**: Integration with DGCA (India's drone authority) airspace data to prevent flights in restricted zones
- **Wind compensation**: The algorithm doesn't account for wind. In Chennai, sea breeze can be significant. Consider orienting scan lines perpendicular to prevailing wind direction
- **Multi-battery mission planning**: For fields larger than ~1 acre, auto-split the mission into segments that fit within one battery charge, with clear "land, swap battery, resume from waypoint N" UX
- **Terrain elevation**: Flat assumption works for Chennai's coastal plains, but if we expand to hilly areas, altitude needs to be adjusted per waypoint using terrain data (e.g., from SRTM elevation data)

---

## 7. AI Analysis Pipeline

### Two-Pass Workflow

This is the core innovation of the project:

```
┌────────────────────────────────────────────────────────────────────┐
│                        PASS 1: SURVEY SCAN                        │
│                                                                    │
│  Drone flies lawnmower path → captures video                       │
│  Each frame is GPS-stamped                                         │
│                                                                    │
│  On-device TFLite model runs at ~30 FPS                            │
│  Classifies each frame:                                            │
│    ✅ Healthy crop    → skip                                       │
│    ⚠️ Anomaly flagged → save frame + GPS coordinate                │
│                                                                    │
│  Result: List of flagged GPS coordinates + thumbnail images        │
└────────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────────┐
│                    POST-FLIGHT CLOUD ANALYSIS                      │
│                                                                    │
│  Flagged frames sent to Cloud Vision API (Gemini / Groq)           │
│  Detailed analysis per frame:                                      │
│    • Disease identification (name + severity)                      │
│    • Pest classification                                           │
│    • Water stress assessment                                       │
│    • Weed detection                                                │
│    • Treatment recommendation                                      │
│                                                                    │
│  Result: Detailed diagnosis for each flagged location              │
└────────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────────┐
│                       PASS 2: TARGETED INSPECTION                  │
│                                                                    │
│  Drone revisits ONLY the flagged GPS coordinates                   │
│  Descends to lower altitude (~3–5m) for close-up images            │
│  Captures multiple angles of the problem area                      │
│                                                                    │
│  Close-ups sent to Cloud API for final, high-confidence diagnosis  │
│                                                                    │
│  Result: Confirmed diagnosis with photographic evidence            │
└────────────────────────────────────────────────────────────────────┘
```

### AI Providers — Full Comparison

#### Cloud Vision APIs (for post-flight analysis)

| Provider | Model | Free Tier | Rate Limit | Quality | Latency | Our Use |
|---|---|---|---|---|---|---|
| **Gemini 2.5 Flash** | Google | ✅ 1M tokens/day | 10 RPM | ⭐⭐⭐⭐ | ~1-2s | **Primary API** |
| **Gemini 2.0 Flash** | Google | ✅ 1M tokens/day | 15 RPM | ⭐⭐⭐ | ~1s | Faster alternative |
| **Groq** | Llama 4 Maverick | ✅ No card needed | ~30 RPM | ⭐⭐⭐ | ~0.2-0.5s ⚡ | **Speed fallback** |
| **NVIDIA NIM** | Various | ✅ Free prototyping | ~40 RPM | ⭐⭐⭐⭐ | ~1-2s | Custom model hosting |
| **OpenRouter** | Multiple free | ✅ Subsidized | ~20 RPM | Varies | Varies | Multi-model fallback |
| **Gemini 2.5 Pro** | Google | 5 RPM free | 5 RPM | ⭐⭐⭐⭐⭐ | ~2-4s | Complex/ambiguous cases |

#### Specialized Agriculture APIs

| Provider | Strength | Free Tier | Notes |
|---|---|---|---|
| **Agrio** | Disease diagnosis + treatment plans | Freemium | Most comprehensive ag-specific API |
| **Kindwise / Crop.health** | Large disease database | Freemium | Good for pest identification |
| **PlantNet** | Plant species identification | Free | Scientific backing, community-driven |
| **TensorFlow CropNet** | Pre-trained crop disease model | Free (open-source) | Can run on-device |

#### On-Device Models (for Pass 1 real-time flagging)

| Model | Size | Speed | Accuracy | Notes |
|---|---|---|---|---|
| **MobileNet V3 (TFLite)** | ~5-15 MB | ~30-50ms | ⭐⭐ | Lightest, fastest — good for basic flagging |
| **EfficientNet-Lite (TFLite)** | ~15-30 MB | ~50-100ms | ⭐⭐⭐ | Better accuracy, still fast |
| **YOLOv8n (TFLite)** | ~6 MB | ~20-40ms | ⭐⭐⭐ | Best for object detection (animals, pests) |

### Estimated Cost Per Flight

| Scenario | API Calls | Cost |
|---|---|---|
| Typical flight (~100 flagged frames) | Gemini Flash free tier | **$0.00** |
| Large field (~300 flagged frames) | Gemini Flash free tier | **$0.00** (within daily limit) |
| Very heavy usage (1000+ frames/day) | Exceeds free tier | **~$0.03–0.10** |

### What's Left / Room for Improvement
- **TFLite model selection and training**: We need to actually fine-tune a MobileNet/EfficientNet model on crop disease data (see Section 8 below). The model quality directly determines how many false positives/negatives Pass 1 generates
- **Prompt engineering for Cloud APIs**: The quality of Gemini's crop analysis depends heavily on the prompt. We need to craft and test prompts that produce structured, actionable output (disease name, confidence, severity 1-5, treatment steps, urgency level)
- **Structured output schema**: Define a JSON schema for AI responses so the dashboard can parse and display results consistently
- **Confidence thresholds**: What TFLite confidence score triggers a "flag"? Too low = too many false positives (wasted drone time on Pass 2). Too high = missed diseases. Needs field-testing to calibrate
- **API failover chain**: If Gemini is rate-limited → try Groq → try OpenRouter → queue for later. Need to implement this gracefully
- **Offline AI**: If there's no internet in the field, the on-device model is the only option. Consider a more capable on-device model (EfficientNet-Lite) for offline-only scenarios
- **Training data from Chennai**: Public datasets (PlantVillage) are lab photos. Real drone images from Chennai fields will look very different (angle, lighting, resolution, crop varieties). We should collect and label our own images to improve accuracy

---

## 8. Model Fine-Tuning Roadmap

This is especially relevant for the **hackathon presentation** — a fine-tuned model trained on agricultural data is a strong differentiator.

### Strategy A: On-Device Model (Pass 1) — Transfer Learning

**Goal:** A lightweight TFLite model that runs on any Android phone at 30+ FPS

```
PlantVillage Dataset (54,309 images, 38 classes)
    + Rice Leaf Disease Dataset (rice-specific)
    + Custom Chennai field images (if available)
              ↓
    MobileNet V3 (pre-trained on ImageNet)
              ↓
    Freeze base layers, retrain classifier head
              ↓
    Export to TFLite (~10 MB)
              ↓
    Deploy in Android app
```

| Aspect | Details |
|---|---|
| **Platform** | Google Colab (free T4 GPU) |
| **Framework** | TensorFlow / Keras |
| **Base Model** | MobileNet V3 or EfficientNet-Lite |
| **Training Time** | ~10–30 minutes |
| **Cost** | **$0** |
| **Output** | `.tflite` file, 5–30 MB |

### Strategy B: Cloud Model (Pass 2) — LoRA Fine-Tuning with Unsloth

**Goal:** A fine-tuned vision-language model that gives detailed crop diagnosis with natural language explanations

```
PlantVillage + PlantDoc + Custom Images
    → Format as instruction dataset (image + question + expected answer)
              ↓
    Llama 3.2 Vision 11B (loaded in 4-bit via QLoRA)
              ↓
    LoRA adapters on vision encoder + language layers
    Fine-tuned using Unsloth framework
              ↓
    Export adapter weights (~50–200 MB)
              ↓
    Deploy via Groq / NVIDIA NIM / Hugging Face Inference
```

| Aspect | Details |
|---|---|
| **Platform** | Google Colab (free T4 GPU) |
| **Framework** | Unsloth (70% less VRAM, 2x faster than vanilla) |
| **Base Model** | Llama 3.2 Vision, Qwen2-VL, or Gemma 3 Vision |
| **Technique** | QLoRA (4-bit quantization + LoRA) |
| **Training Time** | ~30–60 minutes |
| **Cost** | **$0** |
| **Output** | LoRA adapter weights, deployable on free-tier APIs |

### Strategy C: Gemini Fine-Tuning (Alternative)

| Aspect | Details |
|---|---|
| **Platform** | Google AI Studio (free) |
| **Data Format** | JSONL (image URL + instruction + expected output) |
| **Cost** | Free in AI Studio |
| **Pros** | Zero infrastructure, Google manages everything |
| **Cons** | Model stays in Google's cloud — can't export or run locally |

### Available Training Datasets

| Dataset | Size | Classes | Type | Link |
|---|---|---|---|---|
| **PlantVillage** | 54,309 images | 38 disease classes | Lab photos | [Kaggle](https://www.kaggle.com/datasets/abdallahalidev/plantvillage-dataset) |
| **PlantDoc** | 2,569 images | 27 classes | Real-world photos | [GitHub](https://github.com/pratikkayal/PlantDoc-Dataset) |
| **Rice Leaf Disease** | 5,932 images | 4 classes | Rice-specific | [Kaggle](https://www.kaggle.com/datasets/vbookshelf/rice-leaf-diseases) |
| **AI4Boundaries** | Satellite parcels | Field boundaries | EU agricultural parcels | [EU JRC](https://data.jrc.ec.europa.eu/) |
| **Custom (ours)** | TBD | TBD | Drone footage from Chennai | To be collected |

### What's Left / Room for Improvement
- **Data collection plan**: We need a systematic way to collect and label images from actual Chennai fields with our drone. This is the single biggest factor in real-world accuracy
- **Class taxonomy**: Define exactly what classes our model should detect. Proposed initial set:
  - Healthy crop
  - Leaf blight / fungal infection
  - Pest damage (insect holes, bite marks)
  - Water stress (wilting, yellowing)
  - Weed patches
  - Animal/intruder
  - Bare soil / missing plants
- **Evaluation metrics**: How do we measure model quality? Precision, recall, F1-score per class. What's our minimum acceptable performance?
- **Model versioning**: As we collect more data and retrain, we need a system to version models and push updates to the app
- **Edge cases**: Shadows, partial cloud cover over the field, wet leaves after rain, mixed cropping — these all confuse vision models and need specific training data

---

## 9. Android App Technical Details

### Tech Stack

| Layer | Technology | Reasoning |
|---|---|---|
| **Language** | Kotlin | Modern, null-safe, Google's recommended language for Android |
| **UI Framework** | Jetpack Compose | Declarative UI, less boilerplate, modern animations |
| **Maps** | Google Maps SDK + ESRI/Mapbox tiles | Best documentation + multiple tile sources |
| **Local DB** | Room (SQLite wrapper) | Store fields, scan history, AI results |
| **Networking** | Retrofit + OkHttp | API calls to Gemini, Groq, etc. |
| **AI On-Device** | TensorFlow Lite | Run MobileNet model on-device |
| **DI** | Hilt (Dagger) | Dependency injection |
| **Architecture** | MVVM + Clean Architecture | Testable, maintainable |
| **Build** | Gradle (Kotlin DSL) | Standard Android build system |

### Key Screens (Planned)

| Screen | Purpose |
|---|---|
| **Home / Field List** | List of saved fields with last scan date and health status |
| **Map View** | Satellite view with boundary overlay, scan path preview |
| **Boundary Editor** | Draw/adjust field boundary on satellite imagery |
| **Mission Control** | Upload mission to drone, monitor flight status, live telemetry |
| **Scan Results** | Post-flight dashboard with health heatmap and flagged areas |
| **Issue Detail** | Close-up images of flagged area + AI diagnosis + treatment advice |
| **Settings** | Drone connection, API keys, scan parameters, language |

### What's Left / Room for Improvement
- **UI/UX design**: No mockups or wireframes have been created yet. We should design these before writing code
- **Localization**: Target farmers speak Tamil, Hindi, or English. The app needs multi-language support — especially for AI-generated treatment advice
- **Accessibility**: Large fonts, high contrast, simple navigation — many farmers may not be tech-savvy
- **Offline mode**: Rural areas near Chennai often have poor connectivity. The app must work offline for: map tiles (cached), boundary editing, mission upload (over local Wi-Fi to drone), on-device AI. Only cloud AI analysis requires internet
- **Drone connection wizard**: A step-by-step guide to help the farmer connect their phone to the drone's Wi-Fi
- **"Place drone here" guidance**: The app shows a marker on the map where the drone should be placed for takeoff. This needs clear visual guidance (AR overlay? Simple compass direction + distance?)

---

## 10. Regulatory & Safety Considerations

### DGCA (India) Drone Regulations

| Rule | Our Status |
|---|---|
| Drones < 250g: No registration needed | ⚠️ **Need to weigh our drone + ESP32 payload** |
| Drones 250g–2kg: "Micro" category, needs registration on DigiSky | Likely applies to us |
| Max altitude: 120m AGL | ✅ We fly at 10–15m |
| VLOS (Visual Line of Sight) required | ✅ Field scanning is within VLOS |
| No-fly zones: Airports, military, govt buildings | Need to integrate no-fly zone data |
| Daylight only | ✅ Agricultural scanning is daytime activity |

### Safety Features to Implement

| Feature | Priority | Status |
|---|---|---|
| Return-to-home on low battery | Critical | Planned in ESP32 firmware |
| Return-to-home on signal loss | Critical | Planned in ESP32 firmware |
| Geofencing (don't fly beyond boundary + margin) | High | Planned |
| Max altitude limit | High | Planned |
| Emergency land button in app | Critical | Planned |
| Pre-flight checklist in app | Medium | Not started |

### What's Left / Room for Improvement
- **DigiSky registration**: If the drone + payload exceeds 250g, we need to register on [DigiSky](https://digitalsky.dgca.gov.in/). Research the exact process
- **Insurance**: Commercial drone operations may require third-party liability insurance
- **Privacy**: The drone's camera captures images that may include neighboring properties. Consider privacy masking for areas outside the field boundary
- **Collision avoidance**: Our drone has zero obstacle sensors. This is a significant safety risk. For the hackathon demo, we should fly in open fields only. For production, ultrasonic or ToF sensors could be added

---

## 11. Project Timeline & Milestones

### Suggested Phasing

| Phase | Scope | Est. Duration |
|---|---|---|
| **Phase 1: App MVP** | Map display + manual boundary drawing + flight path generation + simulated drone | 2–3 weeks |
| **Phase 2: AI Integration** | Fine-tune TFLite model + integrate Gemini API + results dashboard | 1–2 weeks |
| **Phase 3: Drone Hardware** | ESP32 + GPS integration, firmware development, bench testing | 2–3 weeks |
| **Phase 4: Integration** | Connect app to real drone, field testing in Chennai | 1–2 weeks |
| **Phase 5: Polish** | UX refinement, localization, safety features, hackathon prep | 1 week |

> [!WARNING]
> **Hackathon timeline is unknown.** The above is an ideal schedule. If the hackathon is soon, we should prioritize the AI fine-tuning demo + app UI with simulated drone data.

---

## 12. Summary of All Open Items

| # | Item | Owner | Priority | Section |
|---|---|---|---|---|
| 1 | Identify drone's smartphone app name & protocol | Abhishek | 🔴 High | §3 |
| 2 | Weigh drone + ESP32 payload (DGCA < 250g?) | Abhishek | 🔴 High | §3, §10 |
| 3 | Design UI mockups / wireframes | Team | 🔴 High | §9 |
| 4 | Select & train TFLite on-device model | Team | 🔴 High | §7, §8 |
| 5 | Fine-tune cloud vision model (Unsloth + LoRA) | Team | 🔴 High | §8 |
| 6 | Define AI output JSON schema | Team | 🟡 Medium | §7 |
| 7 | Craft & test Gemini prompts for crop analysis | Team | 🟡 Medium | §7 |
| 8 | Select boundary detection algorithm (OpenCV vs SAM vs U-Net) | Team | 🟡 Medium | §5 |
| 9 | Set up Android project (Kotlin + Compose) | Team | 🟡 Medium | §9 |
| 10 | Decide on backend / no-backend architecture | Team | 🟡 Medium | §2 |
| 11 | Offline tile caching strategy | Team | 🟡 Medium | §4 |
| 12 | Collect training images from Chennai fields | Team | 🟡 Medium | §8 |
| 13 | ESP32 hardware procurement & integration | Abhishek | 🟡 Medium | §3 |
| 14 | Multi-language support (Tamil/Hindi/English) | Team | 🟢 Low | §9 |
| 15 | DigiSky drone registration | Abhishek | 🟢 Low | §10 |
| 16 | Multi-battery mission resume feature | Team | 🟢 Low | §6 |
| 17 | Terrain elevation data integration | Team | 🟢 Low | §6 |

---

> [!NOTE]
> **For teammates joining the project:** Start by reading this document end-to-end. Then pick an open item from the table above and let the team know what you're working on. The research document ([drone_crop_scanner_research.md](file:///home/abhishek/.gemini/antigravity/brain/1165f862-e2f2-422c-93fe-dc1ad94729fd/drone_crop_scanner_research.md)) has additional technical details and API comparisons.
