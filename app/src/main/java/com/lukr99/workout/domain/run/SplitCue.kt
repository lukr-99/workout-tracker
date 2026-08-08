package com.lukr99.workout.domain.run

import kotlin.math.floor

/**
 * Pure "a split just crossed" trigger for the live audio/haptic cues — the isolated, unit-tested
 * heart of R5's cue system. No Android, no timers: given the run's previous and current cumulative
 * distance (metres) and the split length (a km or a mile, so the trigger is **unit-aware**), it
 * reports exactly which whole-split marks the runner crossed on this step, in order. The Android
 * layer ([com.lukr99.workout.data.run.RunCues]) just turns each returned mark into speech + a buzz.
 *
 * Deliberately stateless — the caller holds the "distance already announced through" and feeds the
 * pair `(prev, curr)`; a mark at exactly `curr` counts as crossed, a mark at exactly `prev` does not
 * (half-open `(prev, curr]`), so replaying `(0,1000),(1000,2000)` announces km 1 then km 2 with no
 * double-count and no gap.
 */
object SplitCue {

    /**
     * The 1-based indices of every full-split boundary lying in `(prevMeters, currMeters]`. Usually
     * empty or a single element; more only if a single GPS step jumped several splits at once. Returns
     * empty for non-positive [splitMeters] or a non-advancing/backward step.
     */
    fun crossedMarks(prevMeters: Double, currMeters: Double, splitMeters: Double): List<Int> {
        if (splitMeters <= 0.0 || currMeters <= prevMeters) return emptyList()
        // First whole mark strictly greater than prev, then every mark up to and including curr.
        val firstIndex = floor(prevMeters / splitMeters).toInt() + 1
        val lastIndex = floor(currMeters / splitMeters + EPS).toInt()
        if (lastIndex < firstIndex) return emptyList()
        return (firstIndex..lastIndex).toList()
    }

    /**
     * Spoken text for crossing split [index], unit-aware. Announces the distance reached and the
     * average pace to that point (e.g. "Mile 3. 8 minutes 22 seconds per mile."). [avgPaceSecPerUnit]
     * is seconds per the display unit (per km or per mile); a non-positive pace is omitted.
     */
    fun spokenMessage(index: Int, imperial: Boolean, avgPaceSecPerUnit: Double): String {
        val unit = if (imperial) "mile" else "kilometre"
        val unitPlural = if (imperial) "miles" else "kilometres"
        val distancePart = if (index == 1) "1 $unit" else "$index $unitPlural"
        val pacePart = spokenPace(avgPaceSecPerUnit, imperial)
        return if (pacePart == null) "$distancePart." else "$distancePart. $pacePart."
    }

    /** "8 minutes 22 seconds per mile", or null when the pace is non-positive/not finite. */
    private fun spokenPace(secPerUnit: Double, imperial: Boolean): String? {
        if (secPerUnit <= 0.0 || !secPerUnit.isFinite()) return null
        val total = secPerUnit.toInt()
        val minutes = total / 60
        val seconds = total % 60
        val unit = if (imperial) "mile" else "kilometre"
        val minPart = when (minutes) {
            0 -> ""
            1 -> "1 minute"
            else -> "$minutes minutes"
        }
        val secPart = when (seconds) {
            0 -> ""
            1 -> "1 second"
            else -> "$seconds seconds"
        }
        val body = listOf(minPart, secPart).filter { it.isNotEmpty() }.joinToString(" ")
        return "$body per $unit".takeIf { body.isNotEmpty() }
    }

    private const val EPS = 1e-6
}
