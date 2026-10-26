package com.lukr99.workout.data.images

import java.text.Normalizer

/**
 * Removes accents and other combining marks, so "Pres" with an acute e matches plain "Pres", for
 * matching exercise names. Each platform supplies this file; the web edition's is under web/.
 */
internal fun stripAccents(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
