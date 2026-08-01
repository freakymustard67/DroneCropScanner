package com.example.cropdrone.hardware.socket

import com.example.cropdrone.domain.model.Waypoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference

/**
 * Raw TCP socket client for ESP32 drone comms.
 *
 * Protocol (line-delimited JSON over TCP):
 *   Phone → Drone:
 *     {"cmd":"CONNECT"}
 *     {"cmd":"UPLOAD","waypoints":[{"lat":..,"lng":..,"alt":..,"action":"PHOTO"},...]}
 *     {"cmd":"START"}
 *     {"cmd":"RTH"}
 *     {"cmd":"LAND"}
 *   Drone → Phone (telemetry @ 5 Hz):
 *     {"type":"telemetry","lat":..,"lng":..,"alt":..,"bat":..,"sat":..,"spd":..,"hdg":..}
 *
 * Video frames are received over a separate UDP socket (port 8889) as JPEG byte arrays,
 * decoded to Bitmap by the caller.
 */
class DroneSocketClient(
    private val defaultPort: Int = 8888,
    private val videoPort: Int = 8889,
) {
    private val socketRef: AtomicReference<Socket?> = AtomicReference(null)
    private val writerRef: AtomicReference<PrintWriter?> = AtomicReference(null)
    private val readerRef: AtomicReference<BufferedReader?> = AtomicReference(null)

    suspend fun connect(ip: String, port: Int = defaultPort): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sock = Socket(ip, port)
            sock.tcpNoDelay = true
            socketRef.set(sock)
            writerRef.set(PrintWriter(sock.getOutputStream(), true))
            readerRef.set(BufferedReader(InputStreamReader(sock.getInputStream())))
            send(JSONObject().put("cmd", "CONNECT").toString())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try { writerRef.get()?.apply { send(JSONObject().put("cmd", "DISCONNECT").toString()) } } catch (_: Exception) {}
        writerRef.get()?.close()
        readerRef.get()?.close()
        socketRef.get()?.close()
        writerRef.set(null); readerRef.set(null); socketRef.set(null)
    }

    suspend fun uploadMission(waypoints: List<Waypoint>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val arr = JSONArray()
            for (wp in waypoints) {
                arr.put(JSONObject()
                    .put("lat", wp.lat)
                    .put("lng", wp.lng)
                    .put("alt", wp.altitude)
                    .put("action", wp.action.name)
                    .put("idx", wp.index))
            }
            send(JSONObject().put("cmd", "UPLOAD").put("waypoints", arr).toString())
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun startMission(): Result<Unit> = withContext(Dispatchers.IO) {
        try { send(JSONObject().put("cmd", "START").toString()); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun returnToHome(): Result<Unit> = withContext(Dispatchers.IO) {
        try { send(JSONObject().put("cmd", "RTH").toString()); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun emergencyLand(): Result<Unit> = withContext(Dispatchers.IO) {
        try { send(JSONObject().put("cmd", "LAND").toString()); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    /** Stream incoming JSON telemetry lines from the drone. */
    fun telemetryLines(): Flow<JSONObject> = flow {
        val reader = readerRef.get() ?: return@flow
        while (true) {
            val line = reader.readLine() ?: break
            try { emit(JSONObject(line)) } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    private fun send(msg: String) {
        writerRef.get()?.println(msg) ?: throw IllegalStateException("Not connected")
    }
}
