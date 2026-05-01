package com.edgesite.yolov8tflite

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edgesite.R
import java.text.SimpleDateFormat
import java.util.*

class SessionDetailActivity : AppCompatActivity() {

    companion object {
        @Volatile var pendingSession: SessionRecord? = null
    }

    private lateinit var recycler : RecyclerView
    private lateinit var progress : ProgressBar
    private lateinit var tvEmpty  : TextView
    private lateinit var topBar   : View

    private val adapter = EventAdapter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_session_detail)

        val session = pendingSession ?: run { finish(); return }

        topBar   = findViewById(R.id.sessionDetailTopBar)
        recycler = findViewById(R.id.rvSessionEvents)
        progress = findViewById(R.id.progressSessionDetail)
        tvEmpty  = findViewById(R.id.tvNoSessionEvents)

        findViewById<ImageButton>(R.id.btnSessionDetailBack).setOnClickListener { finish() }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        applyWindowInsets()
        bindHeader(session)
        loadEvents(session.sessionId)
    }
    private fun applyWindowInsets() {
        val root = topBar.parent as View
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.setPadding(topBar.paddingLeft, bars.top + 8.dpToPx(),
                              topBar.paddingRight, topBar.paddingBottom)
            recycler.setPadding(recycler.paddingLeft, recycler.paddingTop,
                                recycler.paddingRight, bars.bottom + 8.dpToPx())
            insets
        }
    }

    private fun Int.dpToPx() = (this * resources.displayMetrics.density + 0.5f).toInt()

    private fun bindHeader(s: SessionRecord) {
        val sdf = SimpleDateFormat("dd MMM yyyy  HH:mm:ss", Locale.getDefault())

        findViewById<TextView>(R.id.tvDetailCamera).text    = "📡  ${s.cameraId.ifBlank { "Unknown" }}"
        findViewById<TextView>(R.id.tvDetailDate).text      = sdf.format(Date(s.startTime))
        findViewById<TextView>(R.id.tvDetailTargets).text   = "🎯  ${s.targetsDetected} targets"
        findViewById<TextView>(R.id.tvDetailSessionId).text = s.sessionId

        val durText = if (s.endTime > 0L) {
            val durMs = s.endTime - s.startTime
            "${durMs / 60_000}m ${(durMs % 60_000) / 1_000}s"
        } else "Active"
        findViewById<TextView>(R.id.tvDetailDuration).text = "⏱  $durText"
    }


    private fun loadEvents(sessionId: String) {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility  = View.GONE

        EventRepository.fetchBySession(
            sessionId = sessionId,
            limit     = 200,
            onResult  = { events ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    if (events.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        adapter.setData(events)
                    }
                }
            },
            onError = { e ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    tvEmpty.visibility  = View.VISIBLE
                    Toast.makeText(this, "Error loading events: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    inner class EventAdapter : RecyclerView.Adapter<EventAdapter.VH>() {

        private val items = mutableListOf<SavedEvent>()

        fun setData(events: List<SavedEvent>) {
            items.clear(); items.addAll(events); notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_saved_event, parent, false)
        )

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val tvObject    : TextView = view.findViewById(R.id.tvEventObject)
            private val tvTime      : TextView = view.findViewById(R.id.tvEventTime)
            private val tvConfidence: TextView = view.findViewById(R.id.tvEventConfidence)
            private val tvLocation  : TextView = view.findViewById(R.id.tvEventLocation)

            fun bind(event: SavedEvent) {
                tvObject.text     = ObjectColorHelper.emojiForLabel(event.objectType)
                tvTime.text       = formatTime(event.timestamp)
                tvConfidence.text = "Confidence: ${"%.0f".format(event.confidence * 100)}%"
                tvLocation.text   = "%.5f, %.5f".format(event.lat, event.lon)
                tvObject.setTextColor(ObjectColorHelper.colorForLabel(event.objectType))
                itemView.setOnClickListener { openEventMap(event) }
            }
        }
    }

    private fun openEventMap(event: SavedEvent) {
        SavedEventMapActivity.pendingEvent = event
        startActivity(Intent(this, SavedEventMapActivity::class.java))
    }

    private fun formatTime(epochMs: Long): String {
        if (epochMs == 0L) return "Unknown"
        return SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(epochMs))
    }
}
