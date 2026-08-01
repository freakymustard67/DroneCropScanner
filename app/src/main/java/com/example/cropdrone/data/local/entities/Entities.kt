package com.example.cropdrone.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.cropdrone.domain.model.LatLng
import com.example.cropdrone.domain.model.Waypoint
import com.example.cropdrone.domain.model.WaypointAction
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "fields")
data class FieldEntity(
    @PrimaryKey val id: String,
    val name: String,
    val polygonJson: String,   // JSON-serialized List<LatLng>
    val areaHectares: Double,
    val createdAt: Long,
)

@Entity(tableName = "missions")
data class ScanMissionEntity(
    @PrimaryKey val id: String,
    val fieldName: String,
    val startedAt: Long,
    val endedAt: Long?,
    val waypointsJson: String,  // JSON-serialized List<Waypoint>
    val status: String,         // MissionStatus.name
    val totalPhotos: Int,
    val flaggedPhotos: Int,
    val analyzedPhotos: Int,
)

@Entity(tableName = "anomalies")
data class AnomalyEntity(
    @PrimaryKey val id: String,
    val missionId: String?,
    val lat: Double,
    val lng: Double,
    val imagePath: String,
    val thumbnailPath: String,
    val pass1Confidence: Double,
    val pass1Label: String,
    val diseaseName: String?,
    val severity: Int?,
    val confidence: Double?,
    val treatment: String?,
    val detectedAt: Long,
    val analyzedAt: Long?,
)

// ---- Type converters for complex fields ----
class Converters {
    @TypeConverter fun polygonToJson(pts: List<LatLng>): String {
        val arr = JSONArray()
        for (p in pts) arr.put(JSONObject().put("lat", p.lat).put("lng", p.lng))
        return arr.toString()
    }
    @TypeConverter fun jsonToPolygon(s: String): List<LatLng> {
        val arr = JSONArray(s)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i); LatLng(o.getDouble("lat"), o.getDouble("lng"))
        }
    }

    @TypeConverter fun waypointsToJson(wps: List<Waypoint>): String {
        val arr = JSONArray()
        for (w in wps) arr.put(JSONObject()
            .put("lat", w.lat).put("lng", w.lng).put("alt", w.altitude)
            .put("idx", w.index).put("action", w.action.name))
        return arr.toString()
    }
    @TypeConverter fun jsonToWaypoints(s: String): List<Waypoint> {
        val arr = JSONArray(s)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Waypoint(o.getDouble("lat"), o.getDouble("lng"), o.getDouble("alt"),
                o.getInt("idx"), WaypointAction.valueOf(o.getString("action")))
        }
    }
}
