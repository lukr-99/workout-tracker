package com.lukr99.workout.domain.stats

import com.lukr99.workout.domain.query.WorkoutDataPoint

interface MetricProvider {
    val key: String
    fun calculate(points: List<WorkoutDataPoint>): MetricValue
}
