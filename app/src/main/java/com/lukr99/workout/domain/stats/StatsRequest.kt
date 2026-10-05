package com.lukr99.workout.domain.stats

import com.lukr99.workout.domain.query.WorkoutQuery
import java.time.ZoneId

data class StatsRequest(
    val query: WorkoutQuery = WorkoutQuery(),
    val metrics: List<String> = listOf(
        MetricKeys.Workouts,
        MetricKeys.Sets,
        MetricKeys.Reps,
        MetricKeys.VolumeKg,
    ),
    val dimensions: List<String> = emptyList(),
    val timeZoneId: String = ZoneId.systemDefault().id,
    val sort: List<StatsSort> = emptyList(),
)
