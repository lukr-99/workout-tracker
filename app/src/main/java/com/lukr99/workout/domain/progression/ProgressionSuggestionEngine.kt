package com.lukr99.workout.domain.progression

import com.lukr99.workout.domain.Estimates
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import kotlin.math.floor
import kotlin.math.round

/** Pure progression prescriptions derived from completed exercise history. */
object ProgressionSuggestionEngine {
    fun suggest(
        sessions: Iterable<WorkoutSession>,
        exerciseId: String,
        scheme: ProgressionScheme,
        deload: DeloadPolicy = DeloadPolicy(),
    ): ProgressionSuggestion {
        require(exerciseId.isNotBlank()) { "exerciseId must not be blank." }
        val history = exerciseHistory(sessions, exerciseId)
        if (history.isEmpty()) {
            return ProgressionSuggestion(
                exerciseId = exerciseId,
                scheme = scheme.key,
                status = SuggestionStatus.InsufficientHistory,
                rationale = "Log at least one working session before requesting a progression target.",
            )
        }

        val latest = history.last()
        val base = when (scheme) {
            is DoubleProgression -> doubleProgression(latest, scheme)
            is LinearProgression -> linearProgression(latest, scheme)
            is PercentOfEstimated1Rm -> percentageProgression(history, scheme)
        }
        val stalled = isStalled(history, deload)
        val targets = if (stalled && deload.enabled) {
            val keep = floor(base.targets.size * (1.0 - deload.volumeReductionFraction))
                .toInt()
                .coerceIn(1, base.targets.size)
            base.targets.take(keep).map { target ->
                target.copy(
                    weightKg = roundToIncrement(
                        target.weightKg * (1.0 - deload.loadReductionFraction),
                        scheme.roundingIncrementKg,
                    ),
                )
            }
        } else {
            base.targets
        }
        val rationale = if (stalled && deload.enabled) {
            "${base.rationale} A ${deload.stallSessions}-session stall triggered a " +
                "${(deload.loadReductionFraction * 100).toInt()}% load and " +
                "${(deload.volumeReductionFraction * 100).toInt()}% volume deload."
        } else {
            base.rationale
        }
        return ProgressionSuggestion(
            exerciseId = exerciseId,
            exerciseName = latest.exerciseName,
            scheme = scheme.key,
            status = SuggestionStatus.Ready,
            targets = targets,
            rationale = rationale,
            isDeload = stalled && deload.enabled,
            basedOnSessionIds = history.takeLast(maxOf(deload.stallSessions + 1, 1))
                .map(SessionPerformance::sessionId),
            currentEstimated1RmKg = history.maxOfOrNull(SessionPerformance::bestEstimated1RmKg),
        )
    }

    private fun doubleProgression(
        latest: SessionPerformance,
        scheme: DoubleProgression,
    ): BaseSuggestion {
        require(scheme.repRange.first > 0 && scheme.repRange.last >= scheme.repRange.first)
        require(scheme.weightIncrementKg > 0)
        val count = scheme.targetSets ?: latest.sets.size
        require(count > 0)
        val source = latest.sets.normalizedCount(count)
        val completedTop = source.all { it.reps >= scheme.repRange.last }
        val targets = if (completedTop) {
            source.map {
                TargetSet(
                    reps = scheme.repRange.first,
                    weightKg = roundToIncrement(
                        it.weightKg + scheme.weightIncrementKg,
                        scheme.roundingIncrementKg,
                    ),
                )
            }
        } else {
            source.map {
                TargetSet(
                    reps = (it.reps + 1).coerceIn(scheme.repRange),
                    weightKg = roundToIncrement(it.weightKg, scheme.roundingIncrementKg),
                )
            }
        }
        val rationale = if (completedTop) {
            "All target sets reached ${scheme.repRange.last} reps; add ${scheme.weightIncrementKg} kg " +
                "and reset to ${scheme.repRange.first} reps."
        } else {
            "Keep the load and add one rep per set toward ${scheme.repRange.last}."
        }
        return BaseSuggestion(targets, rationale)
    }

    private fun linearProgression(
        latest: SessionPerformance,
        scheme: LinearProgression,
    ): BaseSuggestion {
        require(scheme.targetSets > 0 && scheme.targetReps > 0 && scheme.weightIncrementKg > 0)
        val source = latest.sets.normalizedCount(scheme.targetSets)
        val success = source.all { it.reps >= scheme.targetReps }
        val baseWeight = source.minOfOrNull(StrengthSet::weightKg) ?: 0.0
        val targetWeight = if (success) baseWeight + scheme.weightIncrementKg else baseWeight
        return BaseSuggestion(
            targets = List(scheme.targetSets) {
                TargetSet(
                    reps = scheme.targetReps,
                    weightKg = roundToIncrement(targetWeight, scheme.roundingIncrementKg),
                )
            },
            rationale = if (success) {
                "All ${scheme.targetSets} sets reached ${scheme.targetReps} reps; add " +
                    "${scheme.weightIncrementKg} kg."
            } else {
                "Repeat ${scheme.targetSets}×${scheme.targetReps} at the current load."
            },
        )
    }

    private fun percentageProgression(
        history: List<SessionPerformance>,
        scheme: PercentOfEstimated1Rm,
    ): BaseSuggestion {
        require(scheme.percentage in 0.01..1.5)
        require(scheme.targetSets > 0 && scheme.targetReps > 0)
        val e1rm = history.maxOf(SessionPerformance::bestEstimated1RmKg)
        val targetWeight = roundToIncrement(e1rm * scheme.percentage, scheme.roundingIncrementKg)
        return BaseSuggestion(
            targets = List(scheme.targetSets) {
                TargetSet(reps = scheme.targetReps, weightKg = targetWeight)
            },
            rationale = "${scheme.targetSets}×${scheme.targetReps} at " +
                "${(scheme.percentage * 100).toInt()}% of ${formatKg(e1rm)} kg e1RM.",
        )
    }

    private fun exerciseHistory(
        sessions: Iterable<WorkoutSession>,
        exerciseId: String,
    ): List<SessionPerformance> = sessions.asSequence()
        .filter { it.status == WorkoutSessionStatus.Completed }
        .mapNotNull { session ->
            val entries = session.entries.filter { it.exerciseId == exerciseId }
            val sets = entries.flatMap { it.strengthSets }
                .filter { !it.isWarmup && it.setType != SetType.Warmup && it.reps > 0 }
                .sortedBy(StrengthSet::setNumber)
            if (sets.isEmpty()) return@mapNotNull null
            SessionPerformance(
                sessionId = session.id,
                dateUtc = session.completedDateUtc ?: session.startedAtUtc,
                exerciseName = entries.first().exerciseSnapshotName,
                sets = sets,
                bestEstimated1RmKg = sets.maxOf { Estimates.epley(it.weightKg, it.reps) },
            )
        }
        .sortedWith(compareBy(SessionPerformance::dateUtc, SessionPerformance::sessionId))
        .toList()

    private fun isStalled(
        history: List<SessionPerformance>,
        policy: DeloadPolicy,
    ): Boolean {
        if (!policy.enabled || history.size < policy.stallSessions + 1) return false
        val recent = history.takeLast(policy.stallSessions)
        val prior = history.dropLast(policy.stallSessions)
        val previousBest = prior.maxOf(SessionPerformance::bestEstimated1RmKg)
        return recent.all {
            it.bestEstimated1RmKg <= previousBest * (1.0 + policy.minimumImprovementFraction)
        }
    }

    private fun List<StrengthSet>.normalizedCount(count: Int): List<StrengthSet> {
        if (isEmpty()) return emptyList()
        return List(count) { index -> getOrElse(index) { last() } }
    }

    private fun roundToIncrement(value: Double, increment: Double): Double {
        require(increment > 0) { "roundingIncrementKg must be positive." }
        return round(value / increment) * increment
    }

    private fun formatKg(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(java.util.Locale.ROOT, value)

    private data class SessionPerformance(
        val sessionId: String,
        val dateUtc: Long,
        val exerciseName: String,
        val sets: List<StrengthSet>,
        val bestEstimated1RmKg: Double,
    )

    private data class BaseSuggestion(
        val targets: List<TargetSet>,
        val rationale: String,
    )
}

sealed interface ProgressionScheme {
    val key: String
    val roundingIncrementKg: Double
}

data class DoubleProgression(
    val repRange: IntRange = 8..12,
    val weightIncrementKg: Double = 2.5,
    val targetSets: Int? = null,
    override val roundingIncrementKg: Double = 0.5,
) : ProgressionScheme {
    override val key: String = "double_progression"
}

data class LinearProgression(
    val targetSets: Int = 3,
    val targetReps: Int = 5,
    val weightIncrementKg: Double = 2.5,
    override val roundingIncrementKg: Double = 0.5,
) : ProgressionScheme {
    override val key: String = "linear"
}

data class PercentOfEstimated1Rm(
    val percentage: Double = 0.75,
    val targetSets: Int = 3,
    val targetReps: Int = 8,
    override val roundingIncrementKg: Double = 0.5,
) : ProgressionScheme {
    override val key: String = "percent_e1rm"
}

data class DeloadPolicy(
    val enabled: Boolean = true,
    val stallSessions: Int = 3,
    val minimumImprovementFraction: Double = 0.005,
    val loadReductionFraction: Double = 0.10,
    val volumeReductionFraction: Double = 0.25,
) {
    init {
        require(stallSessions > 0)
        require(minimumImprovementFraction >= 0)
        require(loadReductionFraction in 0.0..<1.0)
        require(volumeReductionFraction in 0.0..<1.0)
    }
}

data class TargetSet(
    val reps: Int,
    val weightKg: Double,
    val setType: SetType = SetType.Normal,
)

data class ProgressionSuggestion(
    val exerciseId: String,
    val exerciseName: String = "",
    val scheme: String,
    val status: SuggestionStatus,
    val targets: List<TargetSet> = emptyList(),
    val rationale: String,
    val isDeload: Boolean = false,
    val basedOnSessionIds: List<String> = emptyList(),
    val currentEstimated1RmKg: Double? = null,
)

enum class SuggestionStatus { Ready, InsufficientHistory }
