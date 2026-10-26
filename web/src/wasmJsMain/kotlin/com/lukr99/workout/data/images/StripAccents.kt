package com.lukr99.workout.data.images

import com.lukr99.workout.web.platform.removeCombiningMarks

/**
 * Removes accents and other combining marks, so an acute e matches a plain e, for matching exercise
 * names. Each platform supplies this file; the web edition's does the same as Android (decompose,
 * then drop the marks) with the browser's `String.normalize`.
 */
internal fun stripAccents(value: String): String = removeCombiningMarks(value)
