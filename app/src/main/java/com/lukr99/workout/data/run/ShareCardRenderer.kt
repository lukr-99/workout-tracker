package com.lukr99.workout.data.run

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.Run
import java.io.File
import kotlin.math.cos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a run to a shareable square image — the run's **trace** drawn as an ember line on the
 * near-black brand background, with distance / time / pace beneath — and hands back a
 * `FileProvider`-backed `ACTION_SEND` intent (same SAF/authority pattern as
 * [com.lukr99.workout.data.transfer.AndroidDocumentGateway]). Kept out of `domain/` because it needs
 * Android's `Canvas`; the geometry (haversine, pace) reuses the pure [Pace] math.
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

    private fun render(run: Run, imperial: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BG)

        drawTrace(canvas, run)
        drawStats(canvas, run, imperial)
        return bitmap
    }

    private fun drawTrace(canvas: Canvas, run: Run) {
        val pts = run.trace.map { it.lat to it.lon }
        if (pts.size < 2) return

        val lat0 = pts.map { it.first }.average()
        val mPerLon = 111_320.0 * cos(Math.toRadians(lat0))
        // Project to metres relative to the first point (y up-positive → flip when drawing).
        val xy = pts.map { (la, lo) ->
            (lo - pts[0].second) * mPerLon to (la - pts[0].first) * 110_540.0
        }
        val minX = xy.minOf { it.first }; val maxX = xy.maxOf { it.first }
        val minY = xy.minOf { it.second }; val maxY = xy.maxOf { it.second }
        val spanX = (maxX - minX).coerceAtLeast(1.0)
        val spanY = (maxY - minY).coerceAtLeast(1.0)

        val area = SIZE - 2 * MARGIN
        val top = MARGIN.toFloat()
        val scale = minOf(area / spanX, (area * 0.72) / spanY) // leave room for stats at the bottom
        val drawW = (spanX * scale).toFloat()
        val drawH = (spanY * scale).toFloat()
        val offX = MARGIN + (area - drawW) / 2f
        val offY = top + (area * 0.72f - drawH) / 2f

        val path = Path()
        xy.forEachIndexed { i, (x, y) ->
            val px = offX + ((x - minX) * scale).toFloat()
            val py = offY + drawH - ((y - minY) * scale).toFloat() // flip Y
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = EMBER
            style = Paint.Style.STROKE
            strokeWidth = 12f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, line)
    }

    private fun drawStats(canvas: Canvas, run: Run, imperial: Boolean) {
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MUTED
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val value = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 62f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val brand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = EMBER
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val distText = if (imperial) "%.2f mi".format(run.distanceMeters / Pace.METERS_PER_MILE)
        else "%.2f km".format(run.distanceMeters / Pace.METERS_PER_KM)
        val paceSec = if (imperial) Pace.paceSecPerMile(run.avgPaceSecPerKm) else run.avgPaceSecPerKm
        val paceText = "${Pace.formatPace(paceSec)} /${if (imperial) "mi" else "km"}"
        val timeText = Pace.formatDuration(run.movingSeconds)

        val baseY = SIZE - 150f
        val col = (SIZE - 2 * MARGIN) / 3f
        drawStat(canvas, MARGIN.toFloat(), baseY, "DISTANCE", distText, label, value)
        drawStat(canvas, MARGIN + col, baseY, "TIME", timeText, label, value)
        drawStat(canvas, MARGIN + 2 * col, baseY, "PACE", paceText, label, value)

        canvas.drawText("EMBER", MARGIN.toFloat(), MARGIN + 40f, brand)
    }

    private fun drawStat(
        canvas: Canvas,
        x: Float,
        y: Float,
        label: String,
        value: String,
        labelPaint: Paint,
        valuePaint: Paint,
    ) {
        canvas.drawText(label, x, y, labelPaint)
        canvas.drawText(value, x, y + 66f, valuePaint)
    }

    private companion object {
        const val SIZE = 1080
        const val MARGIN = 72
        val BG = Color.rgb(0x14, 0x12, 0x10)
        val EMBER = Color.rgb(0xE8, 0x62, 0x2C)
        val MUTED = Color.rgb(0x9A, 0x92, 0x8A)
    }
}
