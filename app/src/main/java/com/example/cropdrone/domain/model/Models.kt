package com.example.cropdrone.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LatLng(
    val lat: Double,
    val lng: Double,
)

@Serializable
data class Waypoint(
    val lat: Double,
    val lng: Double,
    val altitude: Double,    // meters
    val index: Int,
    val action: WaypointAction,
)

@Serializable
enum class WaypointAction {
    TAKEOFF, WAYPOINT, PHOTO, RTH, LAND
}

@Serializable
data class FieldBoundary(
    val id: String,
    val name: String,
    val polygon: List<LatLng>,   // closed polygon (first === last)
    val areaHectares: Double,
    val createdAt: Long,
)

@Serializable
data class FlightPlanSettings(
    val altitude: Double = 15.0,        // meters
    val forwardOverlap: Double = 0.7,    // 0..1
    val sideOverlap: Double = 0.3,       // 0..1
    val cameraFovDeg: Double = 75.0,
    val speed: Double = 4.0,             // m/s
)

@Serializable
data class TelemetryData(
    val batteryPct: Int,
    val altitudeM: Double,
    val speedMs: Double,
    val distanceToHomeM: Double,
    val satellites: Int,
    val lat: Double,
    val lng: Double,
    val headingDeg: Double,
    val timestamp: Long,
)

@Serializable
data class Anomaly(
    val id: String,
    val lat: Double,
    val lng: Double,
    val imagePath: String,
    val thumbnailPath: String,
    val pass1Confidence: Double,    // 0..1
    val pass1Label: String,         // disease, pest, water_stress, healthy
    val diseaseName: String? = null,
    val severity: Int? = null,      // 1..5
    val confidence: Double? = null, // 0..1
    val treatment: String? = null,
    val detectedAt: Long,
    val analyzedAt: Long? = null,
)

@Serializable
data class ScanMission(
    val id: String,
    val fieldName: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val waypoints: List<Waypoint>,
    val status: MissionStatus,
    val totalPhotos: Int = 0,
    val flaggedPhotos: Int = 0,
    val analyzedPhotos: Int = 0,
)

enum class MissionStatus {
    IDLE, UPLOADING, READY, FLYING, RETURNING, LANDED, ABORTED
}

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

enum class BoundaryMode {
    MANUAL, AUTO, GPS_WALK
}

enum class AnomalySeverity(val label: String) {
    LOW("Low"), MODERATE("Moderate"), HIGH("High"), CRITICAL("Critical");

    companion object {
        fun fromScore(score: Int): AnomalySeverity = when {
            score <= 2 -> LOW
            score == 3 -> MODERATE
            score == 4 -> HIGH
            else -> CRITICAL
        }
    }
}

data class SafetyCheck(
    val ok: Boolean,
    val message: String,
)
