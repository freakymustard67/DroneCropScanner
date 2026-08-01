package com.example.cropdrone.hardware

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.cropdrone.domain.model.ConnectionState
import com.example.cropdrone.domain.model.TelemetryData
import com.example.cropdrone.domain.model.Waypoint
import com.example.cropdrone.domain.model.WaypointAction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Mock drone controller — simulates ESP32 drone telemetry + video for development.
 *
 * Behavior:
 *   - Connection succeeds 900ms after connect()
 *   - Telemetry updates at 5 Hz with battery drain, GPS trajectory along waypoints
 *   - Video frames synthesized at 4 Hz on a 320x240 canvas
 *   - Mission auto-completes when trajectory progress reaches 1.0
 */
class MockDroneController : DroneController {

    private val _connection = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val _telemetry = MutableStateFlow(
        TelemetryData(
            batteryPct = 100,
            altitudeM = 0.0,
            speedMs = 0.0,
            distanceToHomeM = 0.0,
            satellites = 18,
            lat = 28.61395,
            lng = 77.20902,
            headingDeg = 0.0,
            timestamp = System.currentTimeMillis(),
        )
    )
    private var uploadedWaypoints: List<Waypoint> = emptyList()
    private var missionActive = false
    private var missionJob: kotlinx.coroutines.Job? = null
    private var videoJob: kotlinx.coroutines.Job? = null

    override fun connect(ipAddress: String): Flow<ConnectionState> = flow {
        emit(ConnectionState.CONNECTING)
        delay(900)
        _connection.value = ConnectionState.CONNECTED
        emit(ConnectionState.CONNECTED)
    }

    override suspend fun disconnect() {
        missionJob?.cancel()
        videoJob?.cancel()
        _connection.value = ConnectionState.DISCONNECTED
    }

    override suspend fun uploadMission(waypoints: List<Waypoint>): Result<Unit> {
        require(waypoints.size >= 4) { "Mission needs at least 4 waypoints" }
        uploadedWaypoints = waypoints
        return Result.success(Unit)
    }

    override suspend fun startMission(): Result<Unit> {
        if (uploadedWaypoints.isEmpty()) return Result.failure(IllegalStateException("No mission uploaded"))
        missionActive = true
        startTelemetrySim()
        startVideoSim()
        return Result.success(Unit)
    }

    override suspend fun returnToHome(): Result<Unit> {
        missionActive = false
        missionJob?.cancel()
        videoJob?.cancel()
        uploadedWaypoints.firstOrNull()?.let { home ->
            _telemetry.value = _telemetry.value.copy(
                lat = home.lat, lng = home.lng, altitudeM = 0.0, speedMs = 0.0,
                distanceToHomeM = 0.0, timestamp = System.currentTimeMillis(),
            )
        }
        return Result.success(Unit)
    }

    override suspend fun emergencyLand(): Result<Unit> {
        missionActive = false
        missionJob?.cancel()
        videoJob?.cancel()
        _telemetry.value = _telemetry.value.copy(
            altitudeM = 0.0, speedMs = 0.0, timestamp = System.currentTimeMillis(),
        )
        return Result.success(Unit)
    }

    override fun getTelemetry(): Flow<TelemetryData> = _telemetry.asStateFlow()

    override fun getVideoStream(): Flow<Bitmap> = flow {
        var frame = 0
        while (true) {
            emit(renderMockFrame(frame++, Random.nextDouble() < 0.12))
            delay(250) // 4 FPS
        }
    }

    // ---- Internal simulation ----

    private fun startTelemetrySim() {
        val wps = uploadedWaypoints
        if (wps.size < 2) return
        val totalDist = (1 until wps.size).sumOf { i ->
            haversine(wps[i - 1].lat, wps[i - 1].lng, wps[i].lat, wps[i].lng)
        }
        val speed = 4.0 // m/s
        val durationMs = (totalDist / speed * 1000).toLong()
        val startTime = System.currentTimeMillis()

        missionJob = CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            while (isActive && missionActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toDouble() / durationMs).coerceIn(0.0, 1.0)
                val segFloat = progress * (wps.size - 1)
                val idx = minOf(wps.size - 2, segFloat.toInt())
                val t = segFloat - idx
                val a = wps[idx]
                val b = wps[idx + 1]
                val lat = a.lat + (b.lat - a.lat) * t
                val lng = a.lng + (b.lng - a.lng) * t
                val home = wps.first()
                val distToHome = haversine(lat, lng, home.lat, home.lng)
                val batteryDrop = 100 * (elapsed.toDouble() / durationMs) * 0.7
                _telemetry.value = TelemetryData(
                    batteryPct = maxOf(0, (100 - batteryDrop).toInt()),
                    altitudeM = if (progress >= 0.97) maxOf(0.0, 15.0 * (1 - (progress - 0.97) / 0.03)) else 15.0,
                    speedMs = if (progress >= 0.97) 0.0 else speed,
                    distanceToHomeM = distToHome,
                    satellites = 18 + Random.nextInt(3),
                    lat = lat, lng = lng,
                    headingDeg = ((idx * 37) % 360).toDouble(),
                    timestamp = System.currentTimeMillis(),
                )
                if (progress >= 1.0) {
                    missionActive = false
                    break
                }
                delay(200) // 5 Hz
            }
        }
    }

    private fun startVideoSim() {
        videoJob = CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            // Video frames are emitted via getVideoStream() flow; nothing to do here.
        }
    }

    private fun renderMockFrame(seed: Int, isAnomaly: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        // Green field gradient
        val grad = android.graphics.LinearGradient(0f, 0f, 0f, 240f, 0xFF7CB342.toInt(), 0xFF558B2F.toInt(), android.graphics.Shader.TileMode.CLAMP)
        val paint = Paint().apply { shader = grad }
        canvas.drawRect(0f, 0f, 320f, 240f, paint)
        // Rows
        paint.shader = null
        paint.color = 0x882E7D32.toInt(); paint.strokeWidth = 2f
        for (y in 10..230 step 18) {
            canvas.drawLine(0f, y.toFloat(), 320f, (y + sin(seed * 0.3 + y) * 4).toFloat(), paint)
        }
        // Anomaly spots
        if (isAnomaly) {
            for (i in 0 until 4) {
                paint.color = if (i % 2 == 0) 0xFFFFEB3B.toInt() else 0xFF795548.toInt()
                canvas.drawCircle(
                    (40 + (seed * 7 + i * 70) % 240).toFloat(),
                    (40 + (seed * 13 + i * 50) % 160).toFloat(),
                    (8 + (i % 3) * 3).toFloat(),
                    paint,
                )
            }
        }
        // HUD crosshair
        paint.color = 0x99FFFFFF.toInt(); paint.strokeWidth = 1f
        canvas.drawLine(160f, 100f, 160f, 140f, paint)
        canvas.drawLine(140f, 120f, 180f, 120f, paint)
        paint.color = Color.WHITE; paint.textSize = 10f; paint.isFakeBoldText = true
        canvas.drawText("FRAME ${"%05d".format(seed)}  ALT 15m", 8f, 16f, paint)
        return bmp
    }

    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).let { it * it }
        return 2 * 6378137.0 * kotlin.math.asin(minOf(1.0, sqrt(a)))
    }
}

