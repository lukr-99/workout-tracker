package com.lukr99.workout.data

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

/**
 * The shared look of the workout and run share images: the dark Ember palette, a warm glow, the
 * brand header, stat tiles and the ember bar along the bottom. Both renderers draw through this so
 * the two cards stay consistent with each other and with the app.
 */
object ShareCardStyle {
    const val MARGIN = 72f

    val BG = Color.rgb(0x0F, 0x0D, 0x0C)
    val SURFACE = Color.rgb(0x1A, 0x17, 0x15)
    val BORDER = Color.rgb(0x34, 0x2E, 0x2A)
    val TEXT = Color.rgb(0xF6, 0xF2, 0xED)
    val TEXT_SECONDARY = Color.rgb(0xBD, 0xB3, 0xA9)
    val TEXT_TERTIARY = Color.rgb(0x9A, 0x90, 0x87)
    val EMBER = Color.rgb(0xF9, 0x73, 0x16)
    val EMBER_TEXT = Color.rgb(0xFF, 0x8A, 0x3D)
    val ON_EMBER = Color.rgb(0x1C, 0x0B, 0x00)

    fun paint(color: Int, size: Float, bold: Boolean = true) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    /** Near-black background with a soft ember glow in the top corner. */
    fun drawBackground(canvas: Canvas, width: Int, height: Int) {
        canvas.drawColor(BG)
        val radius = width * 0.9f
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width.toFloat(), 0f, radius,
                Color.argb(70, 0xF9, 0x73, 0x16), Color.TRANSPARENT, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glow)
    }

    /** Brand mark on the left and [dateText] on the right, on one baseline. Returns the baseline. */
    fun drawHeader(canvas: Canvas, width: Int, dateText: String): Float {
        val baseline = MARGIN + 40f
        canvas.drawCircle(MARGIN + 13f, baseline - 13f, 13f, paint(EMBER, 0f))
        canvas.drawText("EMBER", MARGIN + 40f, baseline, paint(TEXT, 36f).apply { letterSpacing = 0.18f })
        val date = paint(TEXT_TERTIARY, 30f).apply { textAlign = Paint.Align.RIGHT; letterSpacing = 0.06f }
        canvas.drawText(dateText.uppercase(), width - MARGIN, baseline, date)
        return baseline
    }

    /** A rounded stat tile: a small label over a large value that shrinks to fit the tile. */
    fun drawTile(canvas: Canvas, rect: RectF, label: String, value: String, accent: Boolean = false) {
        canvas.drawRoundRect(rect, 28f, 28f, paint(SURFACE, 0f))
        canvas.drawRoundRect(
            rect, 28f, 28f,
            paint(BORDER, 0f).apply { style = Paint.Style.STROKE; strokeWidth = 2f },
        )
        val pad = 28f
        val labelPaint = paint(TEXT_TERTIARY, 24f).apply { letterSpacing = 0.1f }
        canvas.drawText(label, rect.left + pad, rect.top + pad + 22f, labelPaint)
        val valuePaint = paint(if (accent) EMBER_TEXT else TEXT, 56f)
        fitTextSize(value, valuePaint, rect.width() - 2 * pad)
        canvas.drawText(value, rect.left + pad, rect.bottom - pad - 4f, valuePaint)
    }

    /** Lays [count] tiles side by side between the margins, [height] tall, from [top]. */
    fun tileRects(width: Int, top: Float, height: Float, count: Int, gap: Float = 18f): List<RectF> {
        val tileWidth = (width - 2 * MARGIN - gap * (count - 1)) / count
        return List(count) { i ->
            val left = MARGIN + i * (tileWidth + gap)
            RectF(left, top, left + tileWidth, top + height)
        }
    }

    /** An ember gradient bar along the bottom edge. */
    fun drawFooterBar(canvas: Canvas, width: Int, height: Int) {
        val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, width.toFloat(), 0f, EMBER, EMBER_TEXT, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, height - 12f, width.toFloat(), height.toFloat(), bar)
    }

    /** Shrinks [paint]'s text size until [text] fits [maxWidth], down to half its size. */
    fun fitTextSize(text: String, paint: Paint, maxWidth: Float) {
        val floor = paint.textSize / 2f
        while (paint.textSize > floor && paint.measureText(text) > maxWidth) paint.textSize -= 2f
    }

    /** Cuts [text] to one line that fits [maxWidth], ending in an ellipsis when it had to cut. */
    fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var trimmed = text
        while (trimmed.isNotEmpty() && paint.measureText("${trimmed.trimEnd()}…") > maxWidth) {
            trimmed = trimmed.dropLast(1)
        }
        return "${trimmed.trimEnd()}…"
    }

    /** Wraps [text] onto at most [maxLines] lines that fit [maxWidth], ellipsizing the last one. */
    fun wrap(text: String, paint: Paint, maxWidth: Float, maxLines: Int = 2): List<String> {
        val lines = mutableListOf<String>()
        var current = ""
        val words = text.split(' ').filter { it.isNotEmpty() }
        for ((index, word) in words.withIndex()) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current = candidate
                continue
            }
            if (lines.size == maxLines - 1) {
                current = words.drop(index).joinToString(" ", prefix = "$current ")
                break
            }
            lines += current
            current = word
        }
        if (current.isNotEmpty()) lines += current
        return lines.take(maxLines).map { ellipsize(it, paint, maxWidth) }
    }
}
