package com.edgesite.yolov8tflite.engine

import android.util.Log
import com.edgesite.yolov8tflite.BoundingBox
import kotlin.math.*


data class GeoDetection(
    val trackId: Int,
    val box: BoundingBox,
    val crumb: GpsBreadcrumb,
    val frameId: Long,
    val timestamp: Long,
    val velocityPx: Float,
    val directionDeg: Float,
    val isMoving: Boolean
)

data class TrackedObject(
    val trackId: Int,
    val classLabel: String,
    val detectionHistory: ArrayDeque<GeoDetection> = ArrayDeque(),
    var consecutiveHits: Int = 0,
    var missedFrames: Int = 0,
    var isLocked: Boolean = false,
    var lockLocation: GpsBreadcrumb? = null,
    var priorityScore: Float = 0f,
    val firstSeenAt: Long = System.currentTimeMillis(),
    var lastSeenAt: Long = System.currentTimeMillis()
)

data class MotionResult(val velocityPx: Float, val directionDeg: Float)

class ObjectTracker(
    private val breadcrumbManager: GpsBreadcrumbManager,
    private val imageWidth: Int = 640,
    private val imageHeight: Int = 640
) {

    companion object {
        private const val TAG = "ObjectTracker"
        private const val IOU_MATCH_THRESHOLD    = 0.25f
        private const val MAX_MISSED_FRAMES      = 5
        private const val HISTORY_SIZE           = 20
        private const val LOCK_CONSECUTIVE_FRAMES = 4
        private const val LOCK_RADIUS_METRES     = 8.0
        private const val MOVE_THRESHOLD_PX      = 8f
    }

    private var nextId = 0

    val activeTracks = HashMap<Int, TrackedObject>()

    private val prevCentroids = HashMap<Int, Pair<Float, Float>>()
    private val prevTimes     = HashMap<Int, Long>()

    var onTargetLocked  : ((TrackedObject) -> Unit)? = null
    var onTargetUnlocked: ((TrackedObject) -> Unit)? = null

    @Synchronized
    fun update(
        newBoxes: List<BoundingBox>,
        frameId: Long,
        frameTimestamp: Long
    ): List<GeoDetection> {
        val crumb = breadcrumbManager.findClosest(frameTimestamp)
            ?: GpsBreadcrumb(frameTimestamp, 0.0, 0.0)

        val matched = HashSet<Int>()
        val results = mutableListOf<GeoDetection>()

        for (box in newBoxes.sortedByDescending { it.cnf }) {
            var bestIoU = IOU_MATCH_THRESHOLD
            var bestId  = -1

            for ((id, track) in activeTracks) {
                if (id in matched) continue
                if (track.classLabel != box.clsName) continue
                val lastBox = track.detectionHistory.lastOrNull()?.box ?: continue
                val iou = calculateIoU(lastBox, box)
                if (iou > bestIoU) { bestIoU = iou; bestId = id }
            }

            val trackId = if (bestId != -1) bestId else nextId++
            matched.add(trackId)

            val motion = analyzeMotion(trackId, box)
            val geo = GeoDetection(
                trackId      = trackId,
                box          = box,
                crumb        = crumb,
                frameId      = frameId,
                timestamp    = System.currentTimeMillis(),
                velocityPx   = motion.velocityPx,
                directionDeg = motion.directionDeg,
                isMoving     = motion.velocityPx > MOVE_THRESHOLD_PX
            )

            updateTrack(trackId, box, geo)
            results.add(geo)
        }

        val toRemove = mutableListOf<Int>()
        for ((id, track) in activeTracks) {
            if (id !in matched) {
                track.missedFrames++
                track.consecutiveHits = 0
                if (track.missedFrames > MAX_MISSED_FRAMES) {
                    if (track.isLocked) onTargetUnlocked?.invoke(track)
                    toRemove.add(id)
                }
            }
        }
        toRemove.forEach { activeTracks.remove(it) }

        return results
    }


    private fun updateTrack(trackId: Int, box: BoundingBox, geo: GeoDetection) {
        val track = activeTracks.getOrPut(trackId) {
            TrackedObject(
                trackId      = trackId,
                classLabel   = box.clsName,
                firstSeenAt  = geo.timestamp
            )
        }

        track.detectionHistory.addLast(geo)
        if (track.detectionHistory.size > HISTORY_SIZE) {
            track.detectionHistory.removeFirst()
        }

        track.consecutiveHits++
        track.missedFrames = 0
        track.lastSeenAt   = geo.timestamp
        track.priorityScore = computePriority(track)

        if (!track.isLocked && track.consecutiveHits >= LOCK_CONSECUTIVE_FRAMES) {
            val recent = track.detectionHistory.takeLast(LOCK_CONSECUTIVE_FRAMES)
            if (isGpsCluster(recent)) {
                track.isLocked      = true
                track.lockLocation  = centroid(recent)
                Log.i(TAG, "TARGET LOCKED: ${track.classLabel} at " +
                        "(${track.lockLocation?.lat}, ${track.lockLocation?.lon}) " +
                        "priority=${track.priorityScore}")
                onTargetLocked?.invoke(track)
            }
        }
    }


    private fun isGpsCluster(history: List<GeoDetection>): Boolean {
        if (history.size < 2) return false
        val c = centroid(history)
        return history.all { det ->
            breadcrumbManager.haversine(det.crumb, c) < LOCK_RADIUS_METRES
        }
    }

    private fun centroid(history: List<GeoDetection>): GpsBreadcrumb {
        val avgLat = history.map { it.crumb.lat }.average()
        val avgLon = history.map { it.crumb.lon }.average()
        return GpsBreadcrumb(System.currentTimeMillis(), avgLat, avgLon)
    }


    private fun computePriority(track: TrackedObject): Float {
        val recent = track.detectionHistory.toList()
        val rawConf = recent.maxOfOrNull { it.box.cnf } ?: 0f
        val spatialConsistency = if (isGpsCluster(recent)) 1.0f else 0.3f
        val boostedConf = minOf(1.0f, rawConf + 0.2f * spatialConsistency *
                (recent.size.toFloat() / HISTORY_SIZE.toFloat()))
        val movement  = if (recent.lastOrNull()?.isMoving == true) 1.0f else 0.2f
        val durationSec = (track.lastSeenAt - track.firstSeenAt) / 1000f
        val durScore  = minOf(1.0f, durationSec / 30f)
        val spatial   = if (isGpsCluster(recent)) 1.0f else 0.0f
        return (boostedConf * 0.4f) + (movement * 0.2f) +
                (durScore * 0.2f)  + (spatial * 0.2f)
    }


    private fun analyzeMotion(trackId: Int, box: BoundingBox): MotionResult {
        val cx  = box.cx * imageWidth
        val cy  = box.cy * imageHeight
        val now = System.currentTimeMillis()

        val prev  = prevCentroids[trackId]
        val prevT = prevTimes[trackId]

        prevCentroids[trackId] = Pair(cx, cy)
        prevTimes[trackId]     = now

        if (prev == null || prevT == null) return MotionResult(0f, 0f)

        val dt = (now - prevT) / 1000f
        if (dt <= 0f) return MotionResult(0f, 0f)

        val dx  = cx - prev.first
        val dy  = cy - prev.second
        val vel = sqrt(dx * dx + dy * dy) / dt
        val dir = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        return MotionResult(vel, dir)
    }


    private fun calculateIoU(a: BoundingBox, b: BoundingBox): Float {
        val x1 = maxOf(a.x1, b.x1)
        val y1 = maxOf(a.y1, b.y1)
        val x2 = minOf(a.x2, b.x2)
        val y2 = minOf(a.y2, b.y2)
        val intersection = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val union = a.w * a.h + b.w * b.h - intersection
        return if (union <= 0f) 0f else intersection / union
    }
}
