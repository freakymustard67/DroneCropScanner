# Drone Crop Scanner 🛩️🌾

An Android application for drone-based agricultural crop monitoring and disease detection. Built with Kotlin and Jetpack Compose.

## Features

- **Drone Control** — Connect to ESP32-based drones via WebSocket
- **Flight Planning** — Generate optimized flight paths for field scanning
- **AI Disease Detection** — TensorFlow Lite on-device crop disease classifier
- **Gemini Integration** — Cloud-based analysis with Google Gemini API
- **Field Mapping** — Real-time map visualization of scanned areas
- **Dashboard** — Analytics and monitoring dashboard

## Tech Stack

- **Kotlin** + Jetpack Compose
- **Hilt** (Dependency Injection)
- **Room** (Local database)
- **TensorFlow Lite** (On-device ML)
- **Google Gemini API** (Cloud analysis)
- **WebSocket** (Drone communication)
- **Google Maps** (Field visualization)

## Architecture

Clean Architecture with three layers:
- **Data** — Database, API services, repositories
- **Domain** — Use cases, models, repository interfaces
- **Presentation** — ViewModels, Compose screens

## Setup

1. Open in Android Studio
2. Get a Gemini API key from Google AI Studio
3. Build and run on an Android device
