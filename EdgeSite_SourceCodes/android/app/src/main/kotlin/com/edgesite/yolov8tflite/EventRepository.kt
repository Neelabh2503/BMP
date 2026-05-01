package com.edgesite.yolov8tflite

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object EventRepository {

    private const val TAG    = "EventRepository"
    const val COLLECTION     = "saved_events"

    private val db by lazy { FirebaseFirestore.getInstance() }

    fun save(
        event    : SavedEvent,
        onSuccess: (docId: String) -> Unit = {},
        onError  : (Exception) -> Unit     = { e -> Log.e(TAG, "save failed: ${e.message}") }
    ) {
        db.collection(COLLECTION)
            .add(event.toFirestoreMap())
            .addOnSuccessListener { ref ->
                Log.i(TAG, "Saved event: ${ref.id} (session=${event.sessionId})")
                onSuccess(ref.id)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save event: ${e.message}")
                onError(e)
            }
    }

    fun fetchRecent(
        limit   : Long = 50,
        onResult: (List<SavedEvent>) -> Unit,
        onError : (Exception) -> Unit = { e -> Log.e(TAG, "fetch failed: ${e.message}") }
    ) {
        db.collection(COLLECTION)
            .whereEqualTo("type", "target")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    onResult(parseSnapshot(snapshot))
                } else {
                    fetchRecentUnfiltered(limit, onResult, onError)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Filtered fetch failed (index?): ${e.message}")
                fetchRecentUnfiltered(limit, onResult, onError)
            }
    }
    fun fetchBySession(
        sessionId: String,
        limit    : Long = 200,
        onResult : (List<SavedEvent>) -> Unit,
        onError  : (Exception) -> Unit = { e -> Log.e(TAG, "fetchBySession: ${e.message}") }
    ) {
        if (sessionId.isBlank()) {
            Log.w(TAG, "fetchBySession called with blank sessionId — returning empty")
            onResult(emptyList())
            return
        }

        db.collection(COLLECTION)
            .whereEqualTo("session_id", sessionId)          
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { snapshot ->
                val events = parseSnapshot(snapshot)
                Log.i(TAG, "Session $sessionId → ${events.size} events")
                onResult(events)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "fetchBySession error: ${e.message}")
                onError(e)
                onResult(emptyList())
            }
    }

    fun fetchByCameraId(
        cameraId : String,
        limit    : Long = 100,
        onResult : (List<SavedEvent>) -> Unit,
        onError  : (Exception) -> Unit = { e -> Log.e(TAG, "fetchByCamera: ${e.message}") }
    ) {
        db.collection(COLLECTION)
            .whereEqualTo("camera_id", cameraId)
            .whereEqualTo("type", "target")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { onResult(parseSnapshot(it)) }
            .addOnFailureListener { e -> onError(e); onResult(emptyList()) }
    }

    private fun fetchRecentUnfiltered(
        limit   : Long,
        onResult: (List<SavedEvent>) -> Unit,
        onError : (Exception) -> Unit
    ) {
        db.collection(COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { onResult(parseSnapshot(it)) }
            .addOnFailureListener { e -> onError(e); onResult(emptyList()) }
    }

    private fun parseSnapshot(
        snapshot: com.google.firebase.firestore.QuerySnapshot
    ): List<SavedEvent> = snapshot.documents.mapNotNull { doc ->
        try { SavedEvent.fromFirestoreMap(doc.id, doc.data ?: emptyMap()) }
        catch (e: Exception) { Log.w(TAG, "Skipping malformed doc ${doc.id}: ${e.message}"); null }
    }
}
