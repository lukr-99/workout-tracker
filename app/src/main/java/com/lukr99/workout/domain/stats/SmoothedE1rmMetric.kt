package com.lukr99.workout.domain.stats

import com.lukr99.workout.domain.Estimates
import com.lukr99.workout.domain.query.WorkoutDataPoint

/**
 * Chronological exponentially weighted e1RM trend. Each session contributes its best working-set
 * estimate so a high-set-count workout cannot dominate the chart.
 */
object SmoothedE1rmMetric : MetricProvider {
    override val key = MetricKeys.SmoothedE1rmKg

    override fun calculate(points: List<WorkoutDataPoint>): MetricValue {
        val perSession = points
            .filter { it.strengthSet != null && it.strengthSet.isWarmup != true }
            .groupBy { it.session.id }
            .values
            .mapNotNull { sessionPoints ->
                val estimate = sessionPoints.maxOfOrNull { point ->
                    point.strengthSet?.let { Estimates.epley(it.weightKg, it.reps) } ?: 0.0
                } ?: return@mapNotNull null
                sessionPoints.first().session.startedAtUtc to estimate
            }
            .sortedBy { it.first }

        var smoothed: Double? = null
        perSession.forEach { (_, estimate) ->
            smoothed = smoothed?.let { previous -> (0.35 * estimate) + (0.65 * previous) } ?: estimate
        }
        return MetricValue(smoothed ?: 0.0, MetricUnit.Kilograms)
    }
}
