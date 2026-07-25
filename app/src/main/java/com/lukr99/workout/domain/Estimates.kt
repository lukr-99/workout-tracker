package com.lukr99.workout.domain

/**
 * Strength estimates — e1RM, volume, tonnage. Pure math over the domain models.
 *
 * These are new in the rework (the MAUI POC stored raw sets only). Formulas are the standard
 * one-rep-max estimators; both are exposed so the UI can pick, and a `best` helper takes the max
 * across a set list.
 */
object Estimates {

    /** Epley: `w · (1 + reps/30)`. Reps ≤ 0 → 0; a single rep returns the load itself. */
    fun epley(weightKg: Double, reps: Int): Double =
        if (reps <= 0) 0.0 else if (reps == 1) weightKg else weightKg * (1.0 + reps / 30.0)

    /** Brzycki: `w · 36 / (37 − reps)`. Valid below 37 reps; clamps out-of-range to 0. */
    fun brzycki(weightKg: Double, reps: Int): Double =
        if (reps <= 0 || reps >= 37) 0.0 else if (reps == 1) weightKg else weightKg * 36.0 / (37.0 - reps)

    /** Best estimated 1RM across a set list (Epley by default), ignoring warmups. */
    fun bestEpley(sets: List<StrengthSet>): Double =
        sets.filter { !it.isWarmup && it.reps > 0 }
            .maxOfOrNull { epley(it.weightKg, it.reps) } ?: 0.0

    /** Volume (tonnage) = Σ weight · reps. Warmups included unless [includeWarmups] is false. */
    fun volume(sets: List<StrengthSet>, includeWarmups: Boolean = true): Double =
        sets.filter { includeWarmups || !it.isWarmup }.sumOf { it.weightKg * it.reps }

    /** Total reps across a set list. */
    fun totalReps(sets: List<StrengthSet>, includeWarmups: Boolean = true): Int =
        sets.filter { includeWarmups || !it.isWarmup }.sumOf { it.reps }
}
