package com.lukr99.workout.web

import kotlin.math.roundToLong

/** The number and time wording on the workout share card, the same as the Android card. */
internal object ShareCardText {
    /** 12,345 as "12,345"; 12,345 over ten thousand as "12.3k"; over a hundred thousand as "123k". */
    fun compactNumber(value: Double): String = when {
        value >= 100_000 -> "${(value / 1_000).roundToLong()}k"
        value >= 10_000 -> {
            val tenths = (value / 100).roundToLong()
            "${tenths / 10}.${tenths % 10}k"
        }
        else -> grouped(value.roundToLong())
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3_600
        val minutes = (seconds % 3_600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private fun grouped(value: Long): String {
        val digits = value.toString().removePrefix("-")
        val withCommas = digits.reversed().chunked(3).joinToString(",").reversed()
        return if (value < 0) "-$withCommas" else withCommas
    }
}
