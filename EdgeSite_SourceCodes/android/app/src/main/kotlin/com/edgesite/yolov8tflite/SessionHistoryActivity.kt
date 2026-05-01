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


class SessionHistoryActivity : AppCompatActivity() {

    private lateinit var recycler   : RecyclerView
    private lateinit var progress   : ProgressBar
    private lateinit var tvEmpty    : TextView
    private lateinit var btnBack    : ImageButton
    private lateinit var topBar     : View

    private val adapter = SessionAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_session_history)

        recycler = findViewById(R.id.rvSessionHistory)
        progress = findViewById(R.id.progressSessionHistory)
        tvEmpty  = findViewById(R.id.tvNoSessions)
        btnBack  = findViewById(R.id.btnSessionHistoryBack)
        topBar   = findViewById(R.id.sessionHistoryTopBar)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        btnBack.setOnClickListener { finish() }
        applyWindowInsets()
        loadSessions()
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

    private fun loadSessions() {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility  = View.GONE

        SessionRepository.fetchAll(
            limit    = 100,
            onResult = { sessions ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    if (sessions.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        adapter.setData(sessions)
                    }
                }
            },
            onError = { e ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    tvEmpty.visibility  = View.VISIBLE
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    inner class SessionAdapter : RecyclerView.Adapter<SessionAdapter.VH>() {

        private val items = mutableListOf<SessionRecord>()

        fun setData(sessions: List<SessionRecord>) {
            items.clear()
            items.addAll(sessions)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_session, parent, false)
        )

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val tvCamera   : TextView = view.findViewById(R.id.tvSessionCamera)
            private val tvDate     : TextView = view.findViewById(R.id.tvSessionDate)
            private val tvDuration : TextView = view.findViewById(R.id.tvSessionDuration)
            private val tvTargets  : TextView = view.findViewById(R.id.tvSessionTargets)
            private val tvStatus   : TextView = view.findViewById(R.id.tvSessionStatus)

            fun bind(s: SessionRecord) {
                val sdf = SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault())
                tvCamera.text  = "📡 ${s.cameraId.ifBlank { "Unknown" }}"
                tvDate.text    = sdf.format(Date(s.startTime))
                tvTargets.text = "🎯 ${s.targetsDetected} targets"

                if (s.endTime > 0L) {
                    val durMs  = s.endTime - s.startTime
                    val mins   = durMs / 60_000
                    val secs   = (durMs % 60_000) / 1_000
                    tvDuration.text = "${mins}m ${secs}s"
                    tvStatus.text   = "● Ended"
                    tvStatus.setTextColor(0xFFAAAAAA.toInt())
                } else {
                    tvDuration.text = "–"
                    tvStatus.text   = "● Active"
                    tvStatus.setTextColor(0xFF00FF88.toInt())
                }

                itemView.setOnClickListener { openDetail(s) }
            }
        }
    }

    private fun openDetail(session: SessionRecord) {
        SessionDetailActivity.pendingSession = session
        startActivity(Intent(this, SessionDetailActivity::class.java))
    }
}
