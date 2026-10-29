package com.lukr99.workout.domain.recovery

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.nowMillis
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/**
 * The muscle recovery model behind the Progress body map (docs/research/muscle-recovery.md).
 *
 * Every working set adds fatigue to the muscles it trains: 1 for the main muscle and
 * [RecoveryConfig.secondaryMuscleContribution] for each secondary one, scaled by how close to failure
 * the set was and whether it was eccentric. Fatigue halves every [RecoveryConfig.halfLifeFor] hours.
 * Readiness is `100 * exp(-fatigue / fatigueSaturationSets)`. Weekly hard sets use the same muscle
 * shares without the effort scaling, and skip sets that were far from failure.
 */
object RecoveryEngine {
    fun calculate(
        sessions: Iterable<WorkoutSession>,
        exercises: Iterable<Exercise>,
        nowUtcMillis: Long = nowMillis(),
        config: RecoveryConfig = RecoveryConfig(),
    ): RecoverySnapshot {
        val catalog = exercises.associateBy(Exercise::id)
        val accumulators = linkedMapOf<String, MuscleAccumulator>()
        fun add(muscles: Map<String, Double>, occurredAt: Long, fatigue: Double, hardSets: Double, volumeKg: Double) {
            val ageHours = (nowUtcMillis - occurredAt) / 3_600_000.0
            if (ageHours < 0 || ageHours > config.lookbackHours) return
            muscles.forEach { (muscle, share) ->
                val acc = accumulators.getOrPut(muscle) { MuscleAccumulator(MuscleGroups.groupOf(muscle)) }
                acc.fatigueNow += fatigue * share * 0.5.pow(ageHours / config.halfLifeFor(acc.group))
                acc.lastTrainedAtUtc = maxOf(acc.lastTrainedAtUtc ?: Long.MIN_VALUE, occurredAt)
                if (ageHours <= config.weeklyWindowHours) {
                    acc.weeklySetCount += hardSets * share
                    acc.weeklyVolumeKg += volumeKg * share
                }
            }
        }

        sessions.asSequence()
            .filter { it.status == WorkoutSessionStatus.Completed }
            .forEach { session ->
                val sessionAt = session.completedDateUtc ?: session.startedAtUtc
                session.entries.forEach { entry ->
                    val exercise = catalog[entry.exerciseId]
                    val muscles = muscleShares(entry.exerciseSnapshotPrimaryBodyPart, exercise, config)
                    entry.strengthSets.filter(::isWorkingSet).forEach { set ->
                        add(
                            muscles,
                            occurredAt = set.performedAtUtc ?: sessionAt,
                            fatigue = effort(set, config),
                            hardSets = if (isHardSet(set, config)) 1.0 else 0.0,
                            volumeKg = set.weightKg * set.reps,
                        )
                    }
                    entry.cardioData?.durationSeconds?.takeIf { it > 0 }?.let { seconds ->
                        add(muscles, sessionAt, seconds / 60.0 / config.cardioMinutesPerSet, 0.0, 0.0)
                    }
                }
            }

        val muscles = accumulators.map { (muscle, data) ->
            val readiness = readiness(data.fatigueNow, config)
            MuscleRecovery(
                bodyPart = muscle,
                group = data.group,
                readiness = readiness,
                state = stateOf(readiness, config),
                fatigueLoad = data.fatigueNow,
                lastTrainedAtUtc = data.lastTrainedAtUtc,
                readyAtUtc = readyAt(nowUtcMillis, data.fatigueNow, config.halfLifeFor(data.group), config),
                weeklyVolumeKg = data.weeklyVolumeKg,
                weeklySetCount = data.weeklySetCount,
                weeklyStatus = data.group?.let { weeklyStatus(data.weeklySetCount, config) },
            )
        }.sortedWith(compareBy(MuscleRecovery::readiness, MuscleRecovery::bodyPart))
        return RecoverySnapshot(
            calculatedAtUtc = nowUtcMillis,
            muscles = muscles,
            averageReadiness = muscles.map(MuscleRecovery::readiness).average()
                .takeUnless(Double::isNaN) ?: 100.0,
            config = config,
        )
    }

    /** Reps in reserve for a set: logged RIR, else 10 minus RPE, else 0 for a failure set. */
    fun repsInReserve(set: StrengthSet): Double? = set.rir
        ?: set.rpe?.let { (10.0 - it).coerceAtLeast(0.0) }
        ?: 0.0.takeIf {
            set.setType == SetType.Failure || SetTag.ToFailure in set.tags || SetTag.Failed in set.tags
        }

    /** The fatigue one set adds to its main muscle. */
    fun effort(set: StrengthSet, config: RecoveryConfig = RecoveryConfig()): Double {
        val rir = repsInReserve(set) ?: config.defaultRir
        val base = (1.0 + (config.defaultRir - rir) * config.effortPerRir)
            .coerceIn(config.minEffort, config.maxEffort)
        val eccentric = set.setType == SetType.Negative ||
            SetTag.Negative in set.tags || SetTag.SlowEccentric in set.tags
        return if (eccentric) base * config.eccentricMultiplier else base
    }

    fun weeklyStatus(hardSets: Double, config: RecoveryConfig = RecoveryConfig()): WeeklyVolumeStatus = when {
        hardSets <= 0.0 -> WeeklyVolumeStatus.None
        hardSets < config.weeklyTargetSets.start -> WeeklyVolumeStatus.Below
        hardSets > config.weeklyTargetSets.endInclusive -> WeeklyVolumeStatus.Above
        else -> WeeklyVolumeStatus.InRange
    }

    private fun isWorkingSet(set: StrengthSet): Boolean =
        !set.isWarmup && set.setType != SetType.Warmup && SetTag.Warmup !in set.tags &&
            (set.reps > 0 || (set.durationSeconds ?: 0) > 0)

    private fun isHardSet(set: StrengthSet, config: RecoveryConfig): Boolean =
        (repsInReserve(set) ?: config.defaultRir) < config.easySetRir

    private fun muscleShares(
        snapshotPrimary: String,
        exercise: Exercise?,
        config: RecoveryConfig,
    ): Map<String, Double> {
        val primary = snapshotPrimary.trim().ifBlank { exercise?.primaryBodyPart.orEmpty().trim() }
        val shares = linkedMapOf<String, Double>()
        fun put(name: String, weight: Double) = MuscleGroups.resolve(name).forEach { (muscle, share) ->
            val key = shares.keys.firstOrNull { it.equals(muscle, ignoreCase = true) } ?: muscle
            shares[key] = maxOf(shares[key] ?: 0.0, share * weight)
        }
        put(primary, 1.0)
        exercise?.secondaryBodyParts.orEmpty().forEach { put(it, config.secondaryMuscleContribution) }
        if (shares.isEmpty()) put(if (exercise?.category == ExerciseCategory.Cardio) "Cardio" else "Full Body", 1.0)
        return shares
    }

    private fun readiness(fatigue: Double, config: RecoveryConfig): Double =
        (100.0 * exp(-fatigue / config.fatigueSaturationSets)).coerceIn(0.0, 100.0)

    private fun stateOf(readiness: Double, config: RecoveryConfig): RecoveryState = when {
        readiness >= config.readyThreshold -> RecoveryState.Ready
        readiness >= config.fatiguedThreshold -> RecoveryState.Recovering
        else -> RecoveryState.Fatigued
    }

    private fun readyAt(nowUtcMillis: Long, fatigueNow: Double, halfLifeHours: Double, config: RecoveryConfig): Long? {
        if (readiness(fatigueNow, config) >= config.readyThreshold) return nowUtcMillis
        val targetFatigue = -config.fatigueSaturationSets * ln(config.readyThreshold / 100.0)
        if (targetFatigue <= 0 || fatigueNow <= 0) return null
        val hours = halfLifeHours * (ln(fatigueNow / targetFatigue) / ln(2.0))
        return nowUtcMillis + (hours.coerceAtLeast(0.0) * 3_600_000).toLong()
    }

    private class MuscleAccumulator(
        val group: MuscleGroup?,
        var fatigueNow: Double = 0.0,
        var lastTrainedAtUtc: Long? = null,
        var weeklyVolumeKg: Double = 0.0,
        var weeklySetCount: Double = 0.0,
    )
}
