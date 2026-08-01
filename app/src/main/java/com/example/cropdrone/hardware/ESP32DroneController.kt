package com.example.cropdrone.hardware

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.cropdrone.domain.model.ConnectionState
import com.example.cropdrone.domain.model.TelemetryData
import com.example.cropdrone.domain.model.Waypoint
import com.example.cropdrone.hardware.socket.DroneSocketClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket

/**
 * Real ESP32 drone controller.
 *
 * - Uses TCP socket (DroneSocketClient) for command/control + telemetry stream.
 * - Uses UDP socket on port 8889 for video frame byte arrays (JPEG-encoded).
 * - Phone connects to drone's Wi-Fi AP (default 192.168.4.1).
 *
 * The phone has NO internet while connected to the drone, so all operations
 * are fully offline. Cloud Pass 2 calls are queued elsewhere (see WorkManager).
 */
class ESP32DroneController(
    private val socketClient: DroneSocketClient = DroneSocketClient(),
    private val videoPort: Int = 8889,
) : DroneController {

    private var videoSocket: DatagramSocket? = null

    override fun connect(ipAddress: String): Flow<ConnectionState> = flow {
        emit(ConnectionState.CONNECTING)
        val result = socketClient.connect(ipAddress)
        if (result.isSuccess) emit(ConnectionState.CONNECTED)
        else emit(ConnectionState.ERROR)
    }

    override suspend fun disconnect() {
        socketClient.disconnect()
        videoSocket?.close()
        videoSocket = null
    }

    override suspend fun uploadMission(waypoints: List<Waypoint>): Result<Unit> =
        socketClient.uploadMission(waypoints)

    override suspend fun startMission(): Result<Unit> =
        socketClient.startMission()

    override suspend fun returnToHome(): Result<Unit> =
        socketClient.returnToHome()

    override suspend fun emergencyLand(): Result<Unit> =
        socketClient.emergencyLand()

    override fun getTelemetry(): Flow<TelemetryData> =
        socketClient.telemetryLines()
            .filter { it.optString("type") == "telemetry" }
            .map { json ->
                TelemetryData(
                    batteryPct = json.optInt("bat", 0),
                    altitudeM = json.optDouble("alt", 0.0),
                    speedMs = json.optDouble("spd", 0.0),
                    distanceToHomeM = json.optDouble("dh", 0.0),
                    satellites = json.optInt("sat", 0),
                    lat = json.optDouble("lat", 0.0),
                    lng = json.optDouble("lng", 0.0),
                    headingDeg = json.optDouble("hdg", 0.0),
                    timestamp = System.currentTimeMillis(),
                )
            }

    override fun getVideoStream(): Flow<Bitmap> = flow {
        if (videoSocket == null) {
            videoSocket = DatagramSocket(videoPort).apply { soTimeout = 5000 }
        }
        val buf = ByteArray(65536)
        val packet = DatagramPacket(buf, buf.size)
        while (true) {
            try {
                videoSocket?.receive(packet)
                val data = packet.data.copyOfRange(0, packet.length)
                BitmapFactory.decodeByteArray(data, 0, data.size)?.let { emit(it) }
            } catch (_: java.net.SocketTimeoutException) {
                // skip
            }
        }
    }
}
