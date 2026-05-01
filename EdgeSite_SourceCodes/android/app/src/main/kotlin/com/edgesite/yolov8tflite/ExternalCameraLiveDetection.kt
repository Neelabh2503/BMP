package com.edgesite.yolov8tflite

import android.content.Intent
import android.graphics.*
import android.os.*
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.edgesite.R
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class ExternalCameraLiveDetection : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var imageView: ImageView
    private lateinit var overlayView: OverlayView
    private lateinit var inferenceTimeView: TextView

    private var detector: Detector? = null

    private val streamExecutor = Executors.newSingleThreadExecutor()
    private val detectExecutor = Executors.newSingleThreadExecutor()

    private val isRunning = AtomicBoolean(false)
    private val isDetecting = AtomicBoolean(false)

    private val latestFrame = AtomicReference<Bitmap?>(null)

    private val BASE_URL   = "http://10.252.126.248"
    private val STREAM_URL = "$BASE_URL:81/stream"

    private val MODEL_SIZE = 640

    private val sessionStartTime = System.currentTimeMillis()
    private val detectionLog     = ArrayList<String>(512)
    private var frameCount       = 0
    private val MAX_LOG_ENTRIES  = 1_000

    companion object {
        private const val TAG = "ESP_STREAM"
        private val JPEG_SOI  = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        private val JPEG_EOI  = byteArrayOf(0xFF.toByte(), 0xD9.toByte())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_external_camera)

        imageView         = findViewById(R.id.previewView)
        overlayView       = findViewById(R.id.overlayView)
        inferenceTimeView = findViewById(R.id.inferenceTime)

        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            stopAndShowSummary()
        }

        detectExecutor.execute {
            detector = Detector(
                applicationContext,
                Constants.MODEL_PATH,
                Constants.LABELS_PATH,
                this
            )
        }
    }

    override fun onResume() {
        super.onResume()
        isRunning.set(true)
        startStream()
        startDetectionLoop()
    }

    override fun onPause() {
        super.onPause()
        isRunning.set(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.set(false)
        detector?.close()
        latestFrame.set(null)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        stopAndShowSummary()
    }

    private fun stopAndShowSummary() {
        isRunning.set(false)

        if (detectionLog.isNotEmpty()) {
            val intent = Intent(this, DetectionSummaryActivity::class.java).apply {
                putExtra(DetectionSummaryActivity.EXTRA_SESSION_START, sessionStartTime)
                putExtra(DetectionSummaryActivity.EXTRA_SESSION_END, System.currentTimeMillis())
                putExtra(DetectionSummaryActivity.EXTRA_TOTAL_FRAMES, frameCount)
                putExtra(DetectionSummaryActivity.EXTRA_SOURCE, "ESP Camera")
                putStringArrayListExtra(
                    DetectionSummaryActivity.EXTRA_DETECTION_EVENTS,
                    detectionLog
                )
            }
            startActivity(intent)
        }

        finish()
    }

    private fun startStream() {
        streamExecutor.execute {
            while (isRunning.get()) {
                try {
                    val conn = (URL(STREAM_URL).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 5000
                        readTimeout = 10000
                        doInput = true
                    }
                    conn.connect()
                    readMJPEG(conn.inputStream)
                    conn.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Stream error: ${e.message}")
                }
                Thread.sleep(1000)
            }
        }
    }

    private fun readMJPEG(input: InputStream) {
        val buffer = ByteArray(8192)
        val data = ByteArrayOutputStream()

        while (isRunning.get()) {
            val n = input.read(buffer)
            if (n == -1) break

            data.write(buffer, 0, n)

            val bytes = data.toByteArray()
            var searchFrom = 0

            while (true) {
                val start = indexOf(bytes, JPEG_SOI, searchFrom)
                val end   = indexOf(bytes, JPEG_EOI, start + 2)

                if (start == -1 || end == -1) break

                val jpeg = bytes.copyOfRange(start, end + 2)

                val bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
                if (bmp != null) {
                    latestFrame.set(bmp)

                    runOnUiThread {
                        imageView.setImageBitmap(bmp)
                    }
                }

                searchFrom = end + 2
            }

            data.reset()
            if (searchFrom < bytes.size) {
                data.write(bytes, searchFrom, bytes.size - searchFrom)
            }
        }
    }

    private fun indexOf(data: ByteArray, pattern: ByteArray, from: Int): Int {
        for (i in from until data.size - pattern.size) {
            var found = true
            for (j in pattern.indices) {
                if (data[i + j] != pattern[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    private fun startDetectionLoop() {
        detectExecutor.execute {

            var skip = 0

            while (isRunning.get()) {

                val frame = latestFrame.get() ?: continue

                if (!isDetecting.compareAndSet(false, true)) {
                    Thread.sleep(5)
                    continue
                }

                skip++
                if (skip % 3 != 0) {
                    isDetecting.set(false)
                    continue
                }

                try {
                    val copy = frame.copy(Bitmap.Config.ARGB_8888, false)

                    val processed = letterbox(copy)
                    copy.recycle()

                    detector?.detect(processed)
                    processed.recycle()

                } catch (e: Exception) {
                    Log.e(TAG, "Detection error: ${e.message}")
                } finally {
                    isDetecting.set(false)
                }
            }
        }
    }

    private fun letterbox(src: Bitmap): Bitmap {
        val dst = Bitmap.createBitmap(MODEL_SIZE, MODEL_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dst)
        canvas.drawColor(Color.BLACK)

        val scale = minOf(
            MODEL_SIZE.toFloat() / src.width,
            MODEL_SIZE.toFloat() / src.height
        )

        val newW = (src.width * scale).toInt()
        val newH = (src.height * scale).toInt()

        val left = (MODEL_SIZE - newW) / 2
        val top  = (MODEL_SIZE - newH) / 2

        val scaled = Bitmap.createScaledBitmap(src, newW, newH, true)

        canvas.drawBitmap(scaled, left.toFloat(), top.toFloat(), null)

        scaled.recycle()

        return dst
    }


    override fun onDetect(boxes: List<BoundingBox>, time: Long) {
        frameCount++

        if (detectionLog.size < MAX_LOG_ENTRIES) {
            val ts = System.currentTimeMillis()
            boxes.forEach {
                detectionLog.add("$ts|${it.clsName}|${it.cnf}")
            }
        }

        runOnUiThread {
            inferenceTimeView.text = "${time}ms"
            overlayView.setResults(boxes)
            overlayView.postInvalidateOnAnimation()
        }
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            overlayView.setResults(emptyList())
            overlayView.postInvalidateOnAnimation()
        }
    }
}