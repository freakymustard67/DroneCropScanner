package com.example.cropdrone.data.mapper

import com.example.cropdrone.data.local.entities.AnomalyEntity
import com.example.cropdrone.data.local.entities.FieldEntity
import com.example.cropdrone.data.local.entities.ScanMissionEntity
import com.example.cropdrone.domain.model.Anomaly
import com.example.cropdrone.domain.model.FieldBoundary
import com.example.cropdrone.domain.model.LatLng
import com.example.cropdrone.domain.model.MissionStatus
import com.example.cropdrone.domain.model.ScanMission
import com.example.cropdrone.domain.model.Waypoint
import com.example.cropdrone.domain.model.WaypointAction
import org.json.JSONArray
import org.json.JSONObject

fun FieldEntity.toDomain(): FieldBoundary = FieldBoundary(
    id = id,
    name = name,
    polygon = polygonFromJson(polygonJson),
    areaHectares = areaHectares,
    createdAt = createdAt,
)

fun FieldBoundary.toEntity(): FieldEntity = FieldEntity(
    id = id,
    name = name,
    polygonJson = polygonToJson(polygon),
    areaHectares = areaHectares,
    createdAt = createdAt,
)

fun ScanMissionEntity.toDomain(): ScanMission = ScanMission(
    id = id,
    fieldName = fieldName,
    startedAt = startedAt,
    endedAt = endedAt,
    waypoints = waypointsFromJson(waypointsJson),
    status = MissionStatus.valueOf(status),
    totalPhotos = totalPhotos,
    flaggedPhotos = flaggedPhotos,
    analyzedPhotos = analyzedPhotos,
)

fun ScanMission.toEntity(): ScanMissionEntity = ScanMissionEntity(
    id = id,
    fieldName = fieldName,
    startedAt = startedAt,
    endedAt = endedAt,
    waypointsJson = waypointsToJson(waypoints),
    status = status.name,
    totalPhotos = totalPhotos,
    flaggedPhotos = flaggedPhotos,
    analyzedPhotos = analyzedPhotos,
)

fun AnomalyEntity.toDomain(): Anomaly = Anomaly(
    id = id, lat = lat, lng = lng, imagePath = imagePath, thumbnailPath = thumbnailPath,
    pass1Confidence = pass1Confidence, pass1Label = pass1Label,
    diseaseName = diseaseName, severity = severity, confidence = confidence, treatment = treatment,
    detectedAt = detectedAt, analyzedAt = analyzedAt,
)

fun Anomaly.toEntity(missionId: String? = null): AnomalyEntity = AnomalyEntity(
    id = id, missionId = missionId, lat = lat, lng = lng,
    imagePath = imagePath, thumbnailPath = thumbnailPath,
    pass1Confidence = pass1Confidence, pass1Label = pass1Label,
    diseaseName = diseaseName, severity = severity, confidence = confidence, treatment = treatment,
    detectedAt = detectedAt, analyzedAt = analyzedAt,
)

// ---- JSON helpers (avoid TypeConverters for repository layer) ----

private fun polygonToJson(pts: List<LatLng>): String {
    val arr = JSONArray()
    for (p in pts) arr.put(JSONObject().put("lat", p.lat).put("lng", p.lng))
    return arr.toString()
}

private fun polygonFromJson(s: String): List<LatLng> {
    val arr = JSONArray(s)
    return (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i); LatLng(o.getDouble("lat"), o.getDouble("lng"))
    }
}

private fun waypointsToJson(wps: List<Waypoint>): String {
    val arr = JSONArray()
    for (w in wps) arr.put(JSONObject()
        .put("lat", w.lat).put("lng", w.lng).put("alt", w.altitude)
        .put("idx", w.index).put("action", w.action.name))
    return arr.toString()
}

private fun waypointsFromJson(s: String): List<Waypoint> {
    val arr = JSONArray(s)
    return (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Waypoint(o.getDouble("lat"), o.getDouble("lng"), o.getDouble("alt"),
            o.getInt("idx"), WaypointAction.valueOf(o.getString("action")))
    }
}
