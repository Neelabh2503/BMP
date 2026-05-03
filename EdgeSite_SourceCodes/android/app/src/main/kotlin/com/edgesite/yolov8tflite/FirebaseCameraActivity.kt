package com.edgesite.yolov8tflite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.edgesite.R
import com.edgesite.yolov8tflite.engine.GpsBreadcrumb
import com.edgesite.yolov8tflite.engine.GpsBreadcrumbManager
import com.edgesite.yolov8tflite.engine.ObjectTracker
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import android.view.inputmethod.InputMethodManager
import java.util.concurrent.Executors

class FirebaseCameraActivity : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var etCameraId       : EditText
    private lateinit var etPassword       : EditText
    private lateinit var btnStart         : Button
    private lateinit var btnStop          : Button
    private lateinit var btnMarkTarget    : Button
    private lateinit var btnMarkUnsafe    : Button
    private lateinit var btnMarkSafe      : Button
    private lateinit var btnViewMap       : Button
    private lateinit var btnToggleControls: Button
    private lateinit var previewView      : android.widget.ImageView
    private lateinit var overlayView      : OverlayView
    private lateinit var tvStatus         : TextView
    private lateinit var tvInference      : TextView
    private lateinit var btnBack          : ImageButton
    private var tvGps    : TextView? = null
    private var tvTarget : TextView? = null

    private var sidePanel     : View? = null
    private var cloudBottomBar: View? = null

    private var controlsVisible = true
    private var detector: Detector? = null
    private val executor = Executors.newSingleThreadExecutor()

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private var cancellationTokenSource = CancellationTokenSource()
    val breadcrumbManager = GpsBreadcrumbManager()

    private val objectTracker by lazy {
        ObjectTracker(breadcrumbManager, CloudConfig.MODEL_INPUT_SIZE, CloudConfig.MODEL_INPUT_SIZE)
    }
    @Volatile private var sessionStartLocation: GpsBreadcrumb? = null
    @Volatile private var latestBox: BoundingBox? = null
    private var firestoreListener  : ListenerRegistration? = null
    private var isStreaming        = false

    @Volatile private var isProcessingFrame = false

    private var currentCamId = ""
    private var lastUrl      : String? = null
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val detectionLog       = ArrayList<String>(512)
    private var frameCount         = 0
    private val startTime          = System.currentTimeMillis()
    private var currentSessionId   = ""
    private var sessionTargetCount = 0

    private var pendingStartAfterPermission = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted   = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            if (pendingStartAfterPermission) {
                pendingStartAfterPermission = false
                fetchDeviceGpsAndStart()
            }
        } else {
            Toast.makeText(this, "⚠️ Location permission denied", Toast.LENGTH_LONG).show()
            if (pendingStartAfterPermission) {
                pendingStartAfterPermission = false
                val id = etCameraId.text.toString().trim()
                if (id.isNotEmpty()) startSessionNow(id)
            }
        }
    }

    companion object {
        private const val TAG = "FirebaseCamera"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_firebase_camera)
        bindViews()
        applyWindowInsets()
        setupListeners()
        setupTrackerCallbacks()
        executor.execute {
            detector = Detector(applicationContext, "model.tflite", "labels.txt", this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancellationTokenSource.cancel()
        stopStreaming()
        detector?.close()
        executor.shutdown()
        MapActivity.breadcrumbManager = null
        MapActivity.lockedTarget      = null
    }

    private var baseTopBarPaddingTop       = -1
    private var baseBottomBarPaddingBottom = -1

    private fun applyWindowInsets() {
        val rootView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(
            R.id.cloud_camera_container
        )
        val topBar    = findViewById<android.view.View>(R.id.cloudTopBar)
        val bottomBar = findViewById<android.view.View>(R.id.cloudBottomBar)

        topBar?.post {
            if (baseTopBarPaddingTop == -1)
                baseTopBarPaddingTop = topBar.paddingTop
        }
        bottomBar?.post {
            if (baseBottomBarPaddingBottom == -1)
                baseBottomBarPaddingBottom = bottomBar.paddingBottom
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars    = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val topInset    = maxOf(systemBars.top, displayCutout.top)
            val bottomInset = systemBars.bottom

            topBar?.setPadding(
                topBar.paddingLeft,
                topInset + (if (baseTopBarPaddingTop > 0) baseTopBarPaddingTop else 4),
                topBar.paddingRight,
                topBar.paddingBottom
            )
            bottomBar?.setPadding(
                bottomBar.paddingLeft,
                bottomBar.paddingTop,
                bottomBar.paddingRight,
                bottomInset + (if (baseBottomBarPaddingBottom > 0) baseBottomBarPaddingBottom else 8)
            )
            insets
        }
    }

    private fun bindViews() {
        etCameraId        = findViewById(R.id.etCameraId)
        etPassword        = findViewById(R.id.etChannelPassword)
        btnStart          = findViewById(R.id.btnStartCloud)
        btnStop           = findViewById(R.id.btnStopCloud)
        btnMarkTarget     = findViewById(R.id.btnMarkTarget)
        btnMarkUnsafe     = findViewById(R.id.btnMarkUnsafe)
        btnMarkSafe       = findViewById(R.id.btnMarkSafe)
        btnViewMap        = findViewById(R.id.btnViewMap)
        btnToggleControls = findViewById(R.id.btnToggleControls)
        previewView       = findViewById(R.id.cloudPreviewView)
        overlayView       = findViewById(R.id.cloudOverlayView)
        tvStatus          = findViewById(R.id.tvCloudStatus)
        tvInference       = findViewById(R.id.tvCloudInference)
        btnBack           = findViewById(R.id.btnCloudBack)
        tvGps             = findViewById(R.id.tvGps)
        tvTarget          = findViewById(R.id.tvTarget)
        sidePanel      = findViewById(R.id.sidePanel)
        cloudBottomBar = findViewById(R.id.cloudBottomBar)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { handleBack() }
        btnToggleControls.setOnClickListener {
            controlsVisible = !controlsVisible
            val newVis = if (controlsVisible) View.VISIBLE else View.GONE
            sidePanel?.visibility      = newVis
            cloudBottomBar?.visibility = newVis
            btnToggleControls.text     = if (controlsVisible) "Hide Controls" else "Show Controls"
        }

        btnStart.setOnClickListener {
            val id       = etCameraId.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (id.isEmpty()) {
                Toast.makeText(this, "Enter Camera ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Enter channel password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dismissKeyboard()
            btnStart.isEnabled = false
            tvStatus.text = "Verifying…"
            ChannelAuthManager.verify(
                channel   = id,
                password  = password,
                onSuccess = {
                    runOnUiThread {
                        sessionStartLocation = null
                        breadcrumbManager.clear()
                        requestLocationPermissionAndStart()
                    }
                },
                onFailure = { reason ->
                    runOnUiThread {
                        btnStart.isEnabled = true
                        tvStatus.text = "Auth failed"
                        Toast.makeText(this, "🔒 $reason", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        btnStop.setOnClickListener { stopStreaming() }
        btnMarkTarget.setOnClickListener { saveTarget() }
        btnMarkUnsafe.setOnClickListener { saveZone(isUnsafe = true) }
        btnMarkSafe.setOnClickListener   { saveZone(isUnsafe = false) }
        btnViewMap.setOnClickListener    { openMap() }
    }

    private fun requestLocationPermissionAndStart() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchDeviceGpsAndStart()
        } else {
            pendingStartAfterPermission = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchDeviceGpsAndStart() {
        val id = etCameraId.text.toString().trim()
        if (id.isEmpty()) return

        tvStatus.text = "Getting GPS fix…"
        tvGps?.text   = "Acquiring GPS…"

        cancellationTokenSource.cancel()
        cancellationTokenSource = CancellationTokenSource()

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    val crumb = GpsBreadcrumb(
                        timestamp = System.currentTimeMillis(),
                        lat       = location.latitude,
                        lon       = location.longitude,
                        altitude  = location.altitude,
                        accuracy  = location.accuracy
                    )
                    sessionStartLocation = crumb
                    runOnUiThread {
                        tvGps?.text = "${"%.5f".format(crumb.lat)}, ${"%.5f".format(crumb.lon)}"
                        tvStatus.text = "GPS ✔ — starting…"
                    }
                } else {
                    fetchLastKnownLocation(id)
                    return@addOnSuccessListener
                }
                startSessionNow(id)
            }.addOnFailureListener { fetchLastKnownLocation(id) }
        } catch (e: SecurityException) {
            startSessionNow(id)
        }
    }

    private fun fetchLastKnownLocation(camId: String) {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val crumb = GpsBreadcrumb(
                            timestamp = System.currentTimeMillis(),
                            lat       = location.latitude,
                            lon       = location.longitude,
                            altitude  = location.altitude,
                            accuracy  = location.accuracy
                        )
                        sessionStartLocation = crumb
                        runOnUiThread {
                            tvGps?.text = "${"%.5f".format(crumb.lat)}, ${"%.5f".format(crumb.lon)} (last known)"
                        }
                    }
                    startSessionNow(camId)
                }
                .addOnFailureListener { startSessionNow(camId) }
        } catch (e: SecurityException) {
            startSessionNow(camId)
        }
    }

    private fun startSessionNow(camId: String) {
        runOnUiThread { startStreaming(camId) }
    }

    private fun openMap() {
        MapActivity.breadcrumbManager = breadcrumbManager
        MapActivity.lockedTarget = objectTracker.activeTracks.values.firstOrNull { it.isLocked }
        sessionStartLocation?.let {
            MapActivity.sessionStartLat = it.lat
            MapActivity.sessionStartLon = it.lon
        } ?: run {
            MapActivity.sessionStartLat = 0.0
            MapActivity.sessionStartLon = 0.0
        }
        MapActivity.currentChannelId = currentCamId
        MapActivity.currentSessionId = currentSessionId
        startActivity(Intent(this, MapActivity::class.java))
    }

    private fun saveTarget() {
        val crumb = currentCrumb() ?: return
        val box   = latestBox ?: run {
            Toast.makeText(this, "⚠️ No detection active", Toast.LENGTH_SHORT).show()
            return
        }
        val startLoc = sessionStartLocation
        val startMap = if (startLoc != null)
            mapOf("lat" to startLoc.lat, "lon" to startLoc.lon)
        else
            mapOf("lat" to crumb.lat, "lon" to crumb.lon)

        val data = mapOf(
            "type"           to "target",
            "object"         to box.clsName,
            "lat"            to crumb.lat,
            "lon"            to crumb.lon,
            "confidence"     to box.cnf.toDouble(),
            "timestamp"      to System.currentTimeMillis(),
            "start_location" to startMap,
            "camera_id"      to currentCamId,
            "session_id"     to currentSessionId
        )

        setButtonWorking(btnMarkTarget, "…")
        firestore.collection("saved_events").add(data)
            .addOnSuccessListener {
                sessionTargetCount++
                SessionRepository.incrementTargets(currentSessionId)
                runOnUiThread {
                    resetButton(btnMarkTarget, "⛳")
                    Toast.makeText(this, "✅ Target saved (${box.clsName})", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    resetButton(btnMarkTarget, "⛳")
                    Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveZone(isUnsafe: Boolean) {
        val crumb = currentCrumb() ?: return
        val collection = if (isUnsafe) "unsafe_zones" else "safe_zones"
        val typeLabel  = if (isUnsafe) "unsafe" else "safe"
        val btn        = if (isUnsafe) btnMarkUnsafe else btnMarkSafe
        val emoji      = if (isUnsafe) "⚠️" else "🟢"

        val data = mapOf(
            "type"      to typeLabel,
            "lat"       to crumb.lat,
            "lon"       to crumb.lon,
            "radius"    to 20.0,
            "timestamp" to System.currentTimeMillis(),
            "camera_id" to currentCamId
        )

        setButtonWorking(btn, "…")
        firestore.collection(collection).add(data)
            .addOnSuccessListener {
                runOnUiThread {
                    resetButton(btn, emoji)
                    Toast.makeText(this, "$emoji Zone saved (20m)", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    resetButton(btn, emoji)
                    Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun currentCrumb(): GpsBreadcrumb? {
        val c = breadcrumbManager.getPath().lastOrNull()
        if (c == null || (c.lat == 0.0 && c.lon == 0.0)) {
            Toast.makeText(this, "⚠️ No GPS fix yet", Toast.LENGTH_SHORT).show()
            return null
        }
        return c
    }

    private fun setButtonWorking(btn: Button, text: String) { btn.isEnabled = false; btn.text = text }
    private fun resetButton(btn: Button, text: String)      { btn.isEnabled = true;  btn.text = text }

    private fun dismissKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    private fun setupTrackerCallbacks() {
        objectTracker.onTargetLocked = { track ->
            MapActivity.lockedTarget = track
            runOnUiThread { tvTarget?.text = "🔒 ${track.classLabel}" }
        }
        objectTracker.onTargetUnlocked = { _ ->
            MapActivity.lockedTarget = null
            runOnUiThread { tvTarget?.text = "—" }
        }
        breadcrumbManager.onNewCrumb = { crumb ->
            runOnUiThread {
                tvGps?.text = "${"%.5f".format(crumb.lat)}, ${"%.5f".format(crumb.lon)}"
            }
            MapActivity.onLiveCrumb?.invoke()
        }
    }

    private fun startStreaming(camId: String) {
        isStreaming    = true
        currentCamId   = camId
        lastUrl        = null
        isProcessingFrame = false
        sessionTargetCount = 0

        btnStart.isEnabled = false
        btnStop.isEnabled  = true
        etCameraId.isEnabled = false
        etPassword.isEnabled = false
        tvStatus.text = "Connecting…"

        val sessionStart = startTime
        currentSessionId = SessionRecord.buildId(camId, sessionStart)
        SessionRepository.create(
            SessionRecord(
                sessionId       = currentSessionId,
                cameraId        = camId,
                startTime       = sessionStart,
                endTime         = 0L,
                targetsDetected = 0
            )
        )

        firestoreListener = firestore
            .collection(CloudConfig.FIRESTORE_COLLECTION)
            .whereEqualTo("camera_id", camId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->

                if (!isStreaming) return@addSnapshotListener

                if (error != null) {
                    Log.e(TAG, "Listener error: ${error.message}")
                    runOnUiThread { tvStatus.text = "Stream error — retrying…" }
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    runOnUiThread { tvStatus.text = "Waiting for first frame…" }
                    return@addSnapshotListener
                }

                val doc = snapshot.documents[0]
                val url = doc.getString("image_url") ?: return@addSnapshotListener

                breadcrumbManager.ingest(
                    GpsBreadcrumb(
                        timestamp = doc.getLong("client_ts") ?: System.currentTimeMillis(),
                        lat       = doc.getDouble("lat")      ?: 0.0,
                        lon       = doc.getDouble("lon")      ?: 0.0,
                        altitude  = doc.getDouble("altitude") ?: 0.0,
                        accuracy  = (doc.getDouble("accuracy") ?: 50.0).toFloat()
                    )
                )

                if (url == lastUrl) return@addSnapshotListener
                lastUrl = url

                if (isProcessingFrame) {
                    Log.d(TAG, "Frame skipped — YOLO still running")
                    return@addSnapshotListener
                }

                isProcessingFrame = true
                frameCount++

                executor.execute {
                    val bmp = downloadBitmap(url)
                    if (bmp == null) {
                        runOnUiThread { tvStatus.text = "Download failed" }
                        isProcessingFrame = false
                        return@execute
                    }

                    runOnUiThread { previewView.setImageBitmap(bmp) }

                    try {
                        detector?.detect(
                            Bitmap.createScaledBitmap(
                                bmp,
                                CloudConfig.MODEL_INPUT_SIZE,
                                CloudConfig.MODEL_INPUT_SIZE,
                                false
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "detect: ${e.message}")
                    } finally {

                        isProcessingFrame = false
                    }
                }
            }
    }

    private fun stopStreaming() {
        isStreaming = false
        firestoreListener?.remove()
        firestoreListener = null
        isProcessingFrame = false

        if (currentSessionId.isNotEmpty()) {
            SessionRepository.close(
                sessionId       = currentSessionId,
                endTime         = System.currentTimeMillis(),
                targetsDetected = sessionTargetCount
            )
        }

        runOnUiThread {
            btnStart.isEnabled   = true
            btnStop.isEnabled    = false
            etCameraId.isEnabled = true
            etPassword.isEnabled = true
            tvStatus.text        = "Stopped"
        }
    }

    override fun onDetect(boxes: List<BoundingBox>, time: Long) {
        val ts = System.currentTimeMillis()
        boxes.forEach { if (detectionLog.size < 1_000) detectionLog.add("$ts|${it.clsName}|${it.cnf}") }
        latestBox = boxes.firstOrNull { it.clsName.equals("person", ignoreCase = true) }
            ?: boxes.maxByOrNull { it.cnf }
        objectTracker.update(boxes, frameCount.toLong(), ts)
        runOnUiThread {
            tvInference.text = "${time}ms"
            overlayView.setResults(boxes)
            overlayView.invalidate()
            tvStatus.text = "✔ $currentCamId"
        }
    }

    override fun onEmptyDetect() {
        objectTracker.update(emptyList(), frameCount.toLong(), System.currentTimeMillis())
        runOnUiThread { overlayView.clear(); tvStatus.text = "✔ $currentCamId (idle)" }
    }
    

    private fun downloadBitmap(url: String): Bitmap? {
        var conn: HttpURLConnection? = null
        var stream: InputStream? = null
        return try {
            conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8_000
            conn.readTimeout    = 8_000
            conn.connect()
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            stream = conn.inputStream
            BitmapFactory.decodeStream(stream)
        } catch (e: Exception) {
            Log.e(TAG, "dl: ${e.message}")
            null
        } finally {
            stream?.close()
            conn?.disconnect()
        }
    }

    private fun handleBack() {
        stopStreaming()
        if (detectionLog.isNotEmpty()) {
            startActivity(Intent(this, DetectionSummaryActivity::class.java).apply {
                putExtra(DetectionSummaryActivity.EXTRA_SESSION_START, startTime)
                putExtra(DetectionSummaryActivity.EXTRA_SESSION_END, System.currentTimeMillis())
                putExtra(DetectionSummaryActivity.EXTRA_TOTAL_FRAMES, frameCount)
                putExtra(DetectionSummaryActivity.EXTRA_SOURCE, "Channel ($currentCamId)")
                putStringArrayListExtra(DetectionSummaryActivity.EXTRA_DETECTION_EVENTS, detectionLog)
            })
        }
        finish()
    }
}
