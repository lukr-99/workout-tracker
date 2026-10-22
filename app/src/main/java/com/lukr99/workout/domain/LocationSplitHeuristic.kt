package com.lukr99.workout.domain

/**
 * Decides when to ask "Track this exercise separately per gym?".
 *
 * For each location, take the top working weight of each of the newest [recentWorkouts] workouts
 * that logged the exercise there, and use their median. Ask when the median here and the median at
 * some other location differ by at least [minRatio] (the larger over the smaller), and each side
 * has enough workouts: [minWorkoutsHere] here (the live workout counts, by its sets marked done)
 * and [minWorkoutsElsewhere] at the other place. Warm-ups, sets without reps and sets without
 * weight are ignored. Workouts with no location never take part, because they could be from
 * anywhere.
 *
 * With the defaults it asks once one gym's numbers are 40% away from the median of another gym's
 * recent workouts (two or more). A normal progression step is a few percent, so ordinary training
 * does not trigger it. Tune the numbers here; the tests pin the edges.
 */
data class LocationSplitHeuristic(
    val minRatio: Double = 1.4,
    val minWorkoutsHere: Int = 1,
    val minWorkoutsElsewhere: Int = 2,
    val recentWorkouts: Int = 5,
) {
    /**
     * A suggestion for [exerciseId] at [locationId] from [sessions] (finished workouts and the live
     * one), or null when the numbers are close or there is too little to compare. Discarded
     * workouts are ignored. When several other places qualify, the furthest one is returned.
     */
    fun evaluate(sessions: List<WorkoutSession>, exerciseId: String, locationId: String?): LocationSplitSuggestion? {
        if (locationId == null || exerciseId.isBlank()) return null
        val topsByLocation = sessions.asSequence()
            .filter { it.status != WorkoutSessionStatus.Discarded }
            .sortedByDescending { it.completedDateUtc ?: it.startedAtUtc }
            .mapNotNull { session ->
                val place = session.locationId ?: return@mapNotNull null
                topWorkingKg(session, exerciseId)?.let { place to it }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, tops) -> tops.take(recentWorkouts) }

        val here = topsByLocation[locationId]?.takeIf { it.size >= minWorkoutsHere } ?: return null
        val hereKg = median(here)
        return topsByLocation.asSequence()
            .filter { (id, tops) -> id != locationId && tops.size >= minWorkoutsElsewhere }
            .map { (id, tops) -> LocationSplitSuggestion(exerciseId, locationId, hereKg, id, median(tops)) }
            .filter { it.ratio >= minRatio - EPSILON }
            .maxByOrNull { it.ratio }
    }

    /**
     * Like [evaluate], but only for an [exercise] that could still be split: a shared strength
     * exercise whose prompt was not muted with "Don't ask again".
     */
    fun evaluateFor(exercise: Exercise, sessions: List<WorkoutSession>, locationId: String?): LocationSplitSuggestion? {
        if (exercise.trackPerLocation || exercise.splitPromptMuted || exercise.category != ExerciseCategory.Strength) return null
        return evaluate(sessions, exercise.id, locationId)
    }

    /** In the live workout only sets marked done count; the rest may still hold another gym's prefill. */
    private fun topWorkingKg(session: WorkoutSession, exerciseId: String): Double? {
        val live = session.status == WorkoutSessionStatus.Active
        return session.entries.asSequence()
            .filter { it.exerciseId == exerciseId && it.isStrength }
            .flatMap { it.strengthSets.asSequence() }
            .filter { !live || it.performedAtUtc != null }
            .filter { it.reps > 0 && it.weightKg > 0.0 && !it.isWarmup && SetTag.Warmup !in it.effectiveTags }
            .maxOfOrNull { it.weightKg }
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

    private companion object {
        const val EPSILON = 1e-9
    }
}
