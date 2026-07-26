package com.lukr99.workout.data.services

import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.progression.DeloadPolicy
import com.lukr99.workout.domain.progression.ProgressionScheme
import com.lukr99.workout.domain.progression.ProgressionSuggestion
import com.lukr99.workout.domain.progression.ProgressionSuggestionEngine
import com.lukr99.workout.domain.query.WorkoutCriterion
import com.lukr99.workout.domain.query.WorkoutQuery
import com.lukr99.workout.domain.query.asFilter
import com.lukr99.workout.domain.records.ExerciseRecords
import com.lukr99.workout.domain.records.RecordAchievements
import com.lukr99.workout.domain.records.RecordsEngine
import com.lukr99.workout.domain.recovery.BodyPartStatsKeys
import com.lukr99.workout.domain.recovery.BodyPartStatsProviders
import com.lukr99.workout.domain.recovery.RecoveryConfig
import com.lukr99.workout.domain.recovery.RecoveryEngine
import com.lukr99.workout.domain.recovery.RecoverySnapshot
import com.lukr99.workout.domain.stats.BuiltInDimensions
import com.lukr99.workout.domain.stats.BuiltInMetrics
import com.lukr99.workout.domain.stats.StatsEngine
import com.lukr99.workout.domain.stats.StatsReport
import com.lukr99.workout.domain.stats.StatsRequest

/** Repository-backed entry point for Phase 3.5 analytics and coaching features. */
class WorkoutInsightsService(
    private val repository: WorkoutRepository,
) {
    suspend fun records(
        exerciseId: String,
        repRange: IntRange = 1..12,
    ): ExerciseRecords = RecordsEngine.forExercise(
        repository.getSessions(),
        exerciseId,
        repRange,
    )

    suspend fun evaluateSetRecord(
        exerciseId: String,
        candidate: StrengthSet,
        repRange: IntRange = 1..12,
    ): RecordAchievements = RecordsEngine.evaluateSet(
        repository.getSessions(),
        exerciseId,
        candidate,
        repRange,
    )

    suspend fun evaluateSessionRecords(
        candidate: WorkoutSession,
        exerciseId: String,
        repRange: IntRange = 1..12,
    ): RecordAchievements = RecordsEngine.evaluateSession(
        repository.getSessions(),
        candidate,
        exerciseId,
        repRange,
    )

    suspend fun recovery(
        nowUtcMillis: Long = System.currentTimeMillis(),
        config: RecoveryConfig = RecoveryConfig(),
    ): RecoverySnapshot = RecoveryEngine.calculate(
        sessions = repository.getSessions(),
        exercises = repository.getExercises(ExerciseFilter(includeArchived = true)),
        nowUtcMillis = nowUtcMillis,
        config = config,
    )

    suspend fun bodyPartStats(
        request: StatsRequest = defaultBodyPartStatsRequest(),
    ): StatsReport {
        val catalog = repository.getExercises(ExerciseFilter(includeArchived = true))
        val engine = StatsEngine(
            metricProviders = BuiltInMetrics.all + BodyPartStatsProviders.metrics,
            dimensionProviders = BuiltInDimensions.all +
                BodyPartStatsProviders.bodyPartDimension(catalog),
        )
        return engine.calculate(repository.getSessions(), request)
    }

    suspend fun progression(
        exerciseId: String,
        scheme: ProgressionScheme,
        deload: DeloadPolicy = DeloadPolicy(),
    ): ProgressionSuggestion = ProgressionSuggestionEngine.suggest(
        repository.getSessions(),
        exerciseId,
        scheme,
        deload,
    )

    companion object {
        fun defaultBodyPartStatsRequest() = StatsRequest(
            query = WorkoutQuery(
                filter = WorkoutCriterion.Statuses(setOf(WorkoutSessionStatus.Completed)).asFilter(),
            ),
            metrics = listOf(
                BodyPartStatsKeys.WorkingSetCount,
                BodyPartStatsKeys.WorkingVolumeKg,
            ),
            dimensions = listOf(BodyPartStatsKeys.AllBodyParts),
        )
    }
}
