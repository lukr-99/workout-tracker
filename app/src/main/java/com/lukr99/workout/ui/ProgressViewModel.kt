package com.lukr99.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.services.WorkoutDataService
import com.lukr99.workout.data.services.WorkoutInsightsService
import com.lukr99.workout.domain.Estimates
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.records.ExerciseRecords
import com.lukr99.workout.domain.recovery.RecoverySnapshot
import com.lukr99.workout.domain.query.WorkoutCriterion
import com.lukr99.workout.domain.query.WorkoutQuery
import com.lukr99.workout.domain.query.asFilter
import com.lukr99.workout.domain.stats.DimensionKeys
import com.lukr99.workout.domain.stats.MetricKeys
import com.lukr99.workout.domain.stats.StatsRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The Progress analytics tab. Headline KPIs and the weekly-volume trend come from the Phase 3 stats
 * service (`calculateStats`); per-exercise series are built from the same completed sessions with the
 * domain `Estimates`/e1RM helpers. No stats math is reimplemented here.
 */
class ProgressViewModel(
    private val repo: WorkoutRepository,
    private val data: WorkoutDataService,
    private val insights: WorkoutInsightsService,
) : ViewModel() {

    private val uiState = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = uiState.asStateFlow()

    /** Records for the currently-open per-exercise detail (Phase 3.5 `insights.records`). */
    private val recordsState = MutableStateFlow<ExerciseRecords?>(null)
    val records: StateFlow<ExerciseRecords?> = recordsState.asStateFlow()

    fun loadRecords(exerciseId: String) {
        recordsState.value = null
        viewModelScope.launch { recordsState.value = insights.records(exerciseId) }
    }

    fun refresh() {
        viewModelScope.launch {
            val completedFilter = WorkoutCriterion.Statuses(setOf(WorkoutSessionStatus.Completed)).asFilter()
            val query = WorkoutQuery(filter = completedFilter)
            val sessions = data.querySessions(query)

            val overviewReport = data.calculateStats(
                StatsRequest(
                    query = query,
                    metrics = listOf(MetricKeys.Workouts, MetricKeys.VolumeKg, MetricKeys.Sets, MetricKeys.PrSets),
                ),
            )
            val row = overviewReport.rows.firstOrNull()
            val overview = ProgressOverview(
                workouts = row?.metrics?.get(MetricKeys.Workouts)?.value?.toInt() ?: 0,
                volumeKg = row?.metrics?.get(MetricKeys.VolumeKg)?.value ?: 0.0,
                sets = row?.metrics?.get(MetricKeys.Sets)?.value?.toInt() ?: 0,
                prSets = row?.metrics?.get(MetricKeys.PrSets)?.value?.toInt() ?: 0,
                streakWeeks = repo.getConsistencySnapshot().currentWeeklyStreak,
            )

            val weekReport = data.calculateStats(
                StatsRequest(
                    query = query,
                    metrics = listOf(MetricKeys.VolumeKg),
                    dimensions = listOf(DimensionKeys.Week),
                ),
            )
            val weekly = weekReport.rows
                .mapNotNull { r -> r.dimensions[DimensionKeys.Week]?.let { it to (r.metrics[MetricKeys.VolumeKg]?.value ?: 0.0) } }
                .sortedBy { it.first }
                .takeLast(10)
                .map { (week, vol) -> WeeklyVolume(label = week.substringAfter("-W").let { "W$it" }, volumeKg = vol) }

            uiState.value = ProgressUiState(
                loaded = true,
                overview = overview,
                weeklyVolume = weekly,
                exercises = buildExerciseSummaries(sessions),
                recovery = insights.recovery(),
            )
        }
    }

    fun detailFor(exerciseId: String): ExerciseProgressDetail? {
        val summary = uiState.value.exercises.firstOrNull { it.exerciseId == exerciseId } ?: return null
        return ExerciseProgressDetail(summary = summary, points = summary.points)
    }

    private fun buildExerciseSummaries(sessions: List<WorkoutSession>): List<ExerciseProgressSummary> {
        data class Acc(
            val name: String,
            val bodyPart: String,
            val category: ExerciseCategory,
            val points: MutableList<ProgressPoint> = mutableListOf(),
        )

        val byExercise = LinkedHashMap<String, Acc>()
        for (session in sessions.sortedBy { it.completedDateUtc ?: it.startedAtUtc }) {
            val date = session.completedDateUtc ?: session.startedAtUtc
            session.entries.filter { it.entryType == ExerciseCategory.Strength && it.exerciseId.isNotBlank() }
                .groupBy { it.exerciseId }
                .forEach { (id, entries) ->
                    val sets = entries.flatMap { it.strengthSets }
                    if (sets.isEmpty()) return@forEach
                    val acc = byExercise.getOrPut(id) {
                        Acc(
                            name = entries.first().exerciseSnapshotName,
                            bodyPart = entries.first().exerciseSnapshotPrimaryBodyPart,
                            category = ExerciseCategory.Strength,
                        )
                    }
                    acc.points += ProgressPoint(
                        dateMillis = date,
                        e1rmKg = Estimates.bestEpley(sets),
                        volumeKg = Estimates.volume(sets),
                        reps = Estimates.totalReps(sets),
                        sessionName = session.name,
                    )
                }
        }

        return byExercise.map { (id, acc) ->
            val e1rms = acc.points.map { it.e1rmKg }
            val best = e1rms.maxOrNull() ?: 0.0
            val latest = e1rms.lastOrNull() ?: 0.0
            val previous = e1rms.dropLast(1).lastOrNull()
            ExerciseProgressSummary(
                exerciseId = id,
                name = acc.name,
                bodyPart = acc.bodyPart,
                category = acc.category,
                bestE1rmKg = best,
                latestE1rmKg = latest,
                deltaKg = if (previous != null) latest - previous else 0.0,
                points = acc.points,
            )
        }.sortedByDescending { it.points.lastOrNull()?.dateMillis ?: 0L }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProgressViewModel(container.repository, container.workoutData, container.insights) as T
        }
    }
}

data class ProgressUiState(
    val loaded: Boolean = false,
    val overview: ProgressOverview = ProgressOverview(),
    val weeklyVolume: List<WeeklyVolume> = emptyList(),
    val exercises: List<ExerciseProgressSummary> = emptyList(),
    val recovery: RecoverySnapshot? = null,
)

data class ProgressOverview(
    val workouts: Int = 0,
    val volumeKg: Double = 0.0,
    val sets: Int = 0,
    val prSets: Int = 0,
    val streakWeeks: Int = 0,
)

data class WeeklyVolume(val label: String, val volumeKg: Double)

data class ProgressPoint(
    val dateMillis: Long,
    val e1rmKg: Double,
    val volumeKg: Double,
    val reps: Int,
    val sessionName: String,
)

data class ExerciseProgressSummary(
    val exerciseId: String,
    val name: String,
    val bodyPart: String,
    val category: ExerciseCategory,
    val bestE1rmKg: Double,
    val latestE1rmKg: Double,
    val deltaKg: Double,
    val points: List<ProgressPoint>,
)

data class ExerciseProgressDetail(
    val summary: ExerciseProgressSummary,
    val points: List<ProgressPoint>,
)
