package com.lukr99.workout.data.run

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.core.content.FileProvider
import com.lukr99.workout.data.ShareCardStyle
import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.RunTrace
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a run to a shareable square image: the brand header, the run's **trace** drawn as a
 * glowing ember line with start and finish dots, and distance, time and pace tiles beneath. Hands
 * back a `FileProvider`-backed `ACTION_SEND` intent (same SAF/authority pattern as
 * [com.lukr99.workout.data.transfer.AndroidDocumentGateway]). Shares its look with the workout card
 * through [ShareCardStyle]. Kept out of `domain/` because it needs Android's `Canvas`; the pace math
 * reuses the pure [Pace].
 *
 * The trace is projected with a local equirectangular fit (accurate at run scale) and letterboxed to
 * preserve aspect, so the shape reads true rather than stretched.
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

        val headerBaseline = ShareCardStyle.drawHeader(canvas, SIZE, DATE_FORMAT.format(Date(run.startedAtUtc)))
        val titleBaseline = headerBaseline + 104f
        canvas.drawText("Run", MARGIN, titleBaseline, ShareCardStyle.paint(ShareCardStyle.TEXT, 72f))

        val tilesTop = SIZE - ShareCardStyle.MARGIN - TILE_HEIGHT
        drawTrace(canvas, run, top = titleBaseline + 48f, bottom = tilesTop - 48f)
        drawStats(canvas, run, imperial, tilesTop)
        ShareCardStyle.drawFooterBar(canvas, SIZE, SIZE)
        return bitmap
    }

    private fun drawTrace(canvas: Canvas, run: Run, top: Float, bottom: Float) {
        val pts = run.trace.map { it.lat to it.lon }
        if (pts.size < 2) return

        val lat0 = pts.map { it.first }.average()
        val mPerLon = 111_320.0 * cos(Math.toRadians(lat0))
        // Project to metres relative to the first point (y up-positive, flipped when drawing).
        val xy = pts.map { (la, lo) ->
            (lo - pts[0].second) * mPerLon to (la - pts[0].first) * 110_540.0
        }
        val minX = xy.minOf { it.first }; val maxX = xy.maxOf { it.first }
        val minY = xy.minOf { it.second }; val maxY = xy.maxOf { it.second }
        val spanX = (maxX - minX).coerceAtLeast(1.0)
        val spanY = (maxY - minY).coerceAtLeast(1.0)

        val areaW = SIZE - 2 * MARGIN
        val areaH = bottom - top
        val scale = minOf(areaW / spanX, areaH / spanY)
        val drawW = (spanX * scale).toFloat()
        val drawH = (spanY * scale).toFloat()
        val offX = MARGIN + (areaW - drawW) / 2f
        val offY = top + (areaH - drawH) / 2f

        fun project(lat: Double, lon: Double): Pair<Float, Float> {
            val x = (lon - pts[0].second) * mPerLon
            val y = (lat - pts[0].first) * 110_540.0
            return (offX + ((x - minX) * scale).toFloat()) to (offY + drawH - ((y - minY) * scale).toFloat())
        }

        // Break the drawn line at manual-pause boundaries (moveTo, not lineTo) so a paused-and-walked
        // stretch shows as a gap. The same projection is shared so every segment lines up.
        val path = Path()
        RunTrace.segments(run.trace).forEach { segment ->
            segment.forEachIndexed { i, tp ->
                val (px, py) = project(tp.lat, tp.lon)
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
        }
        val glow = stroke(Color.argb(60, 0xF9, 0x73, 0x16), 30f)
        canvas.drawPath(path, glow)
        canvas.drawPath(path, stroke(ShareCardStyle.EMBER, 11f))

        // Start as a light dot, finish as an ember dot with a dark ring.
        val (sx, sy) = project(run.trace.first().lat, run.trace.first().lon)
        val (ex, ey) = project(run.trace.last().lat, run.trace.last().lon)
        canvas.drawCircle(sx, sy, 14f, ShareCardStyle.paint(ShareCardStyle.TEXT, 0f))
        canvas.drawCircle(ex, ey, 20f, ShareCardStyle.paint(ShareCardStyle.BG, 0f))
        canvas.drawCircle(ex, ey, 14f, ShareCardStyle.paint(ShareCardStyle.EMBER_TEXT, 0f))
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
        val DATE_FORMAT = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
    }
}
