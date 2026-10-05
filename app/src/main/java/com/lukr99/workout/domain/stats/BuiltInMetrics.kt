package com.lukr99.workout.domain.stats

import com.lukr99.workout.domain.Estimates
import com.lukr99.workout.domain.query.WorkoutDataPoint

object BuiltInMetrics {
    val all: List<MetricProvider> = listOf(
        metric(MetricKeys.Workouts, MetricUnit.Count) { points ->
            points.distinctBy { it.session.id }.size.toDouble()
        },
        metric(MetricKeys.Entries, MetricUnit.Count) { points ->
            points.mapNotNull { it.entry?.id }.distinct().size.toDouble()
        },
        metric(MetricKeys.Sets, MetricUnit.Count) { points ->
            points.count { it.strengthSet != null }.toDouble()
        },
        metric(MetricKeys.Reps, MetricUnit.Count) { points ->
            points.sumOf { it.strengthSet?.reps ?: 0 }.toDouble()
        },
        metric(MetricKeys.VolumeKg, MetricUnit.Kilograms) { points ->
            points.sumOf { point ->
                point.strengthSet?.let { it.weightKg * it.reps } ?: 0.0
            }
        },
        metric(MetricKeys.AverageWeightKg, MetricUnit.Kilograms) { points ->
            points.mapNotNull { it.strengthSet?.weightKg }.averageOrZero()
        },
        metric(MetricKeys.MaxWeightKg, MetricUnit.Kilograms) { points ->
            points.maxOfOrNull { it.strengthSet?.weightKg ?: 0.0 } ?: 0.0
        },
        metric(MetricKeys.BestE1rmKg, MetricUnit.Kilograms) { points ->
            points.maxOfOrNull {
                it.strengthSet?.let { set -> Estimates.epley(set.weightKg, set.reps) } ?: 0.0
            } ?: 0.0
        },
        SmoothedE1rmMetric,
        metric(MetricKeys.DurationSeconds, MetricUnit.Seconds) { points ->
            points.distinctBy { it.session.id }.sumOf { it.session.durationSeconds }.toDouble()
        },
        metric(MetricKeys.TimedWorkSeconds, MetricUnit.Seconds) { points ->
            points.sumOf {
                (it.strengthSet?.durationSeconds ?: 0) + (it.cardio?.durationSeconds ?: 0)
            }.toDouble()
        },
        metric(MetricKeys.CardioDistanceKm, MetricUnit.Kilometers) { points ->
            points.sumOf { it.cardio?.distanceKm ?: 0.0 }
        },
        metric(MetricKeys.Calories, MetricUnit.Kilocalories) { points ->
            points.sumOf { it.cardio?.calories ?: 0.0 }
        },
        metric(MetricKeys.PrSets, MetricUnit.Count) { points ->
            points.count { it.strengthSet?.isPr == true }.toDouble()
        },
        metric(MetricKeys.AverageRpe, MetricUnit.Ratio) { points ->
            points.mapNotNull { it.strengthSet?.rpe }.averageOrZero()
        },
    )

    private fun metric(
        key: String,
        unit: MetricUnit,
        block: (List<WorkoutDataPoint>) -> Double,
    ): MetricProvider = object : MetricProvider {
        override val key = key
        override fun calculate(points: List<WorkoutDataPoint>) = MetricValue(block(points), unit)
    }
}
