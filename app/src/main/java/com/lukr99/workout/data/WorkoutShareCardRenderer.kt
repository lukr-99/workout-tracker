package com.lukr99.workout.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.lukr99.workout.domain.Estimates
import com.lukr99.workout.domain.WorkoutSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a completed lift to a shareable portrait "postcard" — brand header, workout name, a 2×2
 * stat grid (volume / sets / exercises / time) and any personal records set in the session — and
 * hands back a `FileProvider`-backed `ACTION_SEND` intent. Mirrors the run's
 * [com.lukr99.workout.data.run.ShareCardRenderer] (same authority/cache pattern); kept out of
 * `domain/` because it needs Android's `Canvas`. The volume math reuses the pure [Estimates].
 */
class WorkoutShareCardRenderer(
    context: Context,
    private val fileProviderAuthority: String = "${context.packageName}.files",
) {
    private val appContext = context.applicationContext

    suspend fun shareIntent(session: WorkoutSession, imperial: Boolean): Intent = withContext(Dispatchers.IO) {
        val bitmap = render(session, imperial)
        val dir = File(appContext.cacheDir, "shared-exports").apply { mkdirs() }
        val file = File(dir, "workout-${session.id}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val uri = FileProvider.getUriForFile(appContext, fileProviderAuthority, file)
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun render(session: WorkoutSession, imperial: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BG)

        val strengthEntries = session.entries.filter { it.isStrength }
        val volumeKg = strengthEntries.sumOf { Estimates.volume(it.strengthSets) }
        val setCount = strengthEntries.sumOf { it.strengthSets.size }
        val exerciseCount = session.entries.count { it.strengthSets.isNotEmpty() || it.cardioData != null }
        val durationSeconds = session.durationSeconds.takeIf { it > 0 }
            ?: (((session.endedAtUtc ?: 0L) - session.startedAtUtc) / 1_000).coerceAtLeast(0)
        val prNames = strengthEntries
            .filter { entry -> entry.strengthSets.any { it.isPr } }
            .map { it.exerciseSnapshotName }
            .distinct()

        drawHeader(canvas, session)
        drawStatGrid(canvas, volumeKg, imperial, setCount, exerciseCount, durationSeconds)
        drawRecords(canvas, prNames)
        return bitmap
    }

    private fun drawHeader(canvas: Canvas, session: WorkoutSession) {
        val brand = paint(EMBER, 40f, bold = true)
        canvas.drawText("EMBER", MARGIN, MARGIN + 44f, brand)

        val date = paint(MUTED, 34f, bold = true)
        val dateText = DATE_FORMAT.format(Date(session.completedDateUtc ?: session.startedAtUtc)).uppercase()
        canvas.drawText(dateText, MARGIN, MARGIN + 108f, date)

        val title = paint(Color.WHITE, 74f, bold = true)
        val name = session.name.ifBlank { "Workout" }
        ellipsized(name, title, WIDTH - 2 * MARGIN).forEachIndexed { i, line ->
            canvas.drawText(line, MARGIN, MARGIN + 210f + i * 84f, title)
        }
    }

    private fun drawStatGrid(
        canvas: Canvas,
        volumeKg: Double,
        imperial: Boolean,
        setCount: Int,
        exerciseCount: Int,
        durationSeconds: Long,
    ) {
        val volumeDisplay = if (imperial) volumeKg * LB_PER_KG else volumeKg
        val unit = if (imperial) "lb" else "kg"
        val top = 560f
        val colGap = (WIDTH - 2 * MARGIN)
        val leftX = MARGIN
        val rightX = MARGIN + colGap / 2f
        drawStat(canvas, leftX, top, "VOLUME", "${compactNumber(volumeDisplay)} $unit")
        drawStat(canvas, rightX, top, "SETS", setCount.toString())
        drawStat(canvas, leftX, top + 220f, "EXERCISES", exerciseCount.toString())
        drawStat(canvas, rightX, top + 220f, "TIME", formatDuration(durationSeconds))
    }

    private fun drawRecords(canvas: Canvas, prNames: List<String>) {
        val heading = paint(EMBER, 34f, bold = true)
        val body = paint(Color.WHITE, 46f, bold = true)
        val muted = paint(MUTED, 38f, bold = false)
        val y = 1080f
        if (prNames.isEmpty()) {
            canvas.drawText("SESSION COMPLETE", MARGIN, y, heading)
            canvas.drawText("Another one in the books.", MARGIN, y + 66f, muted)
            return
        }
        canvas.drawText("🏆 NEW PERSONAL RECORDS", MARGIN, y, heading)
        prNames.take(4).forEachIndexed { i, name ->
            val line = ellipsized(name, body, WIDTH - 2 * MARGIN).first()
            canvas.drawText("•  $line", MARGIN, y + 74f + i * 66f, body)
        }
        if (prNames.size > 4) {
            canvas.drawText("+${prNames.size - 4} more", MARGIN, y + 74f + 4 * 66f, muted)
        }
    }

    private fun drawStat(canvas: Canvas, x: Float, y: Float, label: String, value: String) {
        canvas.drawText(label, x, y, paint(MUTED, 34f, bold = true))
        canvas.drawText(value, x, y + 78f, paint(Color.WHITE, 76f, bold = true))
    }

    /** Split a string onto at most two lines that fit [maxWidth], ellipsizing the overflow. */
    private fun ellipsized(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (paint.measureText(text) <= maxWidth) return listOf(text)
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines += current.toString()
                current = StringBuilder(word)
                if (lines.size == 1) break
            }
        }
        if (lines.size < 2 && current.isNotEmpty()) lines += current.toString()
        // Ellipsize the last line if it still overflows.
        val last = lines.last()
        if (paint.measureText(last) > maxWidth) {
            var trimmed = last
            while (trimmed.isNotEmpty() && paint.measureText("$trimmed…") > maxWidth) {
                trimmed = trimmed.dropLast(1)
            }
            lines[lines.size - 1] = "$trimmed…"
        }
        return lines.take(2)
    }

    private fun paint(color: Int, size: Float, bold: Boolean) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun compactNumber(value: Double): String = when {
        value >= 100_000 -> "%.0fk".format(value / 1_000)
        value >= 10_000 -> "%.1fk".format(value / 1_000)
        else -> "%,d".format(Math.round(value))
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3_600
        val m = (seconds % 3_600) / 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    private companion object {
        const val WIDTH = 1080
        const val HEIGHT = 1350
        const val MARGIN = 80f
        const val LB_PER_KG = 2.2046226218
        val BG = Color.rgb(0x14, 0x12, 0x10)
        val EMBER = Color.rgb(0xE8, 0x62, 0x2C)
        val MUTED = Color.rgb(0x9A, 0x92, 0x8A)
        val DATE_FORMAT = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
    }
}
