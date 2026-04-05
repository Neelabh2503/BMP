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

class ExternalCameraLiveDetection : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var imageView: ImageView
    private lateinit var overlayView: OverlayView
    private lateinit var inferenceTimeView: TextView

    private var detector: Detector? = null

    private val streamExecutor = Executors.newSingleThreadExecutor()
    private val detectExecutor = Executors.newSingleThreadExecutor()

    private val isRunning = AtomicBoolean(false)

    @Volatile
    private var latestBitmap: Bitmap? = null

    private val BASE_URL   = "http://10.252.126.248" //thisi is ESP's IP that we have..
    private val STREAM_URL = "$BASE_URL:81/stream"

    private val MODEL_SIZE = 640

    private val sessionStartTime = System.currentTimeMillis()
    private val detectionLog     = ArrayList<String>(512)
    private var frameCount       = 0
    private val MAX_LOG_ENTRIES  = 1_000
    companion object {
        private const val TAG = "ESP_STREAM"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_external_camera)

        imageView        = findViewById(R.id.previewView)
        overlayView      = findViewById(R.id.overlayView)
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
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        stopAndShowSummary()
    }
    private fun stopAndShowSummary() {
        isRunning.set(false)

        if (detectionLog.isNotEmpty()) {
            val intent = Intent(this, DetectionSummaryActivity::class.java).apply {
                putExtra(DetectionSummaryActivity.EXTRA_SESSION_START,    sessionStartTime)
                putExtra(DetectionSummaryActivity.EXTRA_SESSION_END,      System.currentTimeMillis())
                putExtra(DetectionSummaryActivity.EXTRA_TOTAL_FRAMES,     frameCount)
                putExtra(DetectionSummaryActivity.EXTRA_SOURCE,           "ESP Camera")
                putStringArrayListExtra(DetectionSummaryActivity.EXTRA_DETECTION_EVENTS, detectionLog)
            }
            startActivity(intent)
        }

        finish()
    }

    private fun startStream() {
        streamExecutor.execute {
            try {
                val url  = URL(STREAM_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.doInput = true
                conn.connect()

                val stream = conn.inputStream
                readMJPEG(stream)

            } catch (e: Exception) {
                Log.e(TAG, "Stream error: ${e.message}")
            }
        }
    }

    private fun readMJPEG(input: InputStream) {
        val buffer = ByteArray(1024 * 50)
        var bytesRead: Int

        val jpegStart = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        val jpegEnd   = byteArrayOf(0xFF.toByte(), 0xD9.toByte())

        val data = ByteArrayOutputStream()

        while (isRunning.get()) {
            bytesRead = input.read(buffer)
            if (bytesRead == -1) break

            data.write(buffer, 0, bytesRead)

            val bytes = data.toByteArray()

            val start = indexOf(bytes, jpegStart)
            val end   = indexOf(bytes, jpegEnd)

            if (start != -1 && end != -1 && end > start) {
                val jpeg = bytes.copyOfRange(start, end + 2)

                val bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
                if (bmp != null) {
                    latestBitmap = bmp

                    runOnUiThread {
                        imageView.setImageBitmap(bmp)
                    }
                }

                data.reset()
            }
        }
    }

    private fun indexOf(data: ByteArray, pattern: ByteArray): Int {
        for (i in data.indices) {
            if (i + pattern.size > data.size) break
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
            while (isRunning.get()) {
                val bmp = latestBitmap ?: continue

                val processed = letterbox(bmp)

                try {
                    detector?.detect(processed)
                } catch (e: Exception) {
                    Log.e(TAG, "Detection error: ${e.message}")
                }

                processed.recycle()
                Thread.sleep(30)
            }
        }
    }
    private fun letterbox(src: Bitmap): Bitmap {
        val dst    = Bitmap.createBitmap(MODEL_SIZE, MODEL_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dst)

        val scale = minOf(
            MODEL_SIZE.toFloat() / src.width,
            MODEL_SIZE.toFloat() / src.height
        )

        val newW = (src.width  * scale).toInt()
        val newH = (src.height * scale).toInt()

        val left = (MODEL_SIZE - newW) / 2
        val top  = (MODEL_SIZE - newH) / 2

        canvas.drawColor(Color.BLACK)

        val rect = Rect(left, top, left + newW, top + newH)

        canvas.drawBitmap(
            Bitmap.createScaledBitmap(src, newW, newH, true),
            null,
            rect,
            Paint(Paint.ANTI_ALIAS_FLAG)
        )

        return enhance(dst)
    }
    private fun enhance(src: Bitmap): Bitmap {
        val out    = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val cm = ColorMatrix().apply { setSaturation(1.5f) }

        val contrast = ColorMatrix(floatArrayOf(
            1.4f, 0f, 0f, 0f, -20f,
            0f, 1.4f, 0f, 0f, -20f,
            0f, 0f, 1.4f, 0f, -20f,
            0f, 0f, 0f, 1f, 0f
        ))

        cm.postConcat(contrast)

        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)

        canvas.drawBitmap(src, 0f, 0f, paint)

        return out
    }

    override fun onDetect(boxes: List<BoundingBox>, time: Long) {
        frameCount++
        if (detectionLog.size < MAX_LOG_ENTRIES) {
            val ts = System.currentTimeMillis()
            boxes.forEach { box ->
                detectionLog.add("$ts|${box.clsName}|${box.cnf}")
            }
        }

        runOnUiThread {
            inferenceTimeView.text = "${time}ms"
            overlayView.setResults(boxes)
            overlayView.invalidate()
        }
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            overlayView.setResults(emptyList())
            overlayView.invalidate()
        }
    }
}
