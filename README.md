# Crop Monitoring 🌾

Satellite-based crop monitoring and analysis using Sentinel Hub APIs and Sarvam AI.

## Features

- **Satellite Imagery** — Sentinel Hub integration for NDVI and multi-spectral analysis
- **AI Analysis** — Sarvam AI for farm-level diagnostics
- **Small Field Analysis** — Specialized algorithms for small/irregular fields
- **Drone Crop Scanner Research** — Companion research for drone-based scanning

## Tech Stack

- **Python** — Sentinel Hub API client (`sentinel_monitor.py`)
- **Sarvam AI** — AI-powered crop health analysis
- **Node.js** — Additional tooling

## Usage

```bash
# Run satellite monitoring
python sentinel_monitor.py
```

## Environment

Requires `.env` with:
- Sentinel Hub API credentials
- Sarvam AI API key
