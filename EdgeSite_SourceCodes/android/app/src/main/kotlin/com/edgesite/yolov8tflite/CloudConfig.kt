package com.edgesite.yolov8tflite

object CloudConfig {

    const val CLOUDINARY_CLOUD_NAME   = "dd8zksknl"
    const val CLOUDINARY_UPLOAD_PRESET = "edgesite"

    const val FIRESTORE_COLLECTION     = "frames"
    const val POLL_INTERVAL_MS         = 1_000L

    const val OFFLINE_THRESHOLD_SECONDS = 10L
    const val MODEL_INPUT_SIZE         = 640
}
