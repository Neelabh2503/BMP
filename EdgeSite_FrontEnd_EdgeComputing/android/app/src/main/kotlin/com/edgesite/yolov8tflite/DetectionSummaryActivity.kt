package com.edgesite.yolov8tflite

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edgesite.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class DetectionSummaryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SESSION_START    = "session_start"
        const val EXTRA_SESSION_END      = "session_end"
        const val EXTRA_TOTAL_FRAMES     = "total_frames"
        const val EXTRA_DETECTION_EVENTS = "detection_events"
        const val EXTRA_SOURCE           = "source"
    }

    data class DetEvent(val timestamp: Long, val label: String, val conf: Float)
    data class ClassRow(val label: String, val count: Int, val maxConf: Float, val totalCount: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection_summary)
        val sessionStart = intent.getLongExtra(EXTRA_SESSION_START, System.currentTimeMillis())
        val sessionEnd   = intent.getLongExtra(EXTRA_SESSION_END,   System.currentTimeMillis())
        val totalFrames  = intent.getIntExtra(EXTRA_TOTAL_FRAMES, 0)
        val source       = intent.getStringExtra(EXTRA_SOURCE) ?: "Live Camera"
        val rawEvents    = intent.getStringArrayListExtra(EXTRA_DETECTION_EVENTS) ?: arrayListOf()
        val events = rawEvents.mapNotNull { line ->
            val p = line.split("|")
            if (p.size == 3) DetEvent(p[0].toLongOrNull() ?: 0L, p[1], p[2].toFloatOrNull() ?: 0f)
            else null
        }


        val classMap = LinkedHashMap<String, Pair<Int, Float>>()
        events.forEach { e ->
            val (cnt, mx) = classMap.getOrDefault(e.label, 0 to 0f)
            classMap[e.label] = (cnt + 1) to maxOf(mx, e.conf)
        }
        val classRows = classMap.entries
            .sortedByDescending { it.value.first }
            .map { ClassRow(it.key, it.value.first, it.value.second, events.size) }

        bindHeader(source, sessionStart, sessionEnd, totalFrames, events.size, classRows.size)

        val rvClasses = findViewById<RecyclerView>(R.id.rvClassSummary)
        rvClasses.layoutManager = LinearLayoutManager(this)
        rvClasses.isNestedScrollingEnabled = false
        rvClasses.adapter = ClassAdapter(classRows)

        val timeSdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timelineLines = events
            .takeLast(200)
            .reversed()
            .map { e ->
                TimelineItem(
                    time  = timeSdf.format(Date(e.timestamp)),
                    label = e.label,
                    conf  = (e.conf * 100).roundToInt()
                )
            }

        val rvTimeline = findViewById<RecyclerView>(R.id.rvTimeline)
        rvTimeline.layoutManager = LinearLayoutManager(this)
        rvTimeline.isNestedScrollingEnabled = false
        rvTimeline.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        rvTimeline.adapter = TimelineAdapter(timelineLines)

        findViewById<Button>(R.id.btnSummaryClose).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnSummaryShare).setOnClickListener {
            shareSummary(source, sessionStart, sessionEnd, events, classRows)
        }
    }
    private fun bindHeader(
        source: String,
        start: Long, end: Long,
        frames: Int, totalDet: Int, uniqueClasses: Int
    ) {
        val dateSdf = SimpleDateFormat("dd MMM yyyy  HH:mm:ss", Locale.getDefault())
        val durationSecs = (end - start) / 1000

        findViewById<TextView>(R.id.tvSummarySource).text   = source
        findViewById<TextView>(R.id.tvSummaryDate).text     = dateSdf.format(Date(start))
        findViewById<TextView>(R.id.tvSummaryDuration).text = formatDuration(durationSecs)
        findViewById<TextView>(R.id.tvStatFrames).text      = frames.toString()
        findViewById<TextView>(R.id.tvStatDetections).text  = totalDet.toString()
        findViewById<TextView>(R.id.tvStatClasses).text     = uniqueClasses.toString()

        val detView = findViewById<TextView>(R.id.tvStatDetections)
        detView.setTextColor(
            when {
                totalDet == 0  -> 0xFF888888.toInt()
                totalDet < 50  -> 0xFF4CAF50.toInt()
                totalDet < 200 -> 0xFFFFC107.toInt()
                else           -> 0xFFFF5722.toInt()
            }
        )
    }
    private fun shareSummary(
        source: String, start: Long, end: Long,
        events: List<DetEvent>, classRows: List<ClassRow>
    ) {
        val dateSdf = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.appendLine("═══ Edge Detection Summary ═══")
        sb.appendLine("Source   : $source")
        sb.appendLine("Started  : ${dateSdf.format(Date(start))}")
        sb.appendLine("Duration : ${formatDuration((end - start) / 1000)}")
        sb.appendLine("Total    : ${events.size} detections")
        sb.appendLine()
        sb.appendLine("── Objects Detected ──")
        classRows.forEach { r ->
            sb.appendLine("  ${r.label.padEnd(20)} ×${r.count}   peak ${(r.maxConf * 100).roundToInt()}%")
        }
        sb.appendLine()
        sb.appendLine("── Recent Timeline ──")
        val timeSdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        events.takeLast(30).reversed().forEach { e ->
            sb.appendLine("  [${timeSdf.format(Date(e.timestamp))}] ${e.label} ${(e.conf * 100).roundToInt()}%")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Edge Detection Summary – ${dateSdf.format(Date(start))}")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(intent, "Share Summary"))
    }
    private fun formatDuration(secs: Long): String = when {
        secs >= 3600 -> "${secs / 3600}h ${(secs % 3600) / 60}m ${secs % 60}s"
        secs >= 60   -> "${secs / 60}m ${secs % 60}s"
        else         -> "${secs}s"
    }
    inner class ClassAdapter(private val items: List<ClassRow>) :
        RecyclerView.Adapter<ClassAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvLabel    : TextView    = v.findViewById(R.id.tvClassLabel)
            val tvCount    : TextView    = v.findViewById(R.id.tvClassCount)
            val tvConf     : TextView    = v.findViewById(R.id.tvClassConf)
            val progressBar: ProgressBar = v.findViewById(R.id.pbClassBar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_class_summary, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val row = items[position]
            holder.tvLabel.text  = row.label
            holder.tvCount.text  = "×${row.count}"
            holder.tvConf.text   = "peak ${(row.maxConf * 100).roundToInt()}%"
            val pct = if (row.totalCount > 0) (row.count * 100 / row.totalCount) else 0
            holder.progressBar.progress = pct
        }
    }

    data class TimelineItem(val time: String, val label: String, val conf: Int)

    inner class TimelineAdapter(private val items: List<TimelineItem>) :
        RecyclerView.Adapter<TimelineAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvTime  : TextView = v.findViewById(R.id.tvTimelineTime)
            val tvLabel : TextView = v.findViewById(R.id.tvTimelineLabel)
            val tvConf  : TextView = v.findViewById(R.id.tvTimelineConf)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_timeline_entry, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvTime.text  = item.time
            holder.tvLabel.text = item.label
            holder.tvConf.text  = "${item.conf}%"

            val confColor = when {
                item.conf >= 80 -> 0xFF4CAF50.toInt()
                item.conf >= 50 -> 0xFFFFC107.toInt()
                else            -> 0xFFFF5722.toInt()
            }
            holder.tvConf.setTextColor(confColor)
        }
    }
}
