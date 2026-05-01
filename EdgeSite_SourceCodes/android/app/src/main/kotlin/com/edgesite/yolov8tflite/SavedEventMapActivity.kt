package com.edgesite.yolov8tflite

import android.graphics.Color
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SavedEventMapActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val TAG = "SavedEventMapActivity"
        @Volatile var pendingEvent: SavedEvent? = null
    }

    private lateinit var btnBack      : ImageButton
    private lateinit var tvTitle      : TextView
    private lateinit var tvInfo       : TextView
    private lateinit var tvPathPoints : TextView
    private lateinit var topBar       : View

    private var googleMap  : GoogleMap? = null
    private var pathPoly   : Polyline?  = null  
    private val event get() = pendingEvent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_saved_event_map)

        btnBack      = findViewById(R.id.btnSavedEventMapBack)
        tvTitle      = findViewById(R.id.tvSavedEventTitle)
        tvInfo       = findViewById(R.id.tvSavedEventInfo)
        tvPathPoints = findViewById(R.id.tvSavedEventPathPoints)
        topBar       = findViewById(R.id.savedEventMapTopBar)

        btnBack.setOnClickListener { finish() }
        event?.let { populateHeader(it) }

        applyWindowInsets()

        val mapFrag = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.savedEventMapContainer, mapFrag)
            .commit()
        mapFrag.getMapAsync(this)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(topBar.parent as View) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.setPadding(
                topBar.paddingLeft,
                bars.top + topBar.paddingBottom,
                topBar.paddingRight,
                topBar.paddingBottom
            )
            insets
        }
    }

    private fun populateHeader(ev: SavedEvent) {
        tvTitle.text = ObjectColorHelper.emojiForLabel(ev.objectType)
        tvTitle.setTextColor(ObjectColorHelper.colorForLabel(ev.objectType))
        val timeStr = if (ev.timestamp > 0L)
            SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(ev.timestamp))
        else "Unknown time"
        tvInfo.text       = "$timeStr  ·  ${"%.0f".format(ev.confidence * 100)}% confidence"
        tvPathPoints.text = "${ev.path.size} GPS points"
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled      = true
        }

        val ev = event
        if (ev == null) {
            Log.e(TAG, "No pendingEvent"); return
        }

        drawStartMarker(map, ev)
        drawPath(map, ev)
        drawTargetMarker(map, ev)
        zoomToPath(map, ev)
    }

    private fun drawStartMarker(map: GoogleMap, ev: SavedEvent) {
        val lat = ev.startLocation["lat"] ?: return
        val lon = ev.startLocation["lon"] ?: return
        if (lat == 0.0 && lon == 0.0) return

        map.addMarker(
            MarkerOptions()
                .position(LatLng(lat, lon))
                .title("Start")
                .snippet("%.5f, %.5f".format(lat, lon))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .zIndex(1f)
        )
    }

    private fun drawPath(map: GoogleMap, ev: SavedEvent) {
        if (ev.path.isEmpty()) return

        val points = ev.path.mapNotNull { pt ->
            val lat = pt["lat"] ?: return@mapNotNull null
            val lon = pt["lon"] ?: return@mapNotNull null
            if (lat == 0.0 && lon == 0.0) null else LatLng(lat, lon)
        }
        if (points.size < 2) return

        val lineColor = ObjectColorHelper.colorForLabel(ev.objectType)

        pathPoly?.remove()
        pathPoly = map.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color(lineColor)
                .width(5f)                  
                .geodesic(true)
                .startCap(RoundCap())      
                .endCap(RoundCap())
                .jointType(JointType.ROUND)
                .zIndex(2f)
        )
    }


    private fun drawTargetMarker(map: GoogleMap, ev: SavedEvent) {
        if (ev.lat == 0.0 && ev.lon == 0.0) return

        val timeStr = if (ev.timestamp > 0L)
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ev.timestamp))
        else ""

        val markerHue = ObjectColorHelper.markerHueForLabel(ev.objectType)
        val confPct   = "${"%.0f".format(ev.confidence * 100)}%"
        val label     = ev.objectType.replaceFirstChar { it.uppercase() }

        val marker = map.addMarker(
            MarkerOptions()
                .position(LatLng(ev.lat, ev.lon))
                .title("$label Detected")
                .snippet("$confPct  ·  $timeStr")
                .icon(BitmapDescriptorFactory.defaultMarker(markerHue))
                .zIndex(3f)
        )
        marker?.showInfoWindow()
    }


    private fun zoomToPath(map: GoogleMap, ev: SavedEvent) {
        val allPoints = mutableListOf<LatLng>()

        ev.startLocation["lat"]?.let { lat ->
            ev.startLocation["lon"]?.let { lon ->
                if (lat != 0.0 || lon != 0.0) allPoints.add(LatLng(lat, lon))
            }
        }
        ev.path.forEach { pt ->
            val lat = pt["lat"] ?: return@forEach
            val lon = pt["lon"] ?: return@forEach
            if (lat != 0.0 || lon != 0.0) allPoints.add(LatLng(lat, lon))
        }
        
        if (ev.lat != 0.0 || ev.lon != 0.0) allPoints.add(LatLng(ev.lat, ev.lon))

        if (allPoints.isEmpty()) return

        if (allPoints.size == 1) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(allPoints[0], 17f))
            return
        }

        val boundsBuilder = LatLngBounds.builder()
        allPoints.forEach { boundsBuilder.include(it) }
        try {
            val bounds  = boundsBuilder.build()
            val padding = (80 * resources.displayMetrics.density).toInt()
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        } catch (e: Exception) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(allPoints.last(), 17f))
        }
    }
}
