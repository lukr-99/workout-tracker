package com.lukr99.workout.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.lukr99.workout.R
import com.lukr99.workout.domain.Estimates
import com.lukr99.workout.domain.ShareExerciseLine
import com.lukr99.workout.domain.Units
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutShareSummary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a completed lift to a shareable card: the Ember logo header, workout name, a row of stat
 * tiles (volume, sets, time, PRs) and each exercise with its best set, PRs marked. When there is
 * room each exercise also lists its sets, and the card is only as tall as its content, from square
 * up to 4:5 ([ShareListLayout]). Hands back a
 * `FileProvider`-backed `ACTION_SEND` intent. Shares its look with the run's
 * [com.lukr99.workout.data.run.ShareCardRenderer] through [ShareCardStyle]; kept out of `domain/`
 * because it needs Android's `Canvas`. The text comes from the pure [WorkoutShareSummary].
 */
class WorkoutShareCardRenderer(
    context: Context,
    private val fileProviderAuthority: String = "${context.packageName}.files",
) {
    private val appContext = context.applicationContext

    data class PreparedShare(val bitmap: Bitmap, val intent: Intent)

    /** Render and persist once so the preview is the exact image attached to the share intent. */
    suspend fun prepare(session: WorkoutSession, imperial: Boolean): PreparedShare = withContext(Dispatchers.IO) {
        val bitmap = render(session, imperial)
        val dir = File(appContext.cacheDir, "shared-exports").apply { mkdirs() }
        val file = File(dir, "workout-${session.id}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(appContext, fileProviderAuthority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        PreparedShare(bitmap, intent)
    }

    suspend fun shareIntent(session: WorkoutSession, imperial: Boolean): Intent {
        val prepared = prepare(session, imperial)
        prepared.bitmap.recycle()
        return prepared.intent
    }

    internal fun render(session: WorkoutSession, imperial: Boolean): Bitmap {
        val strengthEntries = session.entries.filter { it.isStrength }
        val volumeKg = strengthEntries.sumOf { Estimates.volume(it.strengthSets) }
        val setCount = strengthEntries.sumOf { it.strengthSets.size }
        val durationSeconds = session.durationSeconds.takeIf { it > 0 }
            ?: (((session.endedAtUtc ?: 0L) - session.startedAtUtc) / 1_000).coerceAtLeast(0)
        val prCount = strengthEntries.count { entry -> entry.strengthSets.any { it.isPr } }
        val dateText = DATE_FORMAT.format(Date(session.completedDateUtc ?: session.startedAtUtc))
        val lines = WorkoutShareSummary.exerciseLines(session, imperial)

        // Lay everything out first: the card's height follows the exercise list.
        val titlePaint = ShareCardStyle.paint(ShareCardStyle.TEXT, 76f)
        val titleLines = ShareCardStyle.wrap(session.name.ifBlank { "Workout" }, titlePaint, WIDTH - 2 * MARGIN)
        val firstTitleBaseline = HEADER_BASELINE + 118f
        val tilesTop = firstTitleBaseline + (titleLines.size - 1) * TITLE_LINE + 52f
        val headingBaseline = tilesTop + TILE_HEIGHT + 88f
        val rowsTop = headingBaseline + 26f
        val plan = ShareListLayout.plan(
            hasSets = lines.map { it.workingSets.isNotEmpty() },
            rowsTop = rowsTop,
            bottomSpace = BOTTOM_SPACE,
            compactHeight = COMPACT_ROW,
            expandedHeight = EXPANDED_ROW,
            minHeight = MIN_HEIGHT,
            maxHeight = MAX_HEIGHT,
        )

        val bitmap = Bitmap.createBitmap(WIDTH, plan.cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        ShareCardStyle.drawBackground(canvas, WIDTH, plan.cardHeight)
        ShareCardStyle.drawHeader(canvas, WIDTH, dateText, logo())
        // A short workout on a square card: centre the title, tiles and list in the spare room,
        // while the header and the bottom bar keep to the edges.
        canvas.save()
        canvas.translate(0f, plan.slack / 2f)
        titleLines.forEachIndexed { i, line ->
            canvas.drawText(line, MARGIN, firstTitleBaseline + i * TITLE_LINE, titlePaint)
        }

        val volumeDisplay = if (imperial) Units.kgToLb(volumeKg) else volumeKg
        val volumeText = "${compactNumber(volumeDisplay)} ${if (imperial) "lb" else "kg"}"
        val tiles = ShareCardStyle.tileRects(WIDTH, tilesTop, TILE_HEIGHT, 4)
        ShareCardStyle.drawTile(canvas, tiles[0], "VOLUME", volumeText)
        ShareCardStyle.drawTile(canvas, tiles[1], "SETS", setCount.toString())
        ShareCardStyle.drawTile(canvas, tiles[2], "TIME", formatDuration(durationSeconds))
        ShareCardStyle.drawTile(canvas, tiles[3], "PRS", prCount.toString(), accent = prCount > 0)

        drawExercises(canvas, lines, plan, headingBaseline, rowsTop)
        canvas.restore()
        ShareCardStyle.drawFooterBar(canvas, WIDTH, plan.cardHeight)
        return bitmap
    }

    private fun logo() = ContextCompat.getDrawable(appContext, R.drawable.ic_launcher_foreground)

    private fun drawExercises(
        canvas: Canvas,
        lines: List<ShareExerciseLine>,
        plan: ShareListPlan,
        headingBaseline: Float,
        rowsTop: Float,
    ) {
        val heading = ShareCardStyle.paint(ShareCardStyle.EMBER_TEXT, 28f).apply { letterSpacing = 0.12f }
        if (lines.isEmpty()) {
            canvas.drawText("SESSION COMPLETE", MARGIN, headingBaseline, heading)
            canvas.drawText(
                "Another one in the books.",
                MARGIN,
                headingBaseline + 64f,
                ShareCardStyle.paint(ShareCardStyle.TEXT_SECONDARY, 40f, bold = false),
            )
            return
        }
        canvas.drawText("EXERCISES", MARGIN, headingBaseline, heading)

        val name = ShareCardStyle.paint(ShareCardStyle.TEXT, 38f)
        val detail = ShareCardStyle.paint(ShareCardStyle.TEXT_SECONDARY, 36f).apply { textAlign = Paint.Align.RIGHT }
        val sets = ShareCardStyle.paint(ShareCardStyle.TEXT_TERTIARY, 30f, bold = false)
        val divider = ShareCardStyle.paint(ShareCardStyle.BORDER, 0f).apply { strokeWidth = 2f }
        val right = WIDTH - MARGIN

        var top = rowsTop
        lines.take(plan.shown).forEachIndexed { i, line ->
            val rowHeight = plan.rowHeights[i]
            val withSets = plan.expanded && line.workingSets.isNotEmpty()
            val baseline = if (withSets) top + 50f else top + COMPACT_ROW / 2f + 13f
            if (i > 0) canvas.drawLine(MARGIN, top, right, top, divider)
            val pillWidth = if (line.hasPr) PR_PILL_WIDTH + 16f else 0f
            val nameMax = right - MARGIN - detail.measureText(line.detail) - pillWidth - 32f
            val nameText = ShareCardStyle.ellipsize(line.name, name, nameMax)
            canvas.drawText(nameText, MARGIN, baseline, name)
            if (line.hasPr) drawPrPill(canvas, MARGIN + name.measureText(nameText) + 16f, baseline)
            canvas.drawText(line.detail, right, baseline, detail)
            if (withSets) {
                val setsText = ShareCardStyle.ellipsize(line.workingSets.joinToString("   "), sets, right - MARGIN)
                canvas.drawText(setsText, MARGIN, baseline + 46f, sets)
            }
            top += rowHeight
        }
        if (plan.overflow > 0) {
            val baseline = top + COMPACT_ROW / 2f + 13f
            canvas.drawText("+${plan.overflow} more", MARGIN, baseline, ShareCardStyle.paint(ShareCardStyle.TEXT_TERTIARY, 34f))
        }
    }

    private fun drawPrPill(canvas: Canvas, left: Float, baseline: Float) {
        val rect = RectF(left, baseline - 34f, left + PR_PILL_WIDTH, baseline + 6f)
        canvas.drawRoundRect(rect, 20f, 20f, ShareCardStyle.paint(ShareCardStyle.EMBER, 0f))
        val text = ShareCardStyle.paint(ShareCardStyle.ON_EMBER, 24f).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText("PR", rect.centerX(), baseline - 6f, text)
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

        /** Square for a short workout, up to Instagram's 4:5 portrait for a long one. */
        const val MIN_HEIGHT = 1080
        const val MAX_HEIGHT = 1350
        const val MARGIN = ShareCardStyle.MARGIN
        const val HEADER_BASELINE = ShareCardStyle.MARGIN + 37f
        const val TITLE_LINE = 86f
        const val TILE_HEIGHT = 156f
        const val COMPACT_ROW = 76f
        const val EXPANDED_ROW = 116f
        const val BOTTOM_SPACE = 84f
        const val PR_PILL_WIDTH = 60f
        val DATE_FORMAT = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
    }
}
