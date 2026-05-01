package com.edgesite.yolov8tflite

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edgesite.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SavedEventsActivity : AppCompatActivity() {

    private lateinit var recycler   : RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty    : TextView
    private lateinit var btnBack    : ImageButton
    private lateinit var topBar     : LinearLayout 

    private val adapter = EventAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_saved_events)

        recycler    = findViewById(R.id.rvSavedEvents)
        progressBar = findViewById(R.id.progressSavedEvents)
        tvEmpty     = findViewById(R.id.tvNoEvents)
        btnBack     = findViewById(R.id.btnSavedEventsBack)
        topBar      = findViewById(R.id.savedEventsTopBar) 

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        btnBack.setOnClickListener { finish() }
        applyWindowInsets()

        loadEvents()
    }

    private fun applyWindowInsets() {
        val root = topBar.parent as View
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            topBar.setPadding(
                topBar.paddingLeft,
                bars.top + topBar.paddingBottom,   
                topBar.paddingRight,
                topBar.paddingBottom
            )

            recycler.setPadding(
                recycler.paddingLeft,
                recycler.paddingTop,
                recycler.paddingRight,
                bars.bottom + 8.dpToPx()
            )

            insets
        }
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density + 0.5f).toInt()

    private fun loadEvents() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility     = View.GONE

        EventRepository.fetchRecent(
            limit    = 50,
            onResult = { events ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    if (events.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        tvEmpty.visibility = View.GONE
                        adapter.setData(events)
                    }
                }
            },
            onError = { e ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    tvEmpty.visibility     = View.VISIBLE
                    Toast.makeText(this, "Error loading events: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    inner class EventAdapter : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

        private val items = mutableListOf<SavedEvent>()

        fun setData(events: List<SavedEvent>) {
            items.clear()
            items.addAll(events)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_saved_event, parent, false)
            return EventViewHolder(view)
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {

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
        if (epochMs == 0L) return "Unknown time"
        return SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
            .format(Date(epochMs))
    }
}
