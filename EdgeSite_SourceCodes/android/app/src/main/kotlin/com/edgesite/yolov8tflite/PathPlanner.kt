package com.edgesite.yolov8tflite

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

data class UnsafeZone(
    val id: String = "",
    val center: LatLng,
    val radiusMeters: Double = 20.0
)

data class SafeZone(
    val id: String = "",
    val center: LatLng,
    val radiusMeters: Double = 15.0
)

data class PlannedPath(
    val points: List<LatLng>,
    val targetIndex: Int,           
    val isStraight: Boolean,        
    val waypointUsed: LatLng? = null
)


object PathPlanner {

    private const val TAG = "PathPlanner"

    fun computePaths(
        start: LatLng,
        targets: List<LatLng>,
        unsafeZones: List<UnsafeZone>,
        safeZones: List<SafeZone>
    ): List<PlannedPath> {
        return targets.mapIndexed { idx, target ->
            computeSinglePath(start, target, idx, unsafeZones, safeZones)
        }
    }

   
    private fun computeSinglePath(
        start: LatLng,
        target: LatLng,
        targetIndex: Int,
        unsafeZones: List<UnsafeZone>,
        safeZones: List<SafeZone>
    ): PlannedPath {
        val blockingZones = unsafeZones.filter { zone ->
            segmentIntersectsCircle(start, target, zone.center, zone.radiusMeters)
        }

        if (blockingZones.isEmpty()) {
            Log.d(TAG, "Target[$targetIndex]: straight line, no obstacles")
            return PlannedPath(
                points      = listOf(start, target),
                targetIndex = targetIndex,
                isStraight  = true
            )
        }

        Log.d(TAG, "Target[$targetIndex]: ${blockingZones.size} obstacle(s) found, planning detour")

        if (safeZones.isNotEmpty()) {
            val bestWp = safeZones
                .map { sz -> sz.center }
                .minByOrNull { wp ->
                    haversineM(start, wp) + haversineM(wp, target)
                }

            if (bestWp != null) {
              
                val subPath = listOf(start, bestWp, target)
                Log.d(TAG, "Target[$targetIndex]: detour via safe zone wp=${bestWp.latitude},${bestWp.longitude}")
                return PlannedPath(
                    points       = subPath,
                    targetIndex  = targetIndex,
                    isStraight   = false,
                    waypointUsed = bestWp
                )
            }
        }
        val primary = blockingZones.minByOrNull { zone ->
            distanceToSegmentM(zone.center, start, target)
        }!!

        val tangentPt = tangentBypassPoint(start, target, primary)
        Log.d(TAG, "Target[$targetIndex]: tangent bypass at ${tangentPt.latitude},${tangentPt.longitude}")

        return PlannedPath(
            points       = listOf(start, tangentPt, target),
            targetIndex  = targetIndex,
            isStraight   = false,
            waypointUsed = tangentPt
        )
    }


    fun segmentIntersectsCircle(
        a: LatLng,
        b: LatLng,
        centre: LatLng,
        radiusM: Double
    ): Boolean {
        val ax = 0.0
        val ay = 0.0
        val bx = lngDiffToM(b.longitude - a.longitude, a.latitude)
        val by = latDiffToM(b.latitude  - a.latitude)
        val cx = lngDiffToM(centre.longitude - a.longitude, a.latitude)
        val cy = latDiffToM(centre.latitude  - a.latitude)

        val dist = distancePointToSegment(cx, cy, ax, ay, bx, by)
        return dist < radiusM
    }

    private fun distancePointToSegment(
        px: Double, py: Double,
        ax: Double, ay: Double,
        bx: Double, by: Double
    ): Double {
        val dx = bx - ax
        val dy = by - ay
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0.0) return hypot(px - ax, py - ay)

        val t = ((px - ax) * dx + (py - ay) * dy) / lenSq
        val tc = t.coerceIn(0.0, 1.0)
        val projX = ax + tc * dx
        val projY = ay + tc * dy
        return hypot(px - projX, py - projY)
    }
    private fun distanceToSegmentM(point: LatLng, a: LatLng, b: LatLng): Double {
        val px = lngDiffToM(point.longitude - a.longitude, a.latitude)
        val py = latDiffToM(point.latitude  - a.latitude)
        val bx = lngDiffToM(b.longitude - a.longitude, a.latitude)
        val by = latDiffToM(b.latitude  - a.latitude)
        return distancePointToSegment(px, py, 0.0, 0.0, bx, by)
    }

    private fun tangentBypassPoint(a: LatLng, b: LatLng, zone: UnsafeZone): LatLng {
        val clearance = zone.radiusMeters + 8.0   

        val dx = lngDiffToM(b.longitude - a.longitude, a.latitude)
        val dy = latDiffToM(b.latitude  - a.latitude)
        val len = hypot(dx, dy).takeIf { it > 0 } ?: 1.0
        val perpX = -dy / len
        val perpY =  dx / len
        val cx = lngDiffToM(zone.center.longitude - a.longitude, a.latitude)
        val cy = latDiffToM(zone.center.latitude  - a.latitude)

        val t = ((cx * dx + cy * dy) / (len * len)).coerceIn(0.0, 1.0)
        val projX = t * dx
        val projY = t * dy
        val dotPerp = (cx - projX) * perpX + (cy - projY) * perpY
        val sign = if (dotPerp >= 0) -1.0 else 1.0

        val wpX = projX + sign * clearance * perpX
        val wpY = projY + sign * clearance * perpY
        val wpLat = a.latitude  + mToLatDiff(wpY)
        val wpLon = a.longitude + mToLngDiff(wpX, a.latitude)

        return LatLng(wpLat, wpLon)
    }

    private const val R = 6_371_000.0 
    fun latDiffToM(dLat: Double) = Math.toRadians(dLat) * R
    fun lngDiffToM(dLon: Double, refLat: Double) =
        Math.toRadians(dLon) * R * cos(Math.toRadians(refLat))
    fun mToLatDiff(metres: Double) = Math.toDegrees(metres / R)

    fun mToLngDiff(metres: Double, refLat: Double) =
        Math.toDegrees(metres / (R * cos(Math.toRadians(refLat))))
    fun haversineM(a: LatLng, b: LatLng): Double {
        val φ1 = Math.toRadians(a.latitude)
        val φ2 = Math.toRadians(b.latitude)
        val Δφ = Math.toRadians(b.latitude  - a.latitude)
        val Δλ = Math.toRadians(b.longitude - a.longitude)
        val s  = sin(Δφ/2).pow(2) + cos(φ1)*cos(φ2)*sin(Δλ/2).pow(2)
        return R * 2 * atan2(sqrt(s), sqrt(1-s))
    }
}
