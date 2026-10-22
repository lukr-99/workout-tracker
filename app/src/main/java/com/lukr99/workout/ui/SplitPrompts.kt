package com.lukr99.workout.ui

import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.domain.LocationSplitHeuristic
import com.lukr99.workout.domain.WorkoutSession

/**
 * Decides when the live workout asks "Track this exercise separately per gym?" ([LocationSplitHeuristic]),
 * and remembers which exercises were already asked in this workout, so each is asked at most once.
 */
class SplitPrompts(
    private val repo: WorkoutRepository,
    private val heuristic: LocationSplitHeuristic = LocationSplitHeuristic(),
) {
    private val asked = mutableSetOf<String>()

    /** A new workout may ask again. */
    fun reset() = asked.clear()

    /**
     * The question for [entryId] in [draft] after one of its sets was done, or null. Never for a
     * workout without a location. The draft is newer than what is saved, so it stands in for the
     * saved copy of the live workout.
     */
    suspend fun check(draft: WorkoutSession, entryId: String): SplitPrompt? {
        val entry = draft.entries.firstOrNull { it.id == entryId } ?: return null
        val locationId = draft.locationId ?: return null
        if (entry.exerciseId.isBlank() || entry.exerciseId in asked) return null
        val exercise = repo.getExercise(entry.exerciseId) ?: return null
        val sessions = repo.getSessions().filterNot { it.id == draft.id } + draft
        val suggestion = heuristic.evaluateFor(exercise, sessions, locationId) ?: return null
        if (!asked.add(exercise.id)) return null
        return SplitPrompt(
            exerciseId = exercise.id,
            exerciseName = entry.exerciseSnapshotName.ifBlank { exercise.name },
            hereKg = suggestion.hereTopKg,
            otherKg = suggestion.otherTopKg,
            otherLocationName = repo.getLocation(suggestion.otherLocationId)?.name ?: "another gym",
        )
    }
}
