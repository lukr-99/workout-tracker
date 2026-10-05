package com.lukr99.workout.domain

/**
 * The newest [limit] finished workouts that logged [exerciseId], newest first. Warm-up sets are
 * left out, and so are sets with no reps.
 */
fun List<WorkoutSession>.recentOutings(exerciseId: String, limit: Int = 3): List<ExerciseOuting> {
    if (exerciseId.isBlank()) return emptyList()
    return asSequence()
        .filter { it.status == WorkoutSessionStatus.Completed }
        .sortedByDescending { it.completedDateUtc ?: it.startedAtUtc }
        .mapNotNull { session ->
            val sets = session.entries
                .filter { it.exerciseId == exerciseId }
                .flatMap { it.strengthSets }
                .filter { it.reps > 0 && !it.isWarmup && SetTag.Warmup !in it.effectiveTags }
            if (sets.isEmpty()) {
                null
            } else {
                ExerciseOuting(
                    atUtc = session.completedDateUtc ?: session.startedAtUtc,
                    sets = sets,
                    bestE1rmKg = sets.maxOf { Estimates.epley(it.weightKg, it.reps) },
                )
            }
        }
        .take(limit)
        .toList()
}

/**
 * This exercise in a workout swapped for [exercise], for when the machine is taken. The sets, notes
 * and timing stay; the name, category and body part snapshot come from the new exercise.
 */
fun WorkoutEntry.replacedWith(exercise: Exercise): WorkoutEntry = copy(
    exerciseId = exercise.id,
    exerciseSnapshotName = exercise.name,
    exerciseSnapshotCategory = exercise.category,
    exerciseSnapshotPrimaryBodyPart = exercise.primaryBodyPart,
)
