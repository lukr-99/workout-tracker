package com.lukr99.workout.web

import com.lukr99.workout.data.ShareListLayout
import com.lukr99.workout.data.ShareListPlan
import com.lukr99.workout.domain.Estimates
import com.lukr99.workout.domain.ShareExerciseLine
import com.lukr99.workout.domain.Units
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutShareSummary
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.web.platform.BrowserResourceFetcher
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Typeface

/** The finished card: the PNG file, and the same picture to show in the preview. */
internal class RenderedShareCard(val png: ByteArray, val image: Image)

/**
 * Draws the workout share card (a PNG, 1080 px wide, square up to 4:5) on a Skia surface, the same
 * layout and wording as Android's `WorkoutShareCardRenderer`: the Ember header, the workout name,
 * four stat tiles and the exercise list with PRs marked. The text uses the bundled Barlow files.
 * The list layout is the shared [ShareListLayout].
 */
internal object WebWorkoutShareCard {
    private var bold: Typeface? = null
    private var regular: Typeface? = null

    private suspend fun typefaces(): Pair<Typeface, Typeface> {
        val b = bold ?: load("barlow_bold").also { bold = it }
        val r = regular ?: load("barlow_medium").also { regular = it }
        return b to r
    }

    private suspend fun load(file: String): Typeface {
        val bytes = checkNotNull(BrowserResourceFetcher.bytes("fonts/$file.ttf")) { "Font $file could not be fetched" }
        return checkNotNull(FontMgr.default.makeFromData(Data.makeFromBytes(bytes))) { "Font $file could not be read" }
    }

    suspend fun render(session: WorkoutSession, imperial: Boolean): RenderedShareCard {
        val (boldFace, regularFace) = typefaces()
        val strength = session.entries.filter { it.isStrength }
        val volumeKg = strength.sumOf { Estimates.volume(it.strengthSets) }
        val setCount = strength.sumOf { it.strengthSets.size }
        val durationSeconds = session.durationSeconds.takeIf { it > 0 }
            ?: (((session.endedAtUtc ?: 0L) - session.startedAtUtc) / 1_000).coerceAtLeast(0)
        val prCount = strength.count { entry -> entry.strengthSets.any { it.isPr } }
        val dateText = Format.fullDate(session.completedDateUtc ?: session.startedAtUtc)
        val lines = WorkoutShareSummary.exerciseLines(session, imperial)

        val titleFont = Font(boldFace, 76f)
        val titleLines = wrap(session.name.ifBlank { "Workout" }, titleFont, WIDTH - 2 * MARGIN)
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

        val surface = Surface.makeRasterN32Premul(WIDTH, plan.cardHeight)
        val canvas = surface.canvas
        drawBackground(canvas, plan.cardHeight)
        drawHeader(canvas, dateText, boldFace)
        // A short workout on a square card: centre the title, tiles and list in the spare room,
        // while the header and the bottom bar keep to the edges.
        canvas.save()
        canvas.translate(0f, plan.slack / 2f)
        titleLines.forEachIndexed { i, line ->
            canvas.drawString(line, MARGIN, firstTitleBaseline + i * TITLE_LINE, titleFont, fill(TEXT))
        }
        val volumeDisplay = if (imperial) Units.kgToLb(volumeKg) else volumeKg
        val volumeText = "${ShareCardText.compactNumber(volumeDisplay)} ${if (imperial) "lb" else "kg"}"
        val tiles = tileRects(tilesTop)
        drawTile(canvas, tiles[0], "VOLUME", volumeText, boldFace)
        drawTile(canvas, tiles[1], "SETS", setCount.toString(), boldFace)
        drawTile(canvas, tiles[2], "TIME", ShareCardText.formatDuration(durationSeconds), boldFace)
        drawTile(canvas, tiles[3], "PRS", prCount.toString(), boldFace, accent = prCount > 0)
        drawExercises(canvas, lines, plan, headingBaseline, rowsTop, boldFace, regularFace)
        canvas.restore()
        drawFooterBar(canvas, plan.cardHeight)

        val image = surface.makeImageSnapshot()
        val png = checkNotNull(image.encodeToData(EncodedImageFormat.PNG)) { "The card could not be encoded" }.bytes
        return RenderedShareCard(png, image)
    }

    // --- Drawing ------------------------------------------------------------------------------

    private fun fill(color: Int) = Paint().apply { this.color = color; isAntiAlias = true }

    private fun stroke(color: Int, width: Float) = Paint().apply {
        this.color = color
        isAntiAlias = true
        mode = PaintMode.STROKE
        strokeWidth = width
    }

    /** Near-black background with a soft ember glow in the top corner. */
    private fun drawBackground(canvas: Canvas, height: Int) {
        canvas.clear(BG)
        val glow = Paint().apply {
            isAntiAlias = true
            shader = Shader.makeRadialGradient(WIDTH.toFloat(), 0f, WIDTH * 0.9f, intArrayOf(0x46F97316, 0x00F97316))
        }
        canvas.drawRect(Rect.makeWH(WIDTH.toFloat(), height.toFloat()), glow)
    }

    /** The Ember mark and wordmark on the left, the date on the right. */
    private fun drawHeader(canvas: Canvas, dateText: String, boldFace: Typeface) {
        val tile = Rect.makeXYWH(MARGIN, MARGIN - 6f, LOGO_SIZE, LOGO_SIZE)
        val baseline = (tile.top + tile.bottom) / 2f + 13f
        val radius = LOGO_SIZE * 0.26f
        canvas.drawRRect(RRect.makeComplexXYWH(tile.left, tile.top, LOGO_SIZE, LOGO_SIZE, floatArrayOf(radius)), fill(LOGO_BG))
        canvas.drawRRect(RRect.makeComplexXYWH(tile.left, tile.top, LOGO_SIZE, LOGO_SIZE, floatArrayOf(radius)), stroke(BORDER, 2f))
        canvas.drawCircle((tile.left + tile.right) / 2f, (tile.top + tile.bottom) / 2f, LOGO_SIZE / 5f, fill(EMBER))
        spaced(canvas, "EMBER", tile.right + 22f, baseline, Font(boldFace, 36f), fill(TEXT), 0.18f)
        val dateFont = Font(boldFace, 30f)
        val date = dateText.uppercase()
        val dateWidth = spacedWidth(date, dateFont, 0.06f)
        spaced(canvas, date, WIDTH - MARGIN - dateWidth, baseline, dateFont, fill(TEXT_TERTIARY), 0.06f)
    }

    private fun tileRects(top: Float): List<Rect> {
        val gap = 18f
        val width = (WIDTH - 2 * MARGIN - gap * 3) / 4
        return List(4) { i ->
            val left = MARGIN + i * (width + gap)
            Rect.makeXYWH(left, top, width, TILE_HEIGHT)
        }
    }

    /** A rounded stat tile: a small label over a large value that shrinks to fit the tile. */
    private fun drawTile(canvas: Canvas, rect: Rect, label: String, value: String, boldFace: Typeface, accent: Boolean = false) {
        val shape = RRect.makeXYWH(rect.left, rect.top, rect.width, rect.height, 28f)
        canvas.drawRRect(shape, fill(SURFACE))
        canvas.drawRRect(shape, stroke(BORDER, 2f))
        val pad = 28f
        spaced(canvas, label, rect.left + pad, rect.top + pad + 22f, Font(boldFace, 24f), fill(TEXT_TERTIARY), 0.1f)
        val valueFont = Font(boldFace, 56f)
        while (valueFont.size > 28f && valueFont.measureTextWidth(value) > rect.width - 2 * pad) valueFont.size -= 2f
        canvas.drawString(value, rect.left + pad, rect.bottom - pad - 4f, valueFont, fill(if (accent) EMBER_TEXT else TEXT))
    }

    private fun drawExercises(
        canvas: Canvas,
        lines: List<ShareExerciseLine>,
        plan: ShareListPlan,
        headingBaseline: Float,
        rowsTop: Float,
        boldFace: Typeface,
        regularFace: Typeface,
    ) {
        val heading = Font(boldFace, 28f)
        if (lines.isEmpty()) {
            spaced(canvas, "SESSION COMPLETE", MARGIN, headingBaseline, heading, fill(EMBER_TEXT), 0.12f)
            canvas.drawString("Another one in the books.", MARGIN, headingBaseline + 64f, Font(regularFace, 40f), fill(TEXT_SECONDARY))
            return
        }
        spaced(canvas, "EXERCISES", MARGIN, headingBaseline, heading, fill(EMBER_TEXT), 0.12f)

        val name = Font(boldFace, 38f)
        val detail = Font(boldFace, 36f)
        val sets = Font(regularFace, 30f)
        val right = WIDTH - MARGIN
        var top = rowsTop
        lines.take(plan.shown).forEachIndexed { i, line ->
            val withSets = plan.expanded && line.workingSets.isNotEmpty()
            val baseline = if (withSets) top + 50f else top + COMPACT_ROW / 2f + 13f
            if (i > 0) canvas.drawLine(MARGIN, top, right, top, stroke(BORDER, 2f))
            val pillWidth = if (line.hasPr) PR_PILL_WIDTH + 16f else 0f
            val detailWidth = detail.measureTextWidth(line.detail)
            val nameText = ellipsize(line.name, name, right - MARGIN - detailWidth - pillWidth - 32f)
            canvas.drawString(nameText, MARGIN, baseline, name, fill(TEXT))
            if (line.hasPr) drawPrPill(canvas, MARGIN + name.measureTextWidth(nameText) + 16f, baseline, boldFace)
            canvas.drawString(line.detail, right - detailWidth, baseline, detail, fill(TEXT_SECONDARY))
            if (withSets) {
                val setsText = ellipsize(line.workingSets.joinToString("   "), sets, right - MARGIN)
                canvas.drawString(setsText, MARGIN, baseline + 46f, sets, fill(TEXT_TERTIARY))
            }
            top += plan.rowHeights[i]
        }
        if (plan.overflow > 0) {
            val baseline = top + COMPACT_ROW / 2f + 13f
            canvas.drawString("+${plan.overflow} more", MARGIN, baseline, Font(boldFace, 34f), fill(TEXT_TERTIARY))
        }
    }

    private fun drawPrPill(canvas: Canvas, left: Float, baseline: Float, boldFace: Typeface) {
        canvas.drawRRect(RRect.makeXYWH(left, baseline - 34f, PR_PILL_WIDTH, 40f, 20f), fill(EMBER))
        val font = Font(boldFace, 24f)
        val width = font.measureTextWidth("PR")
        canvas.drawString("PR", left + (PR_PILL_WIDTH - width) / 2f, baseline - 6f, font, fill(ON_EMBER))
    }

    /** An ember gradient bar along the bottom edge. */
    private fun drawFooterBar(canvas: Canvas, height: Int) {
        val bar = Paint().apply {
            shader = Shader.makeLinearGradient(0f, 0f, WIDTH.toFloat(), 0f, intArrayOf(EMBER, EMBER_TEXT))
        }
        canvas.drawRect(Rect.makeLTRB(0f, height - 12f, WIDTH.toFloat(), height.toFloat()), bar)
    }

    // --- Text ---------------------------------------------------------------------------------

    private fun spacedWidth(text: String, font: Font, spacing: Float): Float =
        text.sumOf { font.measureTextWidth(it.toString()).toDouble() }.toFloat() + text.length * spacing * font.size

    /** Draws [text] letter by letter with [spacing] (in em) between letters, as Android's letterSpacing does. */
    private fun spaced(canvas: Canvas, text: String, x: Float, baseline: Float, font: Font, paint: Paint, spacing: Float) {
        var cursor = x
        for (char in text) {
            val glyph = char.toString()
            canvas.drawString(glyph, cursor, baseline, font, paint)
            cursor += font.measureTextWidth(glyph) + spacing * font.size
        }
    }

    /** Cuts [text] to one line that fits [maxWidth], ending in an ellipsis when it had to cut. */
    internal fun ellipsize(text: String, font: Font, maxWidth: Float): String {
        if (font.measureTextWidth(text) <= maxWidth) return text
        var trimmed = text
        while (trimmed.isNotEmpty() && font.measureTextWidth("${trimmed.trimEnd()}…") > maxWidth) {
            trimmed = trimmed.dropLast(1)
        }
        return "${trimmed.trimEnd()}…"
    }

    /** Wraps [text] onto at most [maxLines] lines that fit [maxWidth], ellipsizing the last one. */
    private fun wrap(text: String, font: Font, maxWidth: Float, maxLines: Int = 2): List<String> {
        val lines = mutableListOf<String>()
        var current = ""
        val words = text.split(' ').filter { it.isNotEmpty() }
        for ((index, word) in words.withIndex()) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (font.measureTextWidth(candidate) <= maxWidth || current.isEmpty()) {
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
        return lines.take(maxLines).map { ellipsize(it, font, maxWidth) }
    }

    private const val WIDTH = 1080
    private const val MIN_HEIGHT = 1080
    private const val MAX_HEIGHT = 1350
    private const val MARGIN = 72f
    private const val LOGO_SIZE = 60f
    private const val HEADER_BASELINE = MARGIN + 37f
    private const val TITLE_LINE = 86f
    private const val TILE_HEIGHT = 156f
    private const val COMPACT_ROW = 76f
    private const val EXPANDED_ROW = 116f
    private const val BOTTOM_SPACE = 84f
    private const val PR_PILL_WIDTH = 60f

    private const val BG = 0xFF0F0D0C.toInt()
    private const val SURFACE = 0xFF1A1715.toInt()
    private const val BORDER = 0xFF342E2A.toInt()
    private const val TEXT = 0xFFF6F2ED.toInt()
    private const val TEXT_SECONDARY = 0xFFBDB3A9.toInt()
    private const val TEXT_TERTIARY = 0xFF9A9087.toInt()
    private const val EMBER = 0xFFF97316.toInt()
    private const val EMBER_TEXT = 0xFFFF8A3D.toInt()
    private const val ON_EMBER = 0xFF1C0B00.toInt()
    private const val LOGO_BG = 0xFF0B0D10.toInt()
}
