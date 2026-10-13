package com.lukr99.workout.data.run

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.lukr99.workout.R
import com.lukr99.workout.data.ShareCardStyle
import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.Run
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a run to a shareable square image: the Ember logo header, the run's **trace** drawn as a
 * glowing ember line with direction arrows, start and finish markers (one combined marker when a
 * loop ends where it began), and distance, time and pace tiles beneath. Hands back a
 * `FileProvider`-backed `ACTION_SEND` intent (same SAF/authority pattern as
 * [com.lukr99.workout.data.transfer.AndroidDocumentGateway]). Shares its look with the workout card
 * through [ShareCardStyle]; the trace math lives in the pure [TraceGeometry].
 */
class ShareCardRenderer(
    context: Context,
    private val fileProviderAuthority: String = "${context.packageName}.files",
) {
    private val appContext = context.applicationContext

    suspend fun shareIntent(run: Run, imperial: Boolean): Intent = withContext(Dispatchers.IO) {
        val bitmap = render(run, imperial)
        val dir = File(appContext.cacheDir, "shared-exports").apply { mkdirs() }
        val file = File(dir, "run-${run.id}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val uri = FileProvider.getUriForFile(appContext, fileProviderAuthority, file)
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    internal fun render(run: Run, imperial: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        ShareCardStyle.drawBackground(canvas, SIZE, SIZE)

        val logo = ContextCompat.getDrawable(appContext, R.drawable.ic_launcher_foreground)
        val headerBaseline = ShareCardStyle.drawHeader(canvas, SIZE, DATE_FORMAT.format(Date(run.startedAtUtc)), logo)
        val titleBaseline = headerBaseline + 104f
        canvas.drawText("Run", MARGIN, titleBaseline, ShareCardStyle.paint(ShareCardStyle.TEXT, 72f))

        val tilesTop = SIZE - ShareCardStyle.MARGIN - TILE_HEIGHT
        drawTrace(canvas, run, top = titleBaseline + 48f, bottom = tilesTop - 48f)
        drawStats(canvas, run, imperial, tilesTop)
        ShareCardStyle.drawFooterBar(canvas, SIZE, SIZE)
        return bitmap
    }

    private fun drawTrace(canvas: Canvas, run: Run, top: Float, bottom: Float) {
        val lines = TraceGeometry.project(run.trace, MARGIN, top, SIZE - 2 * MARGIN, bottom - top)
        if (lines.isEmpty()) return

        // Break the drawn line at manual-pause boundaries (moveTo, not lineTo) so a paused-and-walked
        // stretch shows as a gap.
        val path = Path()
        lines.forEach { line ->
            line.forEachIndexed { i, (x, y) -> if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
        }
        canvas.drawPath(path, stroke(Color.argb(60, 0xF9, 0x73, 0x16), 30f))
        canvas.drawPath(path, stroke(ShareCardStyle.EMBER, 11f))

        // Arrows show which way the route goes, which a loop cannot show on its own.
        ARROW_FRACTIONS.forEach { fraction -> TraceGeometry.poseAlong(lines, fraction)?.let { drawArrow(canvas, it) } }

        val start = lines.first().first()
        val finish = lines.last().last()
        if (TraceGeometry.startMeetsFinish(start, finish, MARKER_MERGE_DISTANCE)) {
            drawStartFinish(canvas, start.first, start.second)
        } else {
            drawStart(canvas, start.first, start.second)
            drawFinish(canvas, finish.first, finish.second)
        }
    }

    private fun drawArrow(canvas: Canvas, pose: TracePose) {
        val arrow = Path().apply {
            moveTo(14f, 0f)
            lineTo(-10f, -13f)
            lineTo(-4f, 0f)
            lineTo(-10f, 13f)
            close()
        }
        val save = canvas.save()
        canvas.translate(pose.x, pose.y)
        canvas.rotate(pose.angleDegrees)
        canvas.drawPath(arrow, stroke(ShareCardStyle.BG, 6f))
        canvas.drawPath(arrow, ShareCardStyle.paint(ShareCardStyle.TEXT, 0f))
        canvas.restoreToCount(save)
    }

    /** Start: a light dot ringed in the background colour. */
    private fun drawStart(canvas: Canvas, x: Float, y: Float) {
        canvas.drawCircle(x, y, 20f, ShareCardStyle.paint(ShareCardStyle.BG, 0f))
        canvas.drawCircle(x, y, 14f, ShareCardStyle.paint(ShareCardStyle.TEXT, 0f))
    }

    /** Finish: an ember dot ringed in the background colour. */
    private fun drawFinish(canvas: Canvas, x: Float, y: Float) {
        canvas.drawCircle(x, y, 20f, ShareCardStyle.paint(ShareCardStyle.BG, 0f))
        canvas.drawCircle(x, y, 14f, ShareCardStyle.paint(ShareCardStyle.EMBER_TEXT, 0f))
    }

    /** Start and finish in one place: the start dot inside an ember finish ring. */
    private fun drawStartFinish(canvas: Canvas, x: Float, y: Float) {
        canvas.drawCircle(x, y, 27f, ShareCardStyle.paint(ShareCardStyle.BG, 0f))
        canvas.drawCircle(x, y, 21f, ShareCardStyle.paint(ShareCardStyle.EMBER_TEXT, 0f))
        canvas.drawCircle(x, y, 14f, ShareCardStyle.paint(ShareCardStyle.BG, 0f))
        canvas.drawCircle(x, y, 9f, ShareCardStyle.paint(ShareCardStyle.TEXT, 0f))
    }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun drawStats(canvas: Canvas, run: Run, imperial: Boolean, top: Float) {
        val distText = if (imperial) "%.2f mi".format(run.distanceMeters / Pace.METERS_PER_MILE)
        else "%.2f km".format(run.distanceMeters / Pace.METERS_PER_KM)
        val paceSec = if (imperial) Pace.paceSecPerMile(run.avgPaceSecPerKm) else run.avgPaceSecPerKm
        val paceText = "${Pace.formatPace(paceSec)} /${if (imperial) "mi" else "km"}"
        val timeText = Pace.formatDuration(run.movingSeconds)

        val tiles = ShareCardStyle.tileRects(SIZE, top, TILE_HEIGHT, 3)
        ShareCardStyle.drawTile(canvas, tiles[0], "DISTANCE", distText, accent = true)
        ShareCardStyle.drawTile(canvas, tiles[1], "TIME", timeText)
        ShareCardStyle.drawTile(canvas, tiles[2], "PACE", paceText)
    }

    private companion object {
        const val SIZE = 1080
        const val MARGIN = ShareCardStyle.MARGIN
        const val TILE_HEIGHT = 156f

        /** Start and finish closer than this, in pixels, share one marker. */
        const val MARKER_MERGE_DISTANCE = 48f
        val ARROW_FRACTIONS = listOf(0.25f, 0.5f, 0.75f)
        val DATE_FORMAT = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
    }
}
