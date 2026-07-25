package com.lukr99.workout.domain

/**
 * Per-exercise progression series, ported from the MAUI `GetExerciseProgressAsync`.
 *
 * One [ExerciseAnalyticsPoint] per completed session that trained the exercise, in chronological
 * order — best working weight, total volume, and total reps for that day. Pure over the domain
 * models; the repository supplies the completed sessions.
 */
object Progression {

    fun forExercise(
        completedSessions: List<WorkoutSession>,
        exerciseId: String,
    ): List<ExerciseAnalyticsPoint> {
        return completedSessions
            .sortedBy { it.completedDateUtc ?: it.startedAtUtc }
            .mapNotNull { session ->
                val sets = session.entries
                    .filter { it.exerciseId == exerciseId }
                    .flatMap { it.strengthSets }
                if (sets.isEmpty()) return@mapNotNull null

                ExerciseAnalyticsPoint(
                    exerciseId = exerciseId,
                    dateUtc = session.completedDateUtc ?: session.startedAtUtc,
                    bestWeightKg = sets.maxOf { it.weightKg },
                    totalVolumeKg = sets.sumOf { it.weightKg * it.reps },
                    totalReps = sets.sumOf { it.reps },
                    sessionName = session.name,
                )
            }
    }
}
