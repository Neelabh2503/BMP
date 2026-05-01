package com.edgesite.yolov8tflite

data class SavedEvent(
    val id            : String = "",
    val lat           : Double = 0.0,
    val lon           : Double = 0.0,
    val timestamp     : Long   = 0L,
    val objectType    : String = "",       
    val confidence    : Float  = 0f,       
    val path          : List<Map<String, Double>> = emptyList(),
    val startLocation : Map<String, Double>       = emptyMap(),
    val sessionId     : String = "",       
    val cameraId      : String = ""       
) {
    
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "type"           to "target",
        "object"         to objectType,
        "lat"            to lat,
        "lon"            to lon,
        "confidence"     to confidence,
        "timestamp"      to timestamp,
        "path"           to path,
        "start_location" to startLocation,
        "session_id"     to sessionId,
        "camera_id"      to cameraId
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreMap(id: String, data: Map<String, Any?>): SavedEvent {
            val objType = (data["object"] as? String)
                ?: (data["objectType"] as? String)
                ?: ""

            val conf = when (val raw = data["confidence"]) {
                is Double -> raw.toFloat()
                is Float  -> raw
                is Long   -> raw.toFloat()
                else      -> 0f
            }

            return SavedEvent(
                id            = id,
                lat           = (data["lat"]        as? Double) ?: 0.0,
                lon           = (data["lon"]        as? Double) ?: 0.0,
                timestamp     = (data["timestamp"]  as? Long)   ?: 0L,
                objectType    = objType,
                confidence    = conf,
                path          = (data["path"]       as? List<Map<String, Double>>) ?: emptyList(),
                startLocation = (data["start_location"] as? Map<String, Double>)   ?: emptyMap(),
                sessionId     = (data["session_id"] as? String) ?: "",
                cameraId      = (data["camera_id"]  as? String) ?: ""
            )
        }
    }
}
