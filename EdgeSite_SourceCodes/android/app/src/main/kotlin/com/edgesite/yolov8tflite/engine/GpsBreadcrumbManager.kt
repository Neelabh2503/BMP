package com.edgesite.yolov8tflite.engine

import android.util.Log
import kotlin.math.*

data class GpsBreadcrumb(
    val timestamp: Long,
    val lat: Double,
    val lon: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 5f,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val isSmoothed: Boolean = false
)

class GpsBreadcrumbManager {

    companion object {
        private const val TAG = "GpsBreadcrumb"
        private const val MIN_DISTANCE_M = 1.5        
        private const val MAX_SPEED_M_S = 15.0        
        private const val MAX_ACCURACY_M = 30f        
        private const val EWA_ALPHA = 0.3             
        private const val EARTH_RADIUS_M = 6_371_000.0
    }

    private val rawCrumbs = ArrayList<GpsBreadcrumb>(4096)
    private val smoothedCrumbs = ArrayList<GpsBreadcrumb>(4096)

    private var prevCrumb: GpsBreadcrumb? = null
    private var smoothLat = 0.0
    private var smoothLon = 0.0

    var onNewCrumb: ((GpsBreadcrumb) -> Unit)? = null

    @Synchronized
    fun ingest(raw: GpsBreadcrumb) {
        rawCrumbs.add(raw)

        if (!filterCrumb(raw)) return

        val smoothed = smooth(raw)
        smoothedCrumbs.add(smoothed)

        Log.d(TAG, "Crumb #${smoothedCrumbs.size}: (${smoothed.lat}, ${smoothed.lon})")
        onNewCrumb?.invoke(smoothed)
    }

    @Synchronized
    fun getPath(): List<GpsBreadcrumb> = smoothedCrumbs.toList()

    @Synchronized
    fun findClosest(timestampMs: Long, toleranceMs: Long = 3000L): GpsBreadcrumb? {
        return rawCrumbs.minByOrNull { abs(it.timestamp - timestampMs) }
            ?.takeIf { abs(it.timestamp - timestampMs) < toleranceMs }
    }

    @Synchronized
    fun exportGeoJson(): String {
        val coords = smoothedCrumbs.joinToString(",") {
            "[${it.lon},${it.lat},${it.altitude}]"
        }
        return """{"type":"LineString","coordinates":[$coords]}"""
    }

    @Synchronized
    fun clear() {
        rawCrumbs.clear()
        smoothedCrumbs.clear()
        prevCrumb = null
    }

    private fun filterCrumb(crumb: GpsBreadcrumb): Boolean {
        val prev = prevCrumb
        if (prev == null) {
            prevCrumb = crumb
            return true
        }

        if (crumb.accuracy > MAX_ACCURACY_M) {
            Log.d(TAG, "Rejected: poor accuracy ${crumb.accuracy}m")
            return false
        }

        val distM = haversine(prev, crumb)
        val dtSec = (crumb.timestamp - prev.timestamp) / 1000.0
        val speedMs = if (dtSec > 0) distM / dtSec else 0.0

        if (distM < MIN_DISTANCE_M) {
            Log.d(TAG, "Rejected: jitter ${distM.toInt()}m")
            return false
        }

        if (speedMs > MAX_SPEED_M_S) {
            Log.d(TAG, "Rejected: teleport ${speedMs.toInt()}m/s")
            return false
        }

        prevCrumb = crumb
        return true
    }


    private fun smooth(crumb: GpsBreadcrumb): GpsBreadcrumb {
        if (smoothedCrumbs.isEmpty()) {
            smoothLat = crumb.lat
            smoothLon = crumb.lon
        } else {
            smoothLat = EWA_ALPHA * crumb.lat + (1 - EWA_ALPHA) * smoothLat
            smoothLon = EWA_ALPHA * crumb.lon + (1 - EWA_ALPHA) * smoothLon
        }
        return crumb.copy(lat = smoothLat, lon = smoothLon, isSmoothed = true)
    }


    fun haversine(a: GpsBreadcrumb, b: GpsBreadcrumb): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val sinDLat = sin(dLat / 2)
        val sinDLon = sin(dLon / 2)
        val s = sinDLat * sinDLat +
                cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) *
                sinDLon * sinDLon
        return EARTH_RADIUS_M * 2 * atan2(sqrt(s), sqrt(1 - s))
    }
}