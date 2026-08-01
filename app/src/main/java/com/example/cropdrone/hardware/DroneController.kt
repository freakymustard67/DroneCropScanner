package com.example.cropdrone.hardware

import android.graphics.Bitmap
import com.example.cropdrone.domain.model.ConnectionState
import com.example.cropdrone.domain.model.TelemetryData
import com.example.cropdrone.domain.model.Waypoint
import kotlinx.coroutines.flow.Flow

/**
 * Abstract drone controller.
 *
 * Two implementations:
 *   - [MockDroneController]: simulates telemetry + video for development without hardware.
 *   - [ESP32DroneController]: real Wi-Fi TCP/UDP comms to the ESP32 + GPS module.
 *
 * Critical: the phone is connected to the drone's Wi-Fi AP (192.168.4.1) and has NO internet.
 * All methods here must work fully offline. Cloud AI (Pass 2) is queued separately.
 */
interface DroneController {

    /** Stream of connection state changes. */
    fun connect(ipAddress: String): Flow<ConnectionState>

    /** Disconnect and release any underlying sockets. */
    suspend fun disconnect()

    /** Upload a pre-validated mission plan to the drone. */
    suspend fun uploadMission(waypoints: List<Waypoint>): Result<Unit>

    /** Begin autonomous execution of the uploaded mission. */
    suspend fun startMission(): Result<Unit>

    /** Command immediate Return-to-Home. */
    suspend fun returnToHome(): Result<Unit>

    /** Command immediate emergency landing at current position. */
    suspend fun emergencyLand(): Result<Unit>

    /** Live telemetry stream (~5 Hz). */
    fun getTelemetry(): Flow<TelemetryData>

    /** Live video frame stream (~30 FPS) as Bitmaps. */
    fun getVideoStream(): Flow<Bitmap>
}
