package com.lukr99.workout.domain.recovery

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import kotlin.math.exp
import kotlin.math.ln

/** Tunable recency/load model for the Progress muscle-recovery surface. */
object RecoveryEngine {
    fun calculate(
        sessions: Iterable<WorkoutSession>,
        exercises: Iterable<Exercise>,
        nowUtcMillis: Long = System.currentTimeMillis(),
        config: RecoveryConfig = RecoveryConfig(),
    ): RecoverySnapshot {
        val catalog = exercises.associateBy(Exercise::id)
        val accumulators = linkedMapOf<String, MuscleAccumulator>()
        sessions.asSequence()
            .filter { it.status == WorkoutSessionStatus.Completed }
            .forEach { session ->
                val occurredAt = session.completedDateUtc ?: session.startedAtUtc
                val ageHours = (nowUtcMillis - occurredAt) / 3_600_000.0
                if (ageHours < 0 || ageHours > config.lookbackHours) return@forEach
                session.entries.forEach entryLoop@{ entry ->
                    val exercise = catalog[entry.exerciseId]
                    val muscles = muscleWeights(entry.exerciseSnapshotPrimaryBodyPart, exercise, config)
                    val sets = entry.strengthSets.filter {
                        !it.isWarmup && it.setType != SetType.Warmup
                    }
                    val volume = sets.sumOf { it.weightKg * it.reps }
                    val strengthLoad = sets.size * config.setLoadUnits +
                        volume / config.volumeKgPerLoadUnit
                    val cardioLoad = entry.cardioData?.durationSeconds
                        ?.div(60.0 * config.cardioMinutesPerLoadUnit)
                        ?: 0.0
                    val baseLoad = strengthLoad + cardioLoad
                    if (baseLoad <= 0) return@entryLoop
                    muscles.forEach { (muscle, weight) ->
                        val accumulator = accumulators.getOrPut(muscle) { MuscleAccumulator() }
                        val weightedLoad = baseLoad * weight
                        accumulator.fatigueNow += weightedLoad *
                            halfLifeDecay(ageHours, config.halfLifeHours)
                        accumulator.lastTrainedAtUtc = maxOf(
                            accumulator.lastTrainedAtUtc ?: Long.MIN_VALUE,
                            occurredAt,
                        )
                        if (ageHours <= config.weeklyWindowHours) {
                            accumulator.weeklyVolumeKg += volume * weight
                            accumulator.weeklySetCount += sets.size * weight
                            accumulator.weeklyLoadUnits += weightedLoad
                        }
                    }
                }
            }

        val muscles = accumulators.map { (muscle, data) ->
            val readiness = readiness(data.fatigueNow, config.fatigueSaturationLoad)
            MuscleRecovery(
                bodyPart = muscle,
                readiness = readiness,
                fatigueLoad = data.fatigueNow,
                lastTrainedAtUtc = data.lastTrainedAtUtc,
                readyAtUtc = readyAt(
                    nowUtcMillis,
                    data.fatigueNow,
                    config.readyThreshold,
                    config,
                ),
                weeklyVolumeKg = data.weeklyVolumeKg,
                weeklySetCount = data.weeklySetCount,
                weeklyLoadUnits = data.weeklyLoadUnits,
            )
        }.sortedWith(compareBy(MuscleRecovery::readiness, MuscleRecovery::bodyPart))
        return RecoverySnapshot(
            calculatedAtUtc = nowUtcMillis,
            muscles = muscles,
            averageReadiness = muscles.map(MuscleRecovery::readiness).average()
                .takeUnless(Double::isNaN) ?: 100.0,
        )
    }

    private fun muscleWeights(
        snapshotPrimary: String,
        exercise: Exercise?,
        config: RecoveryConfig,
    ): Map<String, Double> {
        val primary = snapshotPrimary.trim().ifBlank { exercise?.primaryBodyPart.orEmpty().trim() }
        val values = linkedMapOf<String, Double>()
        if (primary.isNotBlank()) values[primary] = 1.0
        exercise?.secondaryBodyParts.orEmpty()
            .map(String::trim)
            .filter(String::isNotBlank)
            .filterNot { it.equals(primary, ignoreCase = true) }
            .forEach { secondary ->
                val existing = values.keys.firstOrNull { it.equals(secondary, ignoreCase = true) }
                values[existing ?: secondary] = maxOf(
                    existing?.let(values::get) ?: 0.0,
                    config.secondaryMuscleContribution,
                )
            }
        return values.ifEmpty {
            mapOf(
                (if (exercise?.category == ExerciseCategory.Cardio) "Cardio" else "Full Body") to 1.0,
            )
        }
    }

    private fun readiness(fatigue: Double, saturation: Double): Double =
        (100.0 * exp(-fatigue / saturation)).coerceIn(0.0, 100.0)

    private fun halfLifeDecay(ageHours: Double, halfLifeHours: Double): Double =
        Math.pow(0.5, ageHours / halfLifeHours)

    private fun readyAt(
        nowUtcMillis: Long,
        fatigueNow: Double,
        threshold: Double,
        config: RecoveryConfig,
    ): Long? {
        if (readiness(fatigueNow, config.fatigueSaturationLoad) >= threshold) return nowUtcMillis
        val targetFatigue = -config.fatigueSaturationLoad * ln(threshold / 100.0)
        if (targetFatigue <= 0 || fatigueNow <= 0) return null
        val hours = config.halfLifeHours * (ln(fatigueNow / targetFatigue) / ln(2.0))
        return nowUtcMillis + (hours.coerceAtLeast(0.0) * 3_600_000).toLong()
    }

    private data class MuscleAccumulator(
        var fatigueNow: Double = 0.0,
        var lastTrainedAtUtc: Long? = null,
        var weeklyVolumeKg: Double = 0.0,
        var weeklySetCount: Double = 0.0,
        var weeklyLoadUnits: Double = 0.0,
    )
}
