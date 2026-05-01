package com.edgesite.yolov8tflite.engine

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

 
class GpsReceiver(
    private val baseUrl: String,
    private val breadcrumbManager: GpsBreadcrumbManager,
    private val intervalMs: Long = 1000L
) {

    companion object {
        private const val TAG = "GpsReceiver"
        private const val GPS_PATH = "/gps"
        private const val CONNECT_TIMEOUT = 3000
        private const val READ_TIMEOUT = 3000
    }

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var future: ScheduledFuture<*>? = null

    fun start() {
        future = scheduler.scheduleAtFixedRate({
            try {
                poll()
            } catch (e: Exception) {
                Log.e(TAG, "GPS poll error: ${e.message}")
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS)
        Log.i(TAG, "GpsReceiver started → $baseUrl$GPS_PATH")
    }

    fun stop() {
        future?.cancel(false)
        scheduler.shutdown()
        Log.i(TAG, "GpsReceiver stopped")
    }

    private fun poll() {
        val url = URL("$baseUrl$GPS_PATH")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.requestMethod = "GET"

        try {
            conn.connect()
            if (conn.responseCode != 200) {
                Log.w(TAG, "HTTP ${conn.responseCode}")
                return
            }

            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)

            val crumb = GpsBreadcrumb(
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                lat       = json.getDouble("lat"),
                lon       = json.getDouble("lon"),
                altitude  = json.optDouble("alt", 0.0),
                accuracy  = json.optDouble("accuracy", 5.0).toFloat()
            )

            breadcrumbManager.ingest(crumb)

        } finally {
            conn.disconnect()
        }
    }
}