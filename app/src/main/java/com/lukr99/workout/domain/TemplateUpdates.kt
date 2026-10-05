package com.lukr99.workout.domain

/*
 * Keeping a template in step with how its workouts actually went. "Planned" counts come from the
 * template's targetSets, or one set when it has none, which is how the workout started.
 */

/** What the finished [workout] did differently from this template: added, skipped, other set counts. */
fun WorkoutTemplate.changesIn(workout: WorkoutSession): List<TemplateChange> {
    val done = workout.loggedEntries()
    val planned = exercises.sortedBy { it.sortOrder }
    val plannedIds = planned.map { it.exerciseId }.toSet()
    val doneIds = done.map { it.exerciseId }.toSet()
    val changes = mutableListOf<TemplateChange>()
    done.filter { it.exerciseId !in plannedIds }.forEach { changes += TemplateChange.Added(it.exerciseSnapshotName) }
    planned.filter { it.exerciseId !in doneIds }.forEach { changes += TemplateChange.Skipped(it.exerciseName) }
    planned.forEach { ex ->
        val entry = done.firstOrNull { it.exerciseId == ex.exerciseId } ?: return@forEach
        if (!entry.isStrength) return@forEach
        val sets = entry.strengthSets.size
        val target = ex.targetSets ?: 1
        if (sets != target) changes += TemplateChange.SetCount(ex.exerciseName, done = sets, planned = target)
    }
    return changes
}

/**
 * This template rebuilt from the finished [workout]: its exercises in the order they were done, each
 * with the number of sets logged. Rep ranges, rest, notes and plan details are kept for exercises
 * that were already in the template; supersets follow the workout.
 */
fun WorkoutTemplate.updatedFrom(workout: WorkoutSession): WorkoutTemplate {
    val previous = exercises.associateBy { it.exerciseId }
    return copy(
        exercises = workout.loggedEntries().mapIndexed { index, entry ->
            val kept = previous[entry.exerciseId]
            (kept ?: WorkoutTemplateExercise(
                exerciseId = entry.exerciseId,
                exerciseName = entry.exerciseSnapshotName,
                category = entry.entryType,
                bodyPart = entry.exerciseSnapshotPrimaryBodyPart,
            )).copy(
                sortOrder = index,
                supersetGroup = entry.supersetGroup,
                targetSets = if (entry.isStrength) entry.strengthSets.size.coerceAtLeast(1) else kept?.targetSets,
            )
        },
    )
}

/** Catalog exercises that were actually logged: strength with sets, or cardio with data. */
private fun WorkoutSession.loggedEntries(): List<WorkoutEntry> =
    entries.sortedBy { it.sortOrder }
        .filter { it.exerciseId.isNotBlank() }
        .filter { it.strengthSets.isNotEmpty() || it.cardioData != null }
