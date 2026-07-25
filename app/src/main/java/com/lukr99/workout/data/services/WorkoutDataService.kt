package com.lukr99.workout.data.services

import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.creation.CreationPolicy
import com.lukr99.workout.domain.creation.CreationResult
import com.lukr99.workout.domain.creation.ExerciseDraft
import com.lukr99.workout.domain.creation.SessionDraft
import com.lukr99.workout.domain.creation.TemplateDraft
import com.lukr99.workout.domain.creation.WorkoutFactory
import com.lukr99.workout.domain.query.WorkoutQuery
import com.lukr99.workout.domain.query.WorkoutQueryEngine
import com.lukr99.workout.domain.stats.StatsEngine
import com.lukr99.workout.domain.stats.StatsReport
import com.lukr99.workout.domain.stats.StatsRequest

/**
 * Rich service facade for Phase 2+ ViewModels. It keeps creation/validation and ad-hoc statistics
 * out of UI code while preserving the repository as the persistence boundary.
 */
class WorkoutDataService(
    private val repository: WorkoutRepository,
    val factory: WorkoutFactory = WorkoutFactory(),
    val stats: StatsEngine = StatsEngine(),
) {
    suspend fun snapshot(includeDiscardedSessions: Boolean = false): WorkoutDataSnapshot =
        WorkoutDataSnapshot(
            exercises = repository.getExercises(ExerciseFilter(includeArchived = true)),
            templates = repository.getTemplates(),
            sessions = repository.getSessions(includeDiscardedSessions),
        )

    suspend fun querySessions(
        query: WorkoutQuery,
        includeDiscardedSessions: Boolean = false,
    ): List<WorkoutSession> = WorkoutQueryEngine.filterSessions(
        repository.getSessions(includeDiscardedSessions),
        query,
    )

    suspend fun createExercise(
        draft: ExerciseDraft,
        policy: CreationPolicy = CreationPolicy(),
    ): CreationResult<Exercise> {
        val result = factory.exercise(draft, policy)
        if (result.isValid) repository.saveExercise(result.value)
        return result
    }

    suspend fun createTemplate(
        draft: TemplateDraft,
        policy: CreationPolicy = CreationPolicy(),
    ): CreationResult<WorkoutTemplate> {
        val catalog = repository.getExercises(ExerciseFilter(includeArchived = true))
            .associateBy(Exercise::id)
        val result = factory.template(draft, catalog, policy)
        if (result.isValid) repository.saveTemplate(result.value)
        return result
    }

    suspend fun createSession(
        draft: SessionDraft,
        policy: CreationPolicy = CreationPolicy(),
    ): CreationResult<WorkoutSession> {
        val catalog = repository.getExercises(ExerciseFilter(includeArchived = true))
            .associateBy(Exercise::id)
        val result = factory.session(draft, catalog, policy)
        if (result.isValid) repository.saveWorkoutSession(result.value)
        return result
    }

    suspend fun calculateStats(
        request: StatsRequest = StatsRequest(),
        includeDiscardedSessions: Boolean = false,
    ): StatsReport = stats.calculate(repository.getSessions(includeDiscardedSessions), request)
}

data class WorkoutDataSnapshot(
    val exercises: List<Exercise>,
    val templates: List<WorkoutTemplate>,
    val sessions: List<WorkoutSession>,
)
