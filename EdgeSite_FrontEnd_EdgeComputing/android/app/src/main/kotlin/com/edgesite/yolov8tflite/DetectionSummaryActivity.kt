package com.edgesite.yolov8tflite

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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

        // If the same object class disappears for more than this, it's a new appearance
        private const val GAP_THRESHOLD_MS = 2_000L
    }

    // Raw detection event parsed from the log line "$ts|label|conf"
    data class DetEvent(val timestamp: Long, val label: String, val conf: Float)

    // One continuous "sighting" of an object class
    data class DetectionSpan(
        val label: String,
        val startMs: Long,
        val endMs: Long,
        val peakConf: Float
    ) {
        val durationMs: Long get() = maxOf(endMs - startMs, 0L)
    }

    // Per-class aggregated stats derived from spans
    data class ClassStat(
        val label: String,
        val appearances: Int,
        val totalDurationMs: Long,
        val peakConf: Float
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection_summary)

        val sessionStart = intent.getLongExtra(EXTRA_SESSION_START, System.currentTimeMillis())
        val sessionEnd   = intent.getLongExtra(EXTRA_SESSION_END,   System.currentTimeMillis())
        val totalFrames  = intent.getIntExtra(EXTRA_TOTAL_FRAMES, 0)
        val source       = intent.getStringExtra(EXTRA_SOURCE) ?: "Live Camera"
        val rawEvents    = intent.getStringArrayListExtra(EXTRA_DETECTION_EVENTS) ?: arrayListOf()

        // Parse raw log lines
        val events: List<DetEvent> = rawEvents.mapNotNull { line ->
            val p = line.split("|")
            if (p.size == 3) DetEvent(p[0].toLongOrNull() ?: 0L, p[1], p[2].toFloatOrNull() ?: 0f)
            else null
        }

        // Merge per-frame detections into continuous spans per class
        val spans: List<DetectionSpan> = buildSpans(events)

        // Aggregate per-class stats from spans
        val classStats: List<ClassStat> = spans
            .groupBy { it.label }
            .map { (label, spanList) ->
                ClassStat(
                    label           = label,
                    appearances     = spanList.size,
                    totalDurationMs = spanList.sumOf { it.durationMs },
                    peakConf        = spanList.maxOf { it.peakConf }
                )
            }
            .sortedByDescending { it.totalDurationMs }

        // ── Header ──────────────────────────────────────────────────────────
        bindHeader(source, sessionStart, sessionEnd, totalFrames, events.size, classStats.size)

        // ── Written summary paragraph ───────────────────────────────────────
        val summaryText = buildWrittenSummary(source, sessionStart, sessionEnd, classStats, spans)
        findViewById<TextView>(R.id.tvWrittenSummary).text = summaryText

        // ── Objects Detected card ───────────────────────────────────────────
        val rvClasses = findViewById<RecyclerView>(R.id.rvClassSummary)
        rvClasses.layoutManager = LinearLayoutManager(this)
        rvClasses.isNestedScrollingEnabled = false
        rvClasses.adapter = ClassStatAdapter(classStats)

        if (classStats.isEmpty()) {
            findViewById<View>(R.id.tvNoObjects).visibility = View.VISIBLE
        }

        // ── Timeline card (one row per span, latest first) ──────────────────
        val spansLatestFirst = spans.sortedByDescending { it.startMs }

        val rvTimeline = findViewById<RecyclerView>(R.id.rvTimeline)
        rvTimeline.layoutManager = LinearLayoutManager(this)
        rvTimeline.isNestedScrollingEnabled = false
        rvTimeline.adapter = SpanAdapter(spansLatestFirst)

        if (spans.isEmpty()) {
            findViewById<View>(R.id.tvTimelineEmpty).visibility = View.VISIBLE
        }

        // ── Action buttons ──────────────────────────────────────────────────
        findViewById<Button>(R.id.btnSummaryClose).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSummaryShare).setOnClickListener {
            shareSummary(source, sessionStart, sessionEnd, classStats, spans)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Span builder
    //  For each class: sort events by time, merge events whose gap is less than
    //  GAP_THRESHOLD_MS into one span; otherwise start a new span.
    // ──────────────────────────────────────────────────────────────────────────
    private fun buildSpans(events: List<DetEvent>): List<DetectionSpan> {
        val spans = mutableListOf<DetectionSpan>()

        val byLabel = events.groupBy { it.label }

        for ((label, evts) in byLabel) {
            val sorted = evts.sortedBy { it.timestamp }

            var spanStart = sorted[0].timestamp
            var spanEnd   = sorted[0].timestamp
            var peakConf  = sorted[0].conf

            for (i in 1 until sorted.size) {
                val ev = sorted[i]
                if (ev.timestamp - spanEnd <= GAP_THRESHOLD_MS) {
                    // Extend current span
                    spanEnd  = ev.timestamp
                    peakConf = maxOf(peakConf, ev.conf)
                } else {
                    // Close current span and start a new one
                    spans.add(DetectionSpan(label, spanStart, spanEnd, peakConf))
                    spanStart = ev.timestamp
                    spanEnd   = ev.timestamp
                    peakConf  = ev.conf
                }
            }
            // Close the last open span for this label
            spans.add(DetectionSpan(label, spanStart, spanEnd, peakConf))
        }

        return spans.sortedBy { it.startMs }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Header binding
    // ──────────────────────────────────────────────────────────────────────────
    private fun bindHeader(
        source: String, start: Long, end: Long,
        frames: Int, totalDet: Int, uniqueClasses: Int
    ) {
        val dateSdf      = SimpleDateFormat("dd MMM yyyy  HH:mm:ss", Locale.getDefault())
        val durationSecs = (end - start) / 1000

        findViewById<TextView>(R.id.tvSummarySource).text   = source
        findViewById<TextView>(R.id.tvSummaryDate).text     = dateSdf.format(Date(start))
        findViewById<TextView>(R.id.tvSummaryDuration).text = formatDuration(durationSecs)
        findViewById<TextView>(R.id.tvStatFrames).text      = frames.toString()
        findViewById<TextView>(R.id.tvStatDetections).text  = totalDet.toString()
        findViewById<TextView>(R.id.tvStatClasses).text     = uniqueClasses.toString()

        val detView = findViewById<TextView>(R.id.tvStatDetections)
        detView.setTextColor(when {
            totalDet == 0  -> 0xFF888888.toInt()
            totalDet < 50  -> 0xFF4CAF50.toInt()
            totalDet < 200 -> 0xFFFFC107.toInt()
            else           -> 0xFFFF5722.toInt()
        })
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Written summary  –  no external library needed, built programmatically
    // ──────────────────────────────────────────────────────────────────────────
    private fun buildWrittenSummary(
        source: String,
        start: Long,
        end: Long,
        classStats: List<ClassStat>,
        spans: List<DetectionSpan>
    ): String {
        if (classStats.isEmpty()) {
            return "No objects were detected during this session."
        }

        val durationSecs  = (end - start) / 1000
        val durationLabel = formatDuration(durationSecs)
        val spanCount     = spans.size
        val classCount    = classStats.size
        val sb            = StringBuilder()

        // Opening sentence
        val objectWord     = if (classCount == 1) "1 type of object" else "$classCount types of objects"
        val appearanceWord = if (spanCount == 1)  "1 appearance"      else "$spanCount appearances"
        sb.append("During this $durationLabel session from $source, $objectWord ")
        sb.append("${if (classCount == 1) "was" else "were"} detected across $appearanceWord. ")

        // Per-class sentences
        classStats.forEachIndexed { index, stat ->
            val dur      = formatDuration(stat.totalDurationMs / 1000)
            val confPct  = (stat.peakConf * 100).roundToInt()
            val timesStr = when (stat.appearances) {
                1    -> "once"
                2    -> "twice"
                else -> "${stat.appearances} times"
            }
            val nameCapital = stat.label.replaceFirstChar { it.titlecase() }
            val connector   = when {
                classStats.size == 1                -> ""
                index == 0                          -> ""
                index == classStats.lastIndex       -> "Finally, "
                else                                -> ""
            }
            sb.append("${connector}${nameCapital} was spotted $timesStr ")
            sb.append("with a total visible duration of $dur ")
            sb.append("(peak confidence $confPct%). ")
        }

        return sb.toString().trim()
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Share
    // ──────────────────────────────────────────────────────────────────────────
    private fun shareSummary(
        source: String,
        start: Long,
        end: Long,
        classStats: List<ClassStat>,
        spans: List<DetectionSpan>
    ) {
        val dateSdf = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
        val timeSdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()

        sb.appendLine("═══ Edge Detection Summary ═══")
        sb.appendLine("Source   : $source")
        sb.appendLine("Started  : ${dateSdf.format(Date(start))}")
        sb.appendLine("Duration : ${formatDuration((end - start) / 1000)}")
        sb.appendLine()
        sb.appendLine(buildWrittenSummary(source, start, end, classStats, spans))
        sb.appendLine()
        sb.appendLine("── Object Appearances ──")
        classStats.forEach { stat ->
            val dur = formatDuration(stat.totalDurationMs / 1000)
            sb.appendLine(
                "  ${stat.label.padEnd(20)} " +
                "${stat.appearances} appearance${if (stat.appearances > 1) "s" else ""}   " +
                "$dur total   " +
                "peak ${(stat.peakConf * 100).roundToInt()}%"
            )
        }
        sb.appendLine()
        sb.appendLine("── Detection Spans ──")
        spans.sortedByDescending { it.startMs }.forEach { span ->
            val dur = formatDuration(span.durationMs / 1000)
            sb.appendLine(
                "  [${timeSdf.format(Date(span.startMs))} → ${timeSdf.format(Date(span.endMs))}]  " +
                "${span.label}  ($dur)"
            )
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Edge Detection Summary – ${dateSdf.format(Date(start))}")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(intent, "Share Summary"))
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────────────────
    private fun formatDuration(secs: Long): String = when {
        secs >= 3600 -> "${secs / 3600}h ${(secs % 3600) / 60}m ${secs % 60}s"
        secs >= 60   -> "${secs / 60}m ${secs % 60}s"
        else         -> "${secs}s"
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Class Stats Adapter  (Objects Detected card)
    //  Shows: label | ×N appearances  Xs total | peak XX%
    // ──────────────────────────────────────────────────────────────────────────
    inner class ClassStatAdapter(private val items: List<ClassStat>) :
        RecyclerView.Adapter<ClassStatAdapter.VH>() {

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
            val stat = items[position]
            val dur  = formatDuration(stat.totalDurationMs / 1000)

            holder.tvLabel.text = stat.label
            holder.tvCount.text = "×${stat.appearances}  $dur"
            holder.tvConf.text  = "peak ${(stat.peakConf * 100).roundToInt()}%"

            // Progress bar: share of longest class's duration
            val maxDuration = items.maxOfOrNull { it.totalDurationMs } ?: 1L
            val pct = ((stat.totalDurationMs * 100) / maxDuration).toInt().coerceIn(0, 100)
            holder.progressBar.progress = pct
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Span Adapter  (Detection Timeline card)
    //  One row per DetectionSpan, showing label and from→to time range
    // ──────────────────────────────────────────────────────────────────────────
    inner class SpanAdapter(private val items: List<DetectionSpan>) :
        RecyclerView.Adapter<SpanAdapter.VH>() {

        private val timeSdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvLabel    : TextView = v.findViewById(R.id.tvTimelineLabel)
            val tvTimeRange: TextView = v.findViewById(R.id.tvTimelineTime)
            val tvConf     : TextView = v.findViewById(R.id.tvTimelineConf)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_timeline_entry, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val span  = items[position]
            val start = timeSdf.format(Date(span.startMs))
            val end   = timeSdf.format(Date(span.endMs))
            val dur   = formatDuration(span.durationMs / 1000)

            holder.tvLabel.text     = span.label
            holder.tvTimeRange.text = "$start → $end  ($dur)"
            holder.tvConf.text      = "${(span.peakConf * 100).roundToInt()}%"

            val confColor = when {
                span.peakConf >= 0.80f -> 0xFF4CAF50.toInt()
                span.peakConf >= 0.50f -> 0xFFFFC107.toInt()
                else                   -> 0xFFFF5722.toInt()
            }
            holder.tvConf.setTextColor(confColor)
        }
    }
}