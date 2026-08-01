package com.example.cropdrone.core

/**
 * App-wide constants.
 */
object Constants {
    const val DEFAULT_DRONE_IP = "192.168.4.1"
    const val DEFAULT_DRONE_PORT = 8888
    const val DEFAULT_VIDEO_PORT = 8889

    const val TFLITE_MODEL_FILENAME = "crop_disease_mobilenet.tflite"

    // Pass 1 inference
    const val PASS1_THRESHOLD = 0.6f
    const val VIDEO_FRAME_RATE_FPS = 30
    const val TELEMETRY_RATE_HZ = 5

    // Safety
    const val GEOFENCE_SAFETY_MARGIN_M = 5.0
    const val BATTERY_SAFETY_RESERVE_PCT = 5
    const val AUTO_RTH_BATTERY_THRESHOLD = 20

    // Network
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    const val GEMINI_MODEL = "gemini-2.5-flash"

    // Sentinel-2 / Copernicus
    const val SENTINEL_TOKEN_URL = "https://services.sentinel-hub.com/oauth/token"
    const val SENTINEL_PROCESS_URL = "https://services.sentinel-hub.com/api/v1/process"

    // Notification channels
    const val NOTIF_CHANNEL_MISSION = "mission_status"
    const val NOTIF_CHANNEL_ANOMALY = "anomaly_detected"
    const val NOTIF_CHANNEL_CLOUD = "cloud_analysis"

    // WorkManager
    const val CLOUD_ANALYSIS_WORK_NAME = "cloud_analysis_work"
}
