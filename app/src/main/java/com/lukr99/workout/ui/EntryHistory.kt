package com.lukr99.workout.ui

import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.services.WorkoutInsightsService
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseOuting
import com.lukr99.workout.domain.LocationScope
import com.lukr99.workout.domain.PreviousEntryNote
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.lastSetsFor
import com.lukr99.workout.domain.newId
import com.lukr99.workout.domain.progression.DoubleProgression
import com.lukr99.workout.domain.progression.SuggestionStatus
import com.lukr99.workout.domain.recentOutings

/**
 * What the live workout knows about earlier workouts: last time's sets and notes for the
 * exercises on screen, and what a newly added exercise should start with. Each read takes the
 * workout's location, so an exercise tracked per location only sees workouts at that place.
 */
class EntryHistory(
    private val repo: WorkoutRepository,
    private val insights: WorkoutInsightsService,
) {
    suspend fun notes(
        exerciseIds: Set<String>,
        currentSessionId: String,
        locationId: String?,
    ): Map<String, PreviousEntryNote> = repo.getPreviousEntryNotes(exerciseIds, currentSessionId, locationId)

    /** Last time's sets per exercise, from the newest finished workout that logged it. */
    suspend fun lastSets(exerciseIds: Set<String>, locationId: String?): Map<String, List<StrengthSet>> =
        repo.getLastSets(exerciseIds, locationId)

    /**
     * The progression's suggested sets when it has enough history, else last time's sets, else
     * nothing (the exercise keeps its single empty set). Cardio gets nothing.
     */
    suspend fun prefill(exercise: Exercise, locationId: String?): EntryPrefill? {
        if (exercise.category != ExerciseCategory.Strength || exercise.id.isBlank()) return null
        val scope = LocationScope.of(exercise, locationId)
        val suggestion = runCatching { insights.progression(exercise.id, DoubleProgression(), scope = scope) }.getOrNull()
        if (suggestion != null && suggestion.status == SuggestionStatus.Ready && suggestion.targets.isNotEmpty()) {
            val sets = suggestion.targets.mapIndexed { i, t ->
                StrengthSet(id = newId(), setNumber = i + 1, reps = t.reps, weightKg = t.weightKg, setType = t.setType)
            }
            return EntryPrefill(sets, suggestion.rationale)
        }
        return scope.apply(repo.getSessions()).lastSetsFor(exercise.id).takeIf { it.isNotEmpty() }?.let { EntryPrefill(it) }
    }

    /** The newest few finished workouts with this exercise, for the exercise menu. */
    suspend fun outings(exerciseId: String, locationId: String?, limit: Int = 3): List<ExerciseOuting> =
        repo.locationScope(exerciseId, locationId).apply(repo.getSessions()).recentOutings(exerciseId, limit)

    /** Exercises from the newest finished workouts, newest first, without repeats. */
    suspend fun recentExerciseIds(limit: Int = 8): List<String> =
        repo.getSessions()
            .filter { it.status == WorkoutSessionStatus.Completed }
            .sortedByDescending { it.completedDateUtc ?: it.startedAtUtc }
            .asSequence()
            .flatMap { session -> session.entries.sortedBy { it.sortOrder }.asSequence() }
            .map { it.exerciseId }
            .filter(String::isNotBlank)
            .distinct()
            .take(limit)
            .toList()

    /**
     * The newest finished workouts to repeat, one per name so a weekly "Push day" shows once, and
     * only those with exercises to copy.
     */
    suspend fun repeatable(limit: Int = 3): List<WorkoutSession> =
        repo.getSessions()
            .filter { it.status == WorkoutSessionStatus.Completed && it.entries.any { e -> e.exerciseId.isNotBlank() } }
            .sortedByDescending { it.completedDateUtc ?: it.startedAtUtc }
            .distinctBy { it.name.trim().lowercase() }
            .take(limit)

    /** The templates an empty workout can switch to. */
    suspend fun templates(): List<WorkoutTemplate> = repo.getTemplates()
}
