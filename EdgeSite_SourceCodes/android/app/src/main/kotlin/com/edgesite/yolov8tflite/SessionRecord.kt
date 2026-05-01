package com.edgesite.yolov8tflite

import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log


data class SessionRecord(
    val sessionId      : String = "",
    val cameraId       : String = "",
    val startTime      : Long   = 0L,
    val endTime        : Long   = 0L,  
    val targetsDetected: Int    = 0
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "session_id"        to sessionId,
        "camera_id"         to cameraId,
        "start_time"        to startTime,
        "end_time"          to endTime,
        "targets_detected"  to targetsDetected
    )

    companion object {
        const val COLLECTION = "sessions"


        fun buildId(cameraId: String, startMs: Long): String =
            "sess_${cameraId}_$startMs"

        fun fromFirestoreMap(id: String, data: Map<String, Any?>): SessionRecord {
            val targets = when (val raw = data["targets_detected"]) {
                is Long   -> raw.toInt()
                is Int    -> raw
                is Double -> raw.toInt()
                else      -> 0
            }
            return SessionRecord(
                sessionId       = (data["session_id"] as? String) ?: id,
                cameraId        = (data["camera_id"]  as? String) ?: "",
                startTime       = (data["start_time"] as? Long)   ?: 0L,
                endTime         = (data["end_time"]   as? Long)   ?: 0L,
                targetsDetected = targets
            )
        }
    }
}

object SessionRepository {

    private const val TAG = "SessionRepository"
    private val db by lazy { FirebaseFirestore.getInstance() }

    fun create(
        record   : SessionRecord,
        onSuccess: (sessionId: String) -> Unit = {},
        onError  : (Exception) -> Unit         = { e -> Log.e(TAG, "create: ${e.message}") }
    ) {
        db.collection(SessionRecord.COLLECTION)
            .document(record.sessionId)
            .set(record.toFirestoreMap())
            .addOnSuccessListener {
                Log.i(TAG, "Session created: ${record.sessionId}")
                onSuccess(record.sessionId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Session create failed: ${e.message}")
                onError(e)
            }
    }

    fun close(
        sessionId      : String,
        endTime        : Long,
        targetsDetected: Int,
        onDone         : () -> Unit    = {},
        onError        : (Exception) -> Unit = { e -> Log.e(TAG, "close: ${e.message}") }
    ) {
        db.collection(SessionRecord.COLLECTION)
            .document(sessionId)
            .update(
                mapOf(
                    "end_time"         to endTime,
                    "targets_detected" to targetsDetected
                )
            )
            .addOnSuccessListener { Log.i(TAG, "Session closed: $sessionId"); onDone() }
            .addOnFailureListener { e -> Log.e(TAG, "Session close failed: ${e.message}"); onError(e) }
    }

    fun incrementTargets(sessionId: String) {
        db.collection(SessionRecord.COLLECTION)
            .document(sessionId)
            .update("targets_detected", com.google.firebase.firestore.FieldValue.increment(1))
            .addOnFailureListener { e -> Log.w(TAG, "increment failed: ${e.message}") }
    }

    fun fetchAll(
        limit   : Long                          = 100,
        onResult: (List<SessionRecord>) -> Unit,
        onError : (Exception) -> Unit           = { e -> Log.e(TAG, "fetchAll: ${e.message}") }
    ) {
        db.collection(SessionRecord.COLLECTION)
            .orderBy("start_time", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { snapshot ->
                val sessions = snapshot.documents.mapNotNull { doc ->
                    try { SessionRecord.fromFirestoreMap(doc.id, doc.data ?: emptyMap()) }
                    catch (e: Exception) { Log.w(TAG, "bad doc ${doc.id}: ${e.message}"); null }
                }
                onResult(sessions)
            }
            .addOnFailureListener { e -> onError(e); onResult(emptyList()) }
    }
}
