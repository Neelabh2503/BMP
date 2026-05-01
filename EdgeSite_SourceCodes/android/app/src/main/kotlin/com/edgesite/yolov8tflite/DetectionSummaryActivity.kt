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
import kotlin.math.roundToInt

class DetectionSummaryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SESSION_START    = "session_start"
        const val EXTRA_SESSION_END      = "session_end"
        const val EXTRA_TOTAL_FRAMES     = "total_frames"
        const val EXTRA_DETECTION_EVENTS = "detection_events"
        const val EXTRA_SOURCE           = "source"

        private const val GAP_THRESHOLD_MS = 2_000L
    }

    data class DetEvent(val timestamp: Long, val label: String, val conf: Float)

    data class DetectionSpan(
        val label: String,
        val startMs: Long,
        val endMs: Long,
        val peakConf: Float
    ) {
        val durationMs: Long get() = maxOf(endMs - startMs, 0L)
    }

    data class ClassStat(
        val label: String,
        val appearances: Int,
        val totalDurationMs: Long,
        val peakConf: Float,
        val totalDetections: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_detection_summary)
        applyWindowInsets()

        val sessionStart = intent.getLongExtra(EXTRA_SESSION_START, System.currentTimeMillis())
        val sessionEnd   = intent.getLongExtra(EXTRA_SESSION_END,   System.currentTimeMillis())
        val totalFrames  = intent.getIntExtra(EXTRA_TOTAL_FRAMES, 0)
        val source       = intent.getStringExtra(EXTRA_SOURCE) ?: "Live Camera"
        val rawEvents    = intent.getStringArrayListExtra(EXTRA_DETECTION_EVENTS) ?: arrayListOf()

        val events: List<DetEvent> = rawEvents.mapNotNull { line ->
            val p = line.split("|")
            if (p.size == 3) DetEvent(p[0].toLongOrNull() ?: 0L, p[1], p[2].toFloatOrNull() ?: 0f)
            else null
        }

        val spans      = buildSpans(events)
        val classStats = spans
            .groupBy { it.label }
            .map { (label, spanList) ->
                ClassStat(
                    label            = label,
                    appearances      = spanList.size,
                    totalDurationMs  = spanList.sumOf { it.durationMs },
                    peakConf         = spanList.maxOf { it.peakConf },
                    totalDetections  = events.count { it.label == label }
                )
            }
            .sortedByDescending { it.totalDurationMs }

        bindHeader(source, sessionStart, sessionEnd, totalFrames, events.size, classStats.size)

        val summaryText = buildWrittenSummary(source, sessionStart, sessionEnd, classStats)
        findViewById<TextView>(R.id.tvWrittenSummary).text = summaryText

        val rvClasses = findViewById<RecyclerView>(R.id.rvClassSummary)
        rvClasses.layoutManager = LinearLayoutManager(this)
        rvClasses.isNestedScrollingEnabled = false
        rvClasses.adapter = ClassStatAdapter(classStats)

        if (classStats.isEmpty()) {
            findViewById<View>(R.id.tvNoObjects).visibility = View.VISIBLE
        }
        val timeline   = spans.sortedByDescending { it.startMs }
        val rvTimeline = findViewById<RecyclerView>(R.id.rvTimeline)
        rvTimeline.layoutManager = LinearLayoutManager(this)
        rvTimeline.isNestedScrollingEnabled = false
        rvTimeline.adapter = TimelineAdapter(timeline, sessionStart)

        if (timeline.isEmpty()) {
            findViewById<View>(R.id.tvTimelineEmpty).visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.btnSummaryClose).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSummaryShare).setOnClickListener {
            shareSession(source, sessionStart, sessionEnd, classStats)
        }
    }

    private fun applyWindowInsets() {
        val scrollRoot  = findViewById<View>(R.id.summaryScrollView)
        val heroHeader  = findViewById<View>(R.id.summaryHeroHeader)
        ViewCompat.setOnApplyWindowInsetsListener(scrollRoot) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            heroHeader.setPadding(
                heroHeader.paddingLeft,
                bars.top + 16.dpToPx(),
                heroHeader.paddingRight,
                heroHeader.paddingBottom
            )
            insets
        }
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density + 0.5f).toInt()

    private fun bindHeader(
        source: String,
        sessionStart: Long,
        sessionEnd: Long,
        totalFrames: Int,
        totalDetections: Int,
        uniqueClasses: Int
    ) {
        val sdf        = SimpleDateFormat("dd MMM yyyy  HH:mm:ss", Locale.getDefault())
        val durationMs = sessionEnd - sessionStart
        val mins       = durationMs / 60_000
        val secs       = (durationMs % 60_000) / 1_000

        findViewById<TextView>(R.id.tvSummarySource).text   = source
        findViewById<TextView>(R.id.tvSummaryDate).text     = sdf.format(Date(sessionStart))
        findViewById<TextView>(R.id.tvSummaryDuration).text = "${mins}m ${secs}s"
        findViewById<TextView>(R.id.tvStatFrames).text      = totalFrames.toString()
        findViewById<TextView>(R.id.tvStatDetections).text  = totalDetections.toString()
        findViewById<TextView>(R.id.tvStatClasses).text     = uniqueClasses.toString()
    }

    private fun buildSpans(events: List<DetEvent>): List<DetectionSpan> {
        val sorted = events.sortedWith(compareBy({ it.label }, { it.timestamp }))
        val result = mutableListOf<DetectionSpan>()
        var i = 0
        while (i < sorted.size) {
            val first = sorted[i]
            var end   = first.timestamp
            var peak  = first.conf
            var j     = i + 1
            while (j < sorted.size && sorted[j].label == first.label &&
                   sorted[j].timestamp - end <= GAP_THRESHOLD_MS) {
                end  = sorted[j].timestamp
                peak = maxOf(peak, sorted[j].conf)
                j++
            }
            result.add(DetectionSpan(first.label, first.timestamp, end, peak))
            i = j
        }
        return result
    }

    private fun buildWrittenSummary(
        source: String,
        start: Long,
        end: Long,
        classStats: List<ClassStat>
    ): String {
        val durationMs = end - start
        val mins   = durationMs / 60_000
        val secs   = (durationMs % 60_000) / 1_000
        val durStr = "${mins}m ${secs}s"

        if (classStats.isEmpty())
            return "No objects were detected during this $durStr session from $source."

        val sb = StringBuilder()
        sb.append("During this $durStr session from $source, ${classStats.size} type(s) of objects were detected. ")
        classStats.take(3).forEach { stat ->
            val sDur = stat.totalDurationMs / 1_000
            val pct  = (stat.peakConf * 100).roundToInt()
            sb.append("${stat.label.replaceFirstChar { it.uppercase() }} spotted ${stat.appearances}x (${sDur}s, peak $pct%). ")
        }
        if (classStats.size > 3) sb.append("And ${classStats.size - 3} more class(es).")
        return sb.toString()
    }

    private fun shareSession(source: String, start: Long, end: Long, classStats: List<ClassStat>) {
        val sdf        = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        val durationMs = end - start
        val mins       = durationMs / 60_000
        val secs       = (durationMs % 60_000) / 1_000
        val sb         = StringBuilder()
        sb.append("EdgeSite Detection Report\n")
        sb.append("Source: $source\n")
        sb.append("Date: ${sdf.format(Date(start))}\n")
        sb.append("Duration: ${mins}m ${secs}s\n\n")
        classStats.forEach { sb.append("• ${it.label}: ${it.appearances} appearance(s)\n") }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(intent, "Share Detection Report"))
    }

    private inner class ClassStatAdapter(private val data: List<ClassStat>) :
        RecyclerView.Adapter<ClassStatAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvLabel : TextView    = view.findViewById(R.id.tvClassLabel)
            val tvCount : TextView    = view.findViewById(R.id.tvClassCount)
            val tvConf  : TextView    = view.findViewById(R.id.tvClassConf)
            val pbBar   : ProgressBar = view.findViewById(R.id.pbClassBar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_class_summary, parent, false)
        )

        override fun onBindViewHolder(holder: VH, position: Int) {
            val stat     = data[position]
            val maxDur   = data.maxOf { it.totalDurationMs }.coerceAtLeast(1L)
            val pct      = (stat.peakConf * 100).roundToInt()
            val durSec   = stat.totalDurationMs / 1_000

            holder.tvLabel.text = stat.label.replaceFirstChar { it.uppercase() }
            holder.tvCount.text = "×${stat.totalDetections}"
            holder.tvConf.text  = "peak $pct%  ${durSec}s"
            holder.pbBar.progress = ((stat.totalDurationMs * 100) / maxDur).toInt()
        }

        override fun getItemCount() = data.size
    }

    private inner class TimelineAdapter(
        private val data: List<DetectionSpan>,
        private val sessionStart: Long
    ) : RecyclerView.Adapter<TimelineAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvLabel : TextView = view.findViewById(R.id.tvTimelineLabel)
            val tvConf  : TextView = view.findViewById(R.id.tvTimelineConf)
            val tvTime  : TextView = view.findViewById(R.id.tvTimelineTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_timeline_entry, parent, false)
        )

        override fun onBindViewHolder(holder: VH, position: Int) {
            val span     = data[position]
            val offsetS  = (span.startMs - sessionStart) / 1_000
            val durS     = span.durationMs / 1_000
            val pct      = (span.peakConf * 100).roundToInt()
            val sdf      = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            holder.tvLabel.text = span.label.replaceFirstChar { it.uppercase() }
            holder.tvConf.text  = "$pct%"
            holder.tvTime.text  = "${sdf.format(Date(span.startMs))} → ${sdf.format(Date(span.endMs))}  (${durS}s)"
        }

        override fun getItemCount() = data.size
    }
}
