package com.lukr99.workout.domain

/**
 * Pure Kotlin unit helpers. **No Android imports** — `domain/` stays portable and unit-testable in
 * isolation (this is what could later be lifted into a shared/desktop module). See 01-architecture.md.
 *
 * Phase 0 seeds this with the kg⇄lb conversion so the domain layer has a real, tested function;
 * the fuller analytics/estimates land in Phase 1.
 */
object Units {
    const val LB_PER_KG: Double = 2.2046226218
    const val KM_PER_MILE: Double = 1.609344

    /** Kilograms → pounds. */
    fun kgToLb(kg: Double): Double = kg * LB_PER_KG

    /** Pounds → kilograms. */
    fun lbToKg(lb: Double): Double = lb / LB_PER_KG

    /** Kilometres → miles. */
    fun kmToMiles(km: Double): Double = km / KM_PER_MILE

    /** Miles → kilometres. */
    fun milesToKm(miles: Double): Double = miles * KM_PER_MILE

    /**
     * Display-round a weight to one decimal, dropping a trailing `.0` ("Store raw, derive stats":
     * weights are stored unrounded and rounded only for display — 03-data-model.md).
     */
    fun formatWeight(kg: Double): String {
        val rounded = Math.round(kg * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }

    /** Seconds → `H:MM:SS` / `M:SS` clock string. */
    fun formatDuration(totalSeconds: Long): String {
        val s = totalSeconds.coerceAtLeast(0)
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val seconds = s % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }
}
