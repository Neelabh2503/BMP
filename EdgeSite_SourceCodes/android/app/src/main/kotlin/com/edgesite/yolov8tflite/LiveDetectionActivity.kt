package com.edgesite.yolov8tflite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.edgesite.yolov8tflite.Constants.LABELS_PATH
import com.edgesite.yolov8tflite.Constants.MODEL_PATH
import com.edgesite.databinding.ActivityMainBinding
import com.edgesite.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LiveDetectionActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: Detector? = null

    private lateinit var cameraExecutor: ExecutorService
    private val sessionStartTime = System.currentTimeMillis()
    private val detectionLog     = ArrayList<String>(512)
    private var frameCount       = 0
    private val MAX_LOG_ENTRIES  = 1_000

    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()
        binding.btnBack.setOnClickListener { closeAndGoBack() }
        binding.btnStart.setOnClickListener { onStartClicked() }
        binding.btnStop.setOnClickListener  { onStopClicked()  }
        setRunningState(false)
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraExecutor.execute {
            detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.setPadding(
                binding.topBar.paddingLeft,
                systemBars.top + binding.topBar.paddingBottom, 
                binding.topBar.paddingRight,
                binding.topBar.paddingBottom
            )
            binding.bottomBar.setPadding(
                binding.bottomBar.paddingLeft,
                binding.bottomBar.paddingTop,
                binding.bottomBar.paddingRight,
                systemBars.bottom + binding.bottomBar.paddingTop
            )
            insets
        }
    }
    private fun setRunningState(running: Boolean) {
        isRunning = running
        binding.btnStart.isEnabled = !running
        binding.btnStart.alpha     = if (running) 0.5f else 1.0f
        binding.btnStop.isEnabled  = running
        binding.btnStop.alpha      = if (running) 1.0f else 0.5f
    }

    private fun onStartClicked() {
        if (isRunning) return
        setRunningState(true)
        if (imageAnalyzer == null) {
            bindCameraUseCases()
        } else {
            imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
                processFrame(imageProxy)
            }
        }
    }

    private fun onStopClicked() {
        if (!isRunning) return
        setRunningState(false)
        imageAnalyzer?.clearAnalyzer()
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        if (isRunning) {
            imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
                processFrame(imageProxy)
            }
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }
    private fun processFrame(imageProxy: androidx.camera.core.ImageProxy) {
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
            }
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0,
            bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )

        detector?.detect(rotatedBitmap)
    }
    private fun closeAndGoBack() {
        imageAnalyzer?.clearAnalyzer()
        cameraProvider?.unbindAll()
        cameraExecutor.execute {
            detector?.close()
            detector = null
        }

        if (detectionLog.isNotEmpty()) {
            val intent = Intent(this, DetectionSummaryActivity::class.java).apply {
                putExtra(DetectionSummaryActivity.EXTRA_SESSION_START, sessionStartTime)
                putExtra(DetectionSummaryActivity.EXTRA_SESSION_END,   System.currentTimeMillis())
                putExtra(DetectionSummaryActivity.EXTRA_TOTAL_FRAMES,  frameCount)
                putExtra(DetectionSummaryActivity.EXTRA_SOURCE,        "Live Camera")
                putStringArrayListExtra(DetectionSummaryActivity.EXTRA_DETECTION_EVENTS, detectionLog)
            }
            startActivity(intent)
        }

        finish()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it[Manifest.permission.CAMERA] == true) startCamera()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) startCamera()
        else requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    override fun onPause() {
        super.onPause()
        cameraProvider?.unbindAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        imageAnalyzer?.clearAnalyzer()
        cameraExecutor.execute { detector?.close() }
        cameraExecutor.shutdown()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        closeAndGoBack()
    }

    override fun onEmptyDetect() {
        runOnUiThread { binding.overlay.clear() }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        frameCount++
        if (detectionLog.size < MAX_LOG_ENTRIES) {
            val ts = System.currentTimeMillis()
            boundingBoxes.forEach { box ->
                detectionLog.add("$ts|${box.clsName}|${box.cnf}")
            }
        }
        runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
        }
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).toTypedArray()
    }
}
