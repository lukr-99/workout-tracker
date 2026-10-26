package com.lukr99.workout.web

import org.jetbrains.skia.Image

/** The finished card: the PNG file, and the same picture to show in the preview. */
internal class RenderedShareCard(val png: ByteArray, val image: Image)
