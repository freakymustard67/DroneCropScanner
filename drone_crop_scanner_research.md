# 🛩️ Drone Crop Scanner — Full Research & Design Document

## Project Summary

An Android app that connects to a drone, fetches GPS + high-res satellite imagery, automatically detects field boundaries, generates a lawnmower scan path, and sends the drone to systematically scan the entire field. AI analyzes the captured footage for crop diseases, pests, water stress, and anomalies — then sends the drone back to flagged areas for close-up inspection.

---

## Decisions Made So Far

| Decision | Choice |
|---|---|
| **Android framework** | Kotlin + Jetpack Compose |
| **Drone platform** | Custom budget quadcopter (720p dual cameras, Wi-Fi + 2.4GHz radio) |
| **Drone control architecture** | Pre-upload full mission → autonomous flight → post-flight analysis |
| **ESP32 + GPS** | Will be added to enable GPS waypoint navigation |
| **ESP32 role** | TBD — either pilot (intercept existing FC) or replace FC |
| **Boundary detection** | Auto-detect from satellite imagery + manual adjustment + GPS walk option |
| **Satellite imagery** | Multi-provider: highest resolution first (ESRI/Google), fresher imagery on demand |
| **Scan pattern** | Boustrophedon (lawnmower) |
| **AI strategy** | TFLite on-device (Pass 1 flagging) + Cloud API (Pass 2 detailed analysis) |
| **Two-pass workflow** | Pass 1: Full scan → Pass 2: Revisit flagged coordinates for close-ups |
| **Development approach** | Research-only for now, build later with simulated drone layer |

---

## 🗺️ Satellite Imagery Providers (Ranked by Resolution)

### Tier 1: Ultra High-Res (for boundary marking & map display)

| Provider | Resolution | Cost | Freshness | API Access |
|---|---|---|---|---|
| **Google Maps Static API** | ~0.3–0.6m | Free: 28,500 loads/mo | Months–years old | Maps SDK for Android |
| **ESRI World Imagery** | ~0.3–0.6m | Free tile server | Months–years old | ArcGIS REST tiles |
| **Mapbox Satellite** | ~0.5m | Free: 25k MAU | Months–years old | Mapbox SDK for Android |

### Tier 2: Fresh High-Res (if farmer says image is outdated)

| Provider | Resolution | Cost | Freshness | API Access |
|---|---|---|---|---|
| **Planet SkySat** | 0.5m | Paid (~$8/km²) | Days–weeks | Planet API |
| **Airbus Pléiades Neo** | 0.3m | Paid (expensive) | Days–weeks | OneAtlas API |
| **Maxar (WorldView)** | 0.3m | Paid (expensive) | Days–weeks | SecureWatch API |

### Tier 3: Free Moderate-Res (for NDVI/crop health overlay, NOT boundaries)

| Provider | Resolution | Cost | Freshness | API Access |
|---|---|---|---|---|
| **Sentinel-2** | 10m | Free | 5-day revisit | Sentinel Hub / Copernicus |
| **PlanetScope** | 3m | Free for research | Daily | Planet API |

> [!TIP]
> **Recommended Strategy:** Use ESRI/Google tiles as the default high-res base map. If the farmer flags an outdated image, offer a Sentinel-2 overlay for recent NDVI data. Planet SkySat is the best "fresh + high-res" commercial option for when accuracy is critical.

---

## 🤖 AI Vision Providers — Complete Comparison

### Cloud APIs (for Pass 2 detailed analysis)

| Provider | Model | Free Tier | Rate Limit | Vision Quality | Latency | Best For |
|---|---|---|---|---|---|---|
| **Google Gemini 2.5 Flash** | Gemini Flash | ✅ 1M tokens/day | 10 RPM | ⭐⭐⭐⭐ | ~1-2s | Primary API — best free balance |
| **Google Gemini 2.0 Flash** | Gemini 2.0 | ✅ 1M tokens/day | 15 RPM | ⭐⭐⭐ | ~1s | Faster, cheaper alternative |
| **Groq** | Llama 4 Maverick | ✅ No card needed | ~30 RPM | ⭐⭐⭐ | ~0.2-0.5s ⚡ | Ultra-fast fallback |
| **NVIDIA NIM** | Various | ✅ Free prototyping | ~40 RPM | ⭐⭐⭐⭐ | ~1-2s | NVIDIA-optimized models |
| **OpenRouter** | Multiple free models | ✅ Subsidized | ~20 RPM | Varies | Varies | Model variety/fallback |
| **Google Gemini 2.5 Pro** | Gemini Pro | Free: 5 RPM | 5 RPM | ⭐⭐⭐⭐⭐ | ~2-4s | Ambiguous/complex cases |

### Specialized Agriculture APIs

| Provider | Focus | Free Tier | Best For |
|---|---|---|---|
| **Agrio** | Plant disease, pests | Freemium | Professional crop diagnosis with treatment plans |
| **Kindwise / Crop.health** | Crop diseases | Freemium | Large disease database, pest identification |
| **PlantNet** | Plant identification | Free | Species identification, scientific backing |
| **TensorFlow CropNet** | Crop disease | Free (open-source) | Pre-trained crop disease models |

### On-Device AI (for Pass 1 real-time flagging)

| Approach | Model Size | Inference Speed | Accuracy | Offline? |
|---|---|---|---|---|
| **MobileNet V3 (TFLite)** | ~5-15 MB | ~30-50ms/frame | ⭐⭐ Basic flagging | ✅ Yes |
| **EfficientNet-Lite (TFLite)** | ~15-30 MB | ~50-100ms/frame | ⭐⭐⭐ Good detection | ✅ Yes |
| **YOLOv8n (TFLite)** | ~6 MB | ~20-40ms/frame | ⭐⭐⭐ Object detection | ✅ Yes |

---

## 🔧 Fine-Tuning Options for Hackathon

> [!IMPORTANT]
> Since this is for an **AI innovations hackathon**, fine-tuning a model on agricultural data would be a strong differentiator. Here are ALL your options:

### Option 1: LoRA Fine-Tuning with Unsloth (⭐ RECOMMENDED — Free)

| Aspect | Details |
|---|---|
| **Platform** | Google Colab (free T4 GPU) |
| **Framework** | Unsloth (70% less VRAM, 2x faster) |
| **Base Models** | Llama 3.2 Vision, Qwen2-VL, Gemma 3 Vision |
| **Technique** | QLoRA (4-bit quantization + LoRA adapters) |
| **Cost** | **$0** (runs on free Colab) |
| **Training Time** | ~30-60 min on Colab T4 |
| **Output** | LoRA adapter weights (~50-200 MB) |

**How it works:**
```
1. Load base vision model (e.g., Llama 3.2 Vision 11B) in 4-bit
2. Apply LoRA adapters to vision encoder + language layers
3. Train on PlantVillage + custom Chennai crop images
4. Export adapter weights to Hugging Face
5. Deploy via Groq/NVIDIA NIM or on-device
```

**Datasets available:**
- [PlantVillage](https://www.kaggle.com/datasets/abdallahalidev/plantvillage-dataset) — 54,309 images, 38 disease classes
- [PlantDoc](https://github.com/pratikkayal/PlantDoc-Dataset) — Real-world images (harder, more realistic)
- [Rice Leaf Disease](https://www.kaggle.com/datasets/vbookshelf/rice-leaf-diseases) — Specific to rice (relevant for Chennai)
- Custom: Collect from Chennai fields with your drone

---

### Option 2: Google Gemini Fine-Tuning (Free via AI Studio)

| Aspect | Details |
|---|---|
| **Platform** | Google AI Studio |
| **Base Model** | Gemini 1.5 Flash / Gemini 2.0 Flash |
| **Technique** | Supervised tuning (Google's infra) |
| **Cost** | **Free** in AI Studio (limited quota) |
| **Data Format** | JSONL (image + instruction + expected output) |
| **Output** | Tuned Gemini model accessible via API |

**Pros:** Zero infrastructure management, Google handles everything
**Cons:** Model stays in Google's cloud, can't export or run locally

---

### Option 3: TensorFlow / Keras Transfer Learning (Free — Best for TFLite On-Device)

| Aspect | Details |
|---|---|
| **Platform** | Google Colab or local machine |
| **Base Models** | MobileNet V3, EfficientNet, ResNet50 |
| **Technique** | Transfer learning (freeze base, train classifier head) |
| **Cost** | **$0** |
| **Training Time** | ~10-30 min on Colab |
| **Output** | `.tflite` model (5-30 MB) for Android deployment |

**Best for:** The on-device Pass 1 detection model. Fine-tune MobileNet V3 on PlantVillage → export to TFLite → runs on any Android phone at 30+ FPS.

---

### Option 4: NVIDIA NIM Custom Model Deployment (Free for Prototyping)

| Aspect | Details |
|---|---|
| **Platform** | NVIDIA build.nvidia.com |
| **Approach** | Fine-tune with Unsloth → deploy on NIM |
| **Cost** | Free for dev/testing, paid for production |
| **Rate Limit** | ~40 RPM |
| **Output** | API endpoint for your custom model |

---

### Option 5: Hugging Face AutoTrain (Low-Code — Free Tier)

| Aspect | Details |
|---|---|
| **Platform** | Hugging Face AutoTrain |
| **Approach** | Upload dataset → AutoTrain picks best architecture and hyperparameters |
| **Cost** | Free tier available (limited compute) |
| **Best For** | Quick prototyping without ML expertise |

---

### 🏆 Recommended Fine-Tuning Strategy for Hackathon

```
┌─────────────────────────────────────────────────────┐
│  ON-DEVICE MODEL (Pass 1 — Real-time Flagging)      │
│                                                     │
│  MobileNet V3 → Transfer Learning on PlantVillage   │
│  Platform: Google Colab (free)                      │
│  Output: .tflite (~10 MB)                           │
│  Runs at: 30+ FPS on any Android phone              │
│  Detects: Diseased/Healthy/Pest/Weed/Water-stress   │
└─────────────────────────────────────────────────────┘
                     ↓ Flagged frames
┌─────────────────────────────────────────────────────┐
│  CLOUD MODEL (Pass 2 — Detailed Diagnosis)          │
│                                                     │
│  Llama 3.2 Vision → LoRA fine-tune via Unsloth      │
│  Platform: Google Colab (free)                      │
│  Deploy: Groq (free, ultra-fast) or NVIDIA NIM      │
│  Provides: Disease name, severity, treatment plan   │
│  Fallback: Gemini Flash free tier (no fine-tuning)  │
└─────────────────────────────────────────────────────┘
```

---

## 📱 Full App Architecture (High-Level)

```
┌──────────────────────────────────────────────────────────────────┐
│                      ANDROID APP (Kotlin + Jetpack Compose)      │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────────┐  │
│  │  Map Module   │  │ Boundary     │  │  Flight Path Generator │  │
│  │              │  │ Detection    │  │                        │  │
│  │ • ESRI tiles │  │ • CV auto-   │  │ • Boustrophedon algo   │  │
│  │ • Google Maps│  │   detect     │  │ • Altitude/overlap     │  │
│  │ • Mapbox     │  │ • Manual     │  │ • Battery-aware splits │  │
│  │ • Sentinel   │  │   adjust     │  │ • Waypoint export      │  │
│  │   overlay    │  │ • GPS walk   │  │                        │  │
│  └──────────────┘  └──────────────┘  └────────────────────────┘  │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────────┐  │
│  │  Drone Comm   │  │  AI Engine   │  │  Dashboard & Reports   │  │
│  │  (Adapter)    │  │              │  │                        │  │
│  │              │  │ • TFLite     │  │ • Field health map     │  │
│  │ • Mock/Sim   │  │   on-device  │  │ • Disease heatmap      │  │
│  │ • ESP32 WiFi │  │ • Cloud APIs │  │ • Historical trends    │  │
│  │ • MAVLink    │  │   (Gemini,   │  │ • Treatment advice     │  │
│  │ • DJI SDK    │  │    Groq)     │  │ • Alert system         │  │
│  │  (pluggable) │  │ • Fine-tuned │  │                        │  │
│  └──────────────┘  └──────────────┘  └────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🔌 ESP32 + GPS Hardware (If Adding to Drone)

### Shopping List (~₹500–800 total)

| Component | Model | Price (approx) | Purpose |
|---|---|---|---|
| ESP32 Dev Board | ESP32-WROOM-32 | ~₹300 | Main brain — Wi-Fi + BLE + processing |
| GPS Module | NEO-6M or BN-880 | ~₹200–400 | Position tracking + waypoint navigation |
| Micro SD Module | SPI SD Card Adapter | ~₹50–100 | Store flight mission + log GPS data |
| Voltage Regulator | AMS1117 3.3V | ~₹20 | Power ESP32 from drone's 3.7V LiPo |

### ESP32 Software Architecture

```
ESP32 Firmware (Arduino/ESP-IDF)
├── WiFi AP Mode (phone connects to drone)
├── Mission Receiver (get waypoints from phone before takeoff)
├── GPS Reader (continuous NMEA parsing via UART)
├── Flight Controller Interface (PWM signals to FC)
├── Waypoint Navigator (PID-based heading/speed control)
├── Telemetry Broadcaster (send GPS + status back to phone)
└── Safety Module (return-to-home, low-battery, geofence)
```

---

## ⚠️ Open Questions (To Be Resolved)

1. **Drone app name / protocol** — Need to identify for reverse-engineering
2. **ESP32 integration approach** — Pilot vs. replace flight controller (depends on hardware inspection)
3. **Hackathon timeline** — When is the hackathon? This affects what's feasible to build
4. **Data collection** — Will you capture training images from actual Chennai fields?
5. **Offline capability** — Should the app work in rural areas with no internet?
6. **DGCA regulations** — India's drone regulations require registration for drones >250g. Is your drone under this weight?
7. **Multi-battery support** — With 12-15 min flight time, large fields need multiple flights. Should the app handle "resume scan from where we stopped"?

---

## 📋 Remaining Questions to Discuss

Before building, we still need to decide:
- Dashboard design and what data to show the farmer
- Language/localization (Tamil, Hindi, English?)
- Offline/low-connectivity mode
- How the "place drone here" positioning guidance works in the app
- Alert/notification system for detected issues
