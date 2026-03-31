package com.edgesite.yolov8tflite

import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.edgesite.R

class ImageDetectionActivity : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var detector: Detector

    private lateinit var imageView: ImageView
    private lateinit var overlay: OverlayView
    private lateinit var inferenceTime: TextView

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                processImage(bitmap)
            }
        }

    private fun processImage(bitmap: Bitmap) {
        imageView.setImageBitmap(bitmap)
        overlay.clear()
        Thread {
            detector.detect(bitmap)
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        imageView     = findViewById(R.id.imageView)
        overlay       = findViewById(R.id.overlay)
        inferenceTime = findViewById(R.id.inferenceTime)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnPickImage).setOnClickListener {
            pickImage.launch("image/*")
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        detector = Detector(this, Constants.MODEL_PATH, Constants.LABELS_PATH, this)
        detector.restart(isGpu = false)
        pickImage.launch("image/*")
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTimeMs: Long) {
        runOnUiThread {
            inferenceTime.text = "${inferenceTimeMs}ms"
            overlay.setResults(boundingBoxes)
            overlay.invalidate()
        }
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            overlay.clear()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.close()
    }
}