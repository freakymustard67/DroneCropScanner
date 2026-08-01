package com.example.cropdrone.domain.usecase

import com.example.cropdrone.domain.model.FlightPlanSettings
import com.example.cropdrone.domain.model.LatLng
import com.example.cropdrone.domain.model.SafetyCheck
import com.example.cropdrone.domain.model.Waypoint
import com.example.cropdrone.domain.model.WaypointAction
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_M = 6378137.0
private const val DEG = Math.PI / 180.0
private const val RAD = 180.0 / Math.PI

class GenerateFlightPathUseCase @Inject constructor() {

    /**
     * Generate a lawnmower (boustrophedon) flight path over the given polygon.
     *
     * Algorithm:
     *   1. Project polygon to planar coords (meters) around polygon centroid.
     *   2. Compute footprint width on the ground from altitude + camera FOV.
     *   3. Line spacing = footprint * (1 - sideOverlap).
     *   4. Sweep across the bounding box along the Y axis, stepping in X.
     *   5. Clip each sweep line to the polygon (sampling + inset 8m from edge).
     *   6. Connect sweeps alternating directions.
     *   7. Add Takeoff (at centroid) + RTH + Land waypoints.
     */
    operator fun invoke(
        polygon: List<LatLng>,
        settings: FlightPlanSettings,
    ): List<Waypoint> {
        if (polygon.size < 3) return emptyList()

        // Normalize: ensure not double-closed
        val poly = if (polygon.first() == polygon.last()) polygon.dropLast(1) else polygon
        if (poly.size < 3) return emptyList()

        val ref = LatLng(
            lat = poly.map { it.lat }.average(),
            lng = poly.map { it.lng }.average(),
        )

        val planarPoly = poly.map { toPlanar(it, ref) }
        val bounds = planarBounds(planarPoly)

        // Ground footprint width
        val halfFovRad = (settings.cameraFovDeg / 2.0) * DEG
        val footprintWidthM = 2.0 * settings.altitude * kotlin.math.tan(halfFovRad)
        val lineSpacingM = max(1.0, footprintWidthM * (1.0 - settings.sideOverlap))

        val widthM = bounds.maxX - bounds.minX
        val numLines = max(1, kotlin.math.ceil(widthM / lineSpacingM).toInt())
        val startX = bounds.minX + (widthM - (numLines - 1) * lineSpacingM) / 2.0

        // Generate sweep segments
        data class Seg(val a: LatLng, val b: LatLng)
        val sweeps = mutableListOf<Seg>()
        val horizInset = 10.0
        for (i in 0 until numLines) {
            val x = startX + i * lineSpacingM + horizInset
            if (x > bounds.maxX - horizInset) break
            val top = fromPlanar(x, bounds.maxY, ref)
            val bottom = fromPlanar(x, bounds.minY, ref)
            val segs = clipSegmentToPolygon(top, bottom, poly, samples = 100, insetM = 8.0)
            var j = 0
            while (j + 1 < segs.size) {
                sweeps.add(Seg(segs[j], segs[j + 1]))
                j += 2
            }
        }
        if (sweeps.isEmpty()) return emptyList()

        // Build waypoint list
        val waypoints = mutableListOf<Waypoint>()
        var idx = 1

        // Takeoff at polygon centroid (always inside with max margin)
        val takeoffPoint = ref
        waypoints.add(Waypoint(takeoffPoint.lat, takeoffPoint.lng, 0.0, idx++, WaypointAction.TAKEOFF))

        // Navigate to first sweep start
        val firstStart = sweeps[0].a
        waypoints.add(Waypoint(firstStart.lat, firstStart.lng, settings.altitude, idx++, WaypointAction.WAYPOINT))

        for (i in sweeps.indices) {
            val sweep = sweeps[i]
            val a = if (i % 2 == 0) sweep.a else sweep.b
            val b = if (i % 2 == 0) sweep.b else sweep.a
            val last = waypoints.last()
            if (haversine(LatLng(last.lat, last.lng), a) > 0.5) {
                waypoints.add(Waypoint(a.lat, a.lng, settings.altitude, idx++, WaypointAction.WAYPOINT))
            }
            waypoints.add(Waypoint(b.lat, b.lng, settings.altitude, idx++, WaypointAction.PHOTO))
        }

        // RTH + Land at centroid
        waypoints.add(Waypoint(takeoffPoint.lat, takeoffPoint.lng, settings.altitude, idx++, WaypointAction.RTH))
        waypoints.add(Waypoint(takeoffPoint.lat, takeoffPoint.lng, 0.0, idx++, WaypointAction.LAND))

        return waypoints
    }

    // ---- Geometry helpers ----

    private fun toPlanar(p: LatLng, ref: LatLng): Pair<Double, Double> {
        val x = (p.lng - ref.lng) * DEG * cos(ref.lat * DEG) * EARTH_RADIUS_M
        val y = (p.lat - ref.lat) * DEG * EARTH_RADIUS_M
        return x to y
    }

    private fun fromPlanar(x: Double, y: Double, ref: LatLng): LatLng {
        val lat = ref.lat + (y / EARTH_RADIUS_M) * RAD
        val lng = ref.lng + (x / (EARTH_RADIUS_M * cos(ref.lat * DEG))) * RAD
        return LatLng(lat, lng)
    }

    private fun planarBounds(pts: List<Pair<Double, Double>>): Bounds {
        var minX = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        for ((x, y) in pts) {
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        return Bounds(minX, maxX, minY, maxY)
    }

    private data class Bounds(val minX: Double, val maxX: Double, val minY: Double, val maxY: Double)

    fun haversine(a: LatLng, b: LatLng): Double {
        val dLat = (b.lat - a.lat) * DEG
        val dLng = (b.lng - a.lng) * DEG
        val lat1 = a.lat * DEG
        val lat2 = b.lat * DEG
        val h = sin(dLat / 2).let { it * it } + cos(lat1) * cos(lat2) * sin(dLng / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * kotlin.math.asin(min(1.0, sqrt(h)))
    }

    private fun pointInPolygon(pt: LatLng, poly: List<LatLng>): Boolean {
        if (poly.size < 3) return false
        val ref = poly[0]
        val p = toPlanar(pt, ref)
        val pts = poly.map { toPlanar(it, ref) }
        var inside = false
        var j = pts.size - 1
        for (i in pts.indices) {
            val (xi, yi) = pts[i]
            val (xj, yj) = pts[j]
            val intersect = (yi > p.second) != (yj > p.second) &&
                p.first < ((xj - xi) * (p.second - yi)) / (yj - yi + 1e-12) + xi
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    private fun clipSegmentToPolygon(
        a: LatLng, b: LatLng, poly: List<LatLng>,
        samples: Int = 80, insetM: Double = 6.0,
    ): List<LatLng> {
        val insideFlags = BooleanArray(samples + 1) { i ->
            val t = i.toDouble() / samples
            val p = LatLng(a.lat + (b.lat - a.lat) * t, a.lng + (b.lng - a.lng) * t)
            pointInPolygon(p, poly)
        }
        val segLengthM = haversine(a, b)
        val insetFrac = min(0.4, insetM / max(1.0, segLengthM))

        val segments = mutableListOf<LatLng>()
        var inSegment = false
        var startIdx = 0
        for (i in insideFlags.indices) {
            if (insideFlags[i] && !inSegment) {
                startIdx = i
                inSegment = true
            } else if (!insideFlags[i] && inSegment) {
                val endIdx = i - 1
                val startT = startIdx.toDouble() / samples + insetFrac
                val endT = endIdx.toDouble() / samples - insetFrac
                if (endT > startT) {
                    segments.add(LatLng(a.lat + (b.lat - a.lat) * startT, a.lng + (b.lng - a.lng) * startT))
                    segments.add(LatLng(a.lat + (b.lat - a.lat) * endT, a.lng + (b.lng - a.lng) * endT))
                }
                inSegment = false
            }
        }
        if (inSegment) {
            val endIdx = samples
            val startT = startIdx.toDouble() / samples + insetFrac
            val endT = endIdx.toDouble() / samples - insetFrac
            if (endT > startT) {
                segments.add(LatLng(a.lat + (b.lat - a.lat) * startT, a.lng + (b.lng - a.lng) * startT))
                segments.add(LatLng(a.lat + (b.lat - a.lat) * endT, a.lng + (b.lng - a.lng) * endT))
            }
        }
        return segments
    }
}

class RunPreflightCheckUseCase @Inject constructor() {

    operator fun invoke(
        waypoints: List<Waypoint>,
        polygon: List<LatLng>,
        settings: FlightPlanSettings,
        batteryPct: Int,
    ): List<SafetyCheck> {
        val checks = mutableListOf<SafetyCheck>()
        val safetyMarginM = 5.0
        val gen = GenerateFlightPathUseCase()

        // 1. Geofence
        val geofenceOk = waypoints.all { wp ->
            when (wp.action) {
                WaypointAction.TAKEOFF, WaypointAction.LAND, WaypointAction.RTH -> true
                else -> distanceToPolygonEdge(LatLng(wp.lat, wp.lng), polygon) <= -safetyMarginM
            }
        }
        checks.add(SafetyCheck(
            ok = geofenceOk,
            message = if (geofenceOk) "All waypoints inside field + ${safetyMarginM.toInt()}m geofence"
            else "Some waypoints violate ${safetyMarginM.toInt()}m geofence",
        ))

        // 2. Battery estimate
        var totalDist = 0.0
        for (i in 1 until waypoints.size) {
            totalDist += gen.haversine(
                LatLng(waypoints[i - 1].lat, waypoints[i - 1].lng),
                LatLng(waypoints[i].lat, waypoints[i].lng),
            )
        }
        val estTimeMin = (totalDist / max(0.5, settings.speed)) / 60.0
        val estBatteryDrain = (estTimeMin / 0.6).toInt() // ~1% per 0.6 min
        val batteryOk = batteryPct >= estBatteryDrain + 5
        checks.add(SafetyCheck(
            ok = batteryOk,
            message = "Mission ~${"%.1f".format(estTimeMin)} min, needs ~$estBatteryDrain% + 5% reserve (have $batteryPct%)",
        ))

        // 3. Min waypoints
        val minWpOk = waypoints.size >= 4
        checks.add(SafetyCheck(
            ok = minWpOk,
            message = if (minWpOk) "${waypoints.size} waypoints in plan" else "Plan has too few waypoints",
        ))

        // 4. Polygon closed
        val polyClosed = polygon.size >= 4
        checks.add(SafetyCheck(
            ok = polyClosed,
            message = if (polyClosed) "Field boundary is closed" else "Field boundary not closed",
        ))

        return checks
    }

    private fun distanceToPolygonEdge(pt: LatLng, poly: List<LatLng>): Double {
        if (poly.size < 3) return Double.POSITIVE_INFINITY
        val ref = poly[0]
        val p = toPlanar(pt, ref)
        val pts = poly.map { toPlanar(it, ref) }
        var minDist = Double.POSITIVE_INFINITY
        var j = pts.size - 1
        for (i in pts.indices) {
            val (xi, yi) = pts[i]
            val (xj, yj) = pts[j]
            val dx = xj - xi
            val dy = yj - yi
            val t = max(0.0, min(1.0, ((p.first - xi) * dx + (p.second - yi) * dy) / (dx * dx + dy * dy + 1e-12)))
            val cx = xi + t * dx
            val cy = yi + t * dy
            val d = hypot(p.first - cx, p.second - cy)
            if (d < minDist) minDist = d
            j = i
        }
        val inside = pointInPolygon(pt, poly)
        return if (inside) -minDist else minDist
    }

    private fun toPlanar(p: LatLng, ref: LatLng): Pair<Double, Double> {
        val x = (p.lng - ref.lng) * DEG * cos(ref.lat * DEG) * EARTH_RADIUS_M
        val y = (p.lat - ref.lat) * DEG * EARTH_RADIUS_M
        return x to y
    }

    private fun pointInPolygon(pt: LatLng, poly: List<LatLng>): Boolean {
        if (poly.size < 3) return false
        val ref = poly[0]
        val p = toPlanar(pt, ref)
        val pts = poly.map { toPlanar(it, ref) }
        var inside = false
        var j = pts.size - 1
        for (i in pts.indices) {
            val (xi, yi) = pts[i]
            val (xj, yj) = pts[j]
            val intersect = (yi > p.second) != (yj > p.second) &&
                p.first < ((xj - xi) * (p.second - yi)) / (yj - yi + 1e-12) + xi
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}

class EstimateMissionDurationUseCase @Inject constructor(
    private val generator: GenerateFlightPathUseCase,
) {
    operator fun invoke(waypoints: List<Waypoint>, speedMs: Double): Double {
        if (waypoints.size < 2) return 0.0
        var dist = 0.0
        for (i in 1 until waypoints.size) {
            dist += generator.haversine(
                LatLng(waypoints[i - 1].lat, waypoints[i - 1].lng),
                LatLng(waypoints[i].lat, waypoints[i].lng),
            )
        }
        return dist / max(0.5, speedMs) // seconds
    }
}
