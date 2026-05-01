package com.edgesite.yolov8tflite

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.edgesite.R
import com.edgesite.yolov8tflite.engine.GpsBreadcrumbManager
import com.edgesite.yolov8tflite.engine.TrackedObject
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val TAG = "MapActivity"

        @Volatile var breadcrumbManager : GpsBreadcrumbManager? = null
        @Volatile var lockedTarget      : TrackedObject?         = null
        @Volatile var sessionStartLat   : Double = 0.0
        @Volatile var sessionStartLon   : Double = 0.0
        @Volatile var currentChannelId  : String = ""
        @Volatile var currentSessionId  : String = ""

        @Volatile var onLiveCrumb: (() -> Unit)? = null

        private val PATH_COLORS = listOf(
            Color.parseColor("#1565C0"),  
            Color.parseColor("#6A1B9A"),  
            Color.parseColor("#E65100"),  
            Color.parseColor("#00695C"),  
            Color.parseColor("#AD1457"),  
            Color.parseColor("#558B2F"),  
            Color.parseColor("#4E342E"),  
        )

        fun pathColorForIndex(i: Int) = PATH_COLORS[i % PATH_COLORS.size]

        fun targetHue(conf: Float): Float = when {
            conf >= 0.80f -> BitmapDescriptorFactory.HUE_RED
            conf >= 0.50f -> BitmapDescriptorFactory.HUE_ORANGE
            else          -> BitmapDescriptorFactory.HUE_YELLOW
        }
    }

    private lateinit var btnBack      : ImageButton
    private lateinit var tvStatus     : TextView
    private lateinit var tvTargetInfo : TextView
    private lateinit var targetCard   : View
    private lateinit var tvLegend     : TextView


    private var googleMap             : GoogleMap? = null
    private var startCircle           : Circle?    = null
    private var startMarker           : Marker?    = null
    private val targetMarkers         = mutableListOf<Marker>()
    private val unsafeCircles         = mutableListOf<Circle>()
    private val safeCircles           = mutableListOf<Circle>()
    private var dronePathPolyline     : Polyline?  = null
    private var droneMarker           : Marker?    = null
    private val pathPolylines         = mutableListOf<Polyline>()

    private var droneMarkerIcon       : BitmapDescriptor? = null

    private val targets     = mutableListOf<SavedEvent>()
    private val unsafeZones = mutableListOf<UnsafeZone>()
    private val safeZones   = mutableListOf<SafeZone>()
    private var startLatLng : LatLng? = null
    private var cameraMoved = false
    private var highlightedPathIndex = -1   

    private val executor = Executors.newSingleThreadExecutor()
    private val db       by lazy { FirebaseFirestore.getInstance() }
    private val timeFmt  = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_map)
        bindViews()
        applyWindowInsets()

        val frag = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.mapFragmentContainer, frag)
            .commit()
        frag.getMapAsync(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        onLiveCrumb = null
        executor.shutdown()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val bars   = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val topBar = findViewById<View>(R.id.mapTopBar)
            val botBar = findViewById<View>(R.id.mapBottomBar)
            topBar?.setPadding(topBar.paddingLeft, bars.top, topBar.paddingRight, topBar.paddingBottom)
            botBar?.setPadding(botBar.paddingLeft, botBar.paddingTop, botBar.paddingRight, bars.bottom)
            insets
        }
    }

    private fun bindViews() {
        btnBack      = findViewById(R.id.btnMapBack)
        tvStatus     = findViewById(R.id.tvMapStatus)
        tvTargetInfo = findViewById(R.id.tvMapTargetInfo)
        targetCard   = findViewById(R.id.cardTarget)
        tvLegend     = findViewById(R.id.tvMapLegend)
        btnBack.setOnClickListener { finish() }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        droneMarkerIcon = buildEmojiMarkerIcon("🚁", 72)
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.uiSettings.apply {
            isZoomControlsEnabled     = true
            isCompassEnabled          = true
            isMyLocationButtonEnabled = false
        }

        map.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            val idx = marker.tag as? Int
            if (idx != null) {
                highlightPath(idx)
            }
            true
        }

        map.setOnMapClickListener { resetPathHighlight() }

        startLatLng = resolveStartPosition()
        startLatLng?.let { drawStartMarker(map, it) }
        drawDronePath(map)


        onLiveCrumb = {
            runOnUiThread { drawDronePath(map) }
        }

        tvStatus.text = "Loading…"

        loadUnsafeZones {
            loadSafeZones {
                loadTargets {
                    if (startLatLng == null) {
                        startLatLng = resolveStartPositionFromTargets()
                        startLatLng?.let { drawStartMarker(map, it) }
                    }
                    planAndDrawAllPaths()
                    updateUI()
                }
            }
        }
    }


    private fun clearAllMapOverlays() {
        pathPolylines.forEach   { it.remove() };  pathPolylines.clear()
        targetMarkers.forEach   { it.remove() };  targetMarkers.clear()
        unsafeCircles.forEach   { it.remove() };  unsafeCircles.clear()
        safeCircles.forEach     { it.remove() };  safeCircles.clear()
        startCircle?.remove();  startCircle  = null
        startMarker?.remove();  startMarker  = null
        dronePathPolyline?.remove(); dronePathPolyline = null
        droneMarker?.remove();  droneMarker  = null
        targets.clear()
        unsafeZones.clear()
        safeZones.clear()
    }

    private fun resolveStartPosition(): LatLng? {
        if (sessionStartLat != 0.0 || sessionStartLon != 0.0) {
            return LatLng(sessionStartLat, sessionStartLon)
        }
        val crumb = breadcrumbManager?.getPath()
            ?.firstOrNull { it.lat != 0.0 || it.lon != 0.0 }
        if (crumb != null) return LatLng(crumb.lat, crumb.lon)
        return null
    }

    private fun resolveStartPositionFromTargets(): LatLng? {
        val first = targets.firstOrNull() ?: return null
        return LatLng(first.lat + PathPlanner.mToLatDiff(50.0), first.lon)
    }

    private fun drawStartMarker(map: GoogleMap, pos: LatLng) {
        startCircle?.remove()
        startMarker?.remove()

        startCircle = map.addCircle(
            CircleOptions()
                .center(pos)
                .radius(8.0)
                .strokeColor(Color.parseColor("#1B5E20"))
                .strokeWidth(3f)
                .fillColor(Color.argb(180, 76, 175, 80))
                .zIndex(6f)
        )
        startMarker = map.addMarker(
            MarkerOptions()
                .position(pos)
                .title("Start")
                .snippet("%.5f, %.5f".format(pos.latitude, pos.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .zIndex(6f)
        )
        if (!cameraMoved) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 17f))
            cameraMoved = true
        }
    }

    private fun drawDronePath(map: GoogleMap) {
        val path = breadcrumbManager?.getPath() ?: return
        if (path.size < 2) return

        val pts = path.map { LatLng(it.lat, it.lon) }

        dronePathPolyline?.remove()
        dronePathPolyline = map.addPolyline(
            PolylineOptions()
                .addAll(pts)
                .color(Color.parseColor("#FF1744"))      
                .width(10f)                              
                .geodesic(true)
                .startCap(RoundCap())
                .endCap(RoundCap())
                .jointType(JointType.ROUND)
                .zIndex(8f)   
        )
        droneMarker?.remove()
        val last = path.last()
        droneMarker = map.addMarker(
            MarkerOptions()
                .position(LatLng(last.lat, last.lon))
                .title("Drone")
                .snippet("%.5f, %.5f".format(last.lat, last.lon))
                .icon(droneMarkerIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                .anchor(0.5f, 0.5f)
                .zIndex(9f)
        )
    }
    private fun buildEmojiMarkerIcon(emoji: String, sizePx: Int): BitmapDescriptor {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx * 0.75f
            textAlign = Paint.Align.CENTER
        }
        val x = sizePx / 2f
        val y = sizePx / 2f - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(emoji, x, y, paint)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    private fun loadUnsafeZones(onDone: () -> Unit) {
        db.collection("unsafe_zones").get()
            .addOnSuccessListener { snap ->
                unsafeCircles.forEach { it.remove() }; unsafeCircles.clear(); unsafeZones.clear()
                for (doc in snap.documents) {
                    val lat    = doc.getDouble("lat")    ?: continue
                    val lon    = doc.getDouble("lon")    ?: continue
                    val radius = doc.getDouble("radius") ?: 20.0
                    val zone   = UnsafeZone(doc.id, LatLng(lat, lon), radius)
                    unsafeZones.add(zone)
                    googleMap?.addCircle(
                        CircleOptions().center(zone.center).radius(radius)
                            .strokeColor(Color.RED).strokeWidth(3f)
                            .fillColor(Color.argb(60, 255, 0, 0)).zIndex(2f)
                    )?.also { unsafeCircles.add(it) }
                }
                onDone()
            }
            .addOnFailureListener { e -> Log.e(TAG, "loadUnsafe: ${e.message}"); onDone() }
    }

    private fun loadSafeZones(onDone: () -> Unit) {
        db.collection("safe_zones").get()
            .addOnSuccessListener { snap ->
                safeCircles.forEach { it.remove() }; safeCircles.clear(); safeZones.clear()
                for (doc in snap.documents) {
                    val lat    = doc.getDouble("lat")    ?: continue
                    val lon    = doc.getDouble("lon")    ?: continue
                    val radius = doc.getDouble("radius") ?: 15.0
                    val zone   = SafeZone(doc.id, LatLng(lat, lon), radius)
                    safeZones.add(zone)
                    googleMap?.addCircle(
                        CircleOptions().center(zone.center).radius(radius)
                            .strokeColor(Color.parseColor("#2E7D32")).strokeWidth(2f)
                            .fillColor(Color.argb(50, 76, 175, 80)).zIndex(2f)
                    )?.also { safeCircles.add(it) }
                }
                onDone()
            }
            .addOnFailureListener { e -> Log.e(TAG, "loadSafe: ${e.message}"); onDone() }
    }

    private fun loadTargets(onDone: () -> Unit) {
        targetMarkers.forEach { it.remove() }; targetMarkers.clear(); targets.clear()

        db.collection("saved_events").limit(100).get()
            .addOnSuccessListener { snap ->
                val channelId = currentChannelId
                val docs = snap.documents.filter { doc ->
                    val type         = doc.getString("type") ?: ""
                    val isTarget     = type == "target" || type.isEmpty()
                    val docChannel   = doc.getString("camera_id") ?: ""
                    val channelMatch = channelId.isEmpty() || docChannel.isEmpty()
                            || docChannel == channelId
                    isTarget && channelMatch
                }.sortedBy { it.getLong("timestamp") ?: 0L }

                docs.forEachIndexed { idx, doc ->
                    val lat  = doc.getDouble("lat")        ?: return@forEachIndexed
                    val lon  = doc.getDouble("lon")        ?: return@forEachIndexed
                    val conf = doc.getDouble("confidence")?.toFloat() ?: 0f
                    val obj  = doc.getString("object") ?: doc.getString("objectType") ?: "person"
                    val ts   = doc.getLong("timestamp") ?: 0L

                    targets.add(SavedEvent(id=doc.id, lat=lat, lon=lon, timestamp=ts,
                        objectType=obj, confidence=conf))

                    val num     = idx + 1
                    val confPct = "%.0f".format(conf * 100)
                    val timeStr = if (ts > 0L) timeFmt.format(Date(ts)) else "—"

                    val marker = googleMap?.addMarker(
                        MarkerOptions()
                            .position(LatLng(lat, lon))
                            .title("Target #$num")
                            .snippet("$obj · ${confPct}% · $timeStr")
                            .icon(BitmapDescriptorFactory.defaultMarker(targetHue(conf)))
                            .zIndex(5f)
                    )
                    marker?.tag = idx
                    if (marker != null) targetMarkers.add(marker)
                }

                Log.i(TAG, "Targets loaded: ${targets.size}")
                if (!cameraMoved && targets.isNotEmpty()) {
                    googleMap?.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(targets[0].lat, targets[0].lon), 16f)
                    )
                    cameraMoved = true
                }
                onDone()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "loadTargets FAILED: ${e.message}")
                tvStatus.text = "❌ ${e.message}"
                onDone()
            }
    }


    private fun planAndDrawAllPaths() {
        val map = googleMap ?: return

        if (targets.isEmpty()) {
            tvStatus.text = if (currentChannelId.isNotEmpty())
                "No targets for: $currentChannelId" else "No targets saved yet"
            return
        }

        val start = startLatLng ?: run {
            val fb = resolveStartPositionFromTargets()
            startLatLng = fb
            fb?.let { drawStartMarker(map, it) }
            fb
        } ?: run {
            tvStatus.text = "✓ ${targets.size} target(s) — no GPS for paths"
            return
        }
        pathPolylines.forEach { it.remove() }; pathPolylines.clear()
        highlightedPathIndex = -1

        tvStatus.text = "Drawing ${targets.size} path(s)…"

        executor.execute {
            val targetLatLngs = targets.map { LatLng(it.lat, it.lon) }
            val paths = try {
                PathPlanner.computePaths(
                    start       = start,
                    targets     = targetLatLngs,
                    unsafeZones = unsafeZones.toList(),
                    safeZones   = safeZones.toList()
                )
            } catch (e: Exception) {
                Log.e(TAG, "PathPlanner error: ${e.message}")
                emptyList()
            }

            runOnUiThread {
                pathPolylines.forEach { it.remove() }; pathPolylines.clear()

                for (path in paths) {
                    val color = pathColorForIndex(path.targetIndex)
                    val poly = map.addPolyline(
                        PolylineOptions()
                            .addAll(path.points)
                            .color(color)
                            .width(6f)     
                            .geodesic(true)
                            .startCap(RoundCap()) 
                            .endCap(RoundCap())
                            .jointType(JointType.ROUND)
                            .zIndex(4f)
                    )
                    pathPolylines.add(poly)
                }

                val detours = paths.count { !it.isStraight }
                tvStatus.text = "✓ ${targets.size} target(s)" +
                        if (detours > 0) " · $detours detour(s)" else ""
            }
        }
    }


    private fun highlightPath(targetIndex: Int) {
        if (pathPolylines.isEmpty()) return
        highlightedPathIndex = targetIndex

        for (i in pathPolylines.indices) {
            val poly      = pathPolylines[i]
            val baseColor = pathColorForIndex(i)
            if (i == targetIndex) {
                poly.color     = baseColor
                poly.width     = 10f
                poly.zIndex    = 5f
                poly.isVisible = true
            } else {
                poly.isVisible = false
            }
        }

        if (targetIndex < targets.size) {
            val t    = targets[targetIndex]
            val conf = "%.0f".format(t.confidence * 100)
            tvStatus.text = "Target #${targetIndex + 1} · ${t.objectType} · ${conf}%"
        }
    }

    private fun resetPathHighlight() {
        highlightedPathIndex = -1
        for (i in pathPolylines.indices) {
            pathPolylines[i].isVisible = true
            pathPolylines[i].color     = pathColorForIndex(i)
            pathPolylines[i].width     = 6f
            pathPolylines[i].zIndex    = 4f
        }
        tvStatus.text = "✓ ${targets.size} target(s)"
    }

    private fun updateUI() {
        if (targets.isEmpty()) {
            targetCard.visibility = View.GONE
            tvLegend.text = buildLegend(0)
            return
        }
        targetCard.visibility = View.VISIBLE
        val t    = targets[0]
        val conf = "%.0f".format(t.confidence * 100)
        tvTargetInfo.text = "🔴 Target #1 · ${t.objectType} · ${conf}%"
        tvLegend.text = buildLegend(targets.size)
    }

    private fun buildLegend(n: Int): String {
        val sb = StringBuilder()
        if (startLatLng != null)          sb.append("🟢 Start   ")
        if (unsafeZones.isNotEmpty())     sb.append("🔴 Unsafe(${unsafeZones.size})   ")
        if (safeZones.isNotEmpty())       sb.append("🟩 Safe(${safeZones.size})   ")
        if (n > 0)                        sb.append("📍 Target($n)   ")
        val crumbs = breadcrumbManager?.getPath()?.size ?: 0
        if (crumbs > 0)                   sb.append("🚁 Drone($crumbs pts)")
        val ch = currentChannelId
        if (ch.isNotEmpty())              sb.append("   📡 $ch")
        return sb.toString()
    }
}
