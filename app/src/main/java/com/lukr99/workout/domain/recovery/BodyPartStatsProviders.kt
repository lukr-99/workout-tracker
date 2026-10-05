package com.lukr99.workout.domain.recovery

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.query.WorkoutDataPoint
import com.lukr99.workout.domain.stats.DimensionProvider
import com.lukr99.workout.domain.stats.ExpandingDimensionProvider
import com.lukr99.workout.domain.stats.MetricProvider
import com.lukr99.workout.domain.stats.MetricUnit
import com.lukr99.workout.domain.stats.MetricValue
import java.time.ZoneId

object BodyPartStatsProviders {
    val metrics: List<MetricProvider> = listOf(
        object : MetricProvider {
            override val key = BodyPartStatsKeys.WorkingSetCount
            override fun calculate(points: List<WorkoutDataPoint>) = MetricValue(
                points.count {
                    it.strengthSet?.let { set ->
                        !set.isWarmup && set.setType != SetType.Warmup
                    } == true
                }.toDouble(),
                MetricUnit.Count,
            )
        },
        object : MetricProvider {
            override val key = BodyPartStatsKeys.WorkingVolumeKg
            override fun calculate(points: List<WorkoutDataPoint>) = MetricValue(
                points.sumOf {
                    it.strengthSet?.takeIf { set ->
                        !set.isWarmup && set.setType != SetType.Warmup
                    }?.let { set -> set.weightKg * set.reps } ?: 0.0
                },
                MetricUnit.Kilograms,
            )
        },
    )

    fun bodyPartDimension(exercises: Iterable<Exercise>): DimensionProvider {
        val catalog = exercises.associateBy(Exercise::id)
        return object : ExpandingDimensionProvider {
            override val key = BodyPartStatsKeys.AllBodyParts
            override fun resolveValues(point: WorkoutDataPoint, zoneId: ZoneId): Set<String> {
                val entry = point.entry ?: return emptySet()
                val exercise = catalog[entry.exerciseId]
                val values = linkedMapOf<String, String>()
                return (sequenceOf(entry.exerciseSnapshotPrimaryBodyPart) +
                    exercise?.secondaryBodyParts.orEmpty().asSequence())
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .onEach { values.putIfAbsent(it.lowercase(), it) }
                    .toList()
                    .let { values.values.toSet() }
            }
        }
    }
}
