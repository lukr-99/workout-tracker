package com.lukr99.workout.web

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface

/**
 * Decodes stored photos for drawing. A thumbnail never needs the 1280 px original, so each photo is
 * decoded once and kept at [DRAW_EDGE] pixels on its long side; the cache holds the last [LIMIT]
 * photos so a long list does not fill the phone's memory.
 */
internal object WebPhotoImages {
    private const val DRAW_EDGE = 640
    private const val LIMIT = 48
    private val cache = LinkedHashMap<String, ImageBitmap>()

    /** The photo ready to draw, or null when its bytes are not an image the browser can decode. */
    fun bitmap(photo: WebPhotoModel): ImageBitmap? {
        cache.remove(photo.key)?.let { cache[photo.key] = it; return it }
        val decoded = decode(photo.bytes) ?: return null
        cache[photo.key] = decoded
        while (cache.size > LIMIT) cache.remove(cache.keys.first())
        return decoded
    }

    private fun decode(bytes: ByteArray): ImageBitmap? = runCatching {
        val source = Image.makeFromEncoded(bytes)
        try {
            val scale = minOf(1f, DRAW_EDGE.toFloat() / maxOf(source.width, source.height))
            val width = (source.width * scale).toInt().coerceAtLeast(1)
            val height = (source.height * scale).toInt().coerceAtLeast(1)
            if (scale >= 1f) {
                source.toComposeImageBitmap()
            } else {
                val surface = Surface.makeRasterN32Premul(width, height)
                surface.canvas.drawImageRect(
                    source,
                    Rect.makeWH(source.width.toFloat(), source.height.toFloat()),
                    Rect.makeWH(width.toFloat(), height.toFloat()),
                    SamplingMode.LINEAR,
                    null,
                    true,
                )
                surface.makeImageSnapshot().toComposeImageBitmap()
            }
        } finally {
            source.close()
        }
    }.getOrNull()
}
