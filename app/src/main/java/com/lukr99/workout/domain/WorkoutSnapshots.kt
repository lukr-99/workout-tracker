package com.lukr99.workout.domain

/*
 * Copies between the catalog, templates and workouts. Each copy takes a snapshot, so a later edit
 * to the catalog or a template never rewrites a workout that already happened.
 */

/** A fresh exercise in a workout with the exercise's name, category and body part copied onto it. */
fun Exercise.toNewEntry(sortOrder: Int = 0): WorkoutEntry = WorkoutEntry(
    exerciseId = id,
    exerciseSnapshotName = name,
    exerciseSnapshotCategory = category,
    exerciseSnapshotPrimaryBodyPart = primaryBodyPart,
    sortOrder = sortOrder,
    entryType = category,
    strengthSets = firstSetsFor(category),
    cardioData = firstCardioFor(category),
)

/** The template's exercises as exercises in workout [sessionId], in template order, notes kept. */
fun WorkoutTemplate.toEntries(sessionId: String): List<WorkoutEntry> =
    exercises.sortedBy { it.sortOrder }.mapIndexed { index, ex ->
        WorkoutEntry(
            workoutSessionId = sessionId,
            exerciseId = ex.exerciseId,
            exerciseSnapshotName = ex.exerciseName,
            exerciseSnapshotCategory = ex.category,
            exerciseSnapshotPrimaryBodyPart = ex.bodyPart,
            sortOrder = index,
            entryType = ex.category,
            notes = ex.notes,
            strengthSets = firstSetsFor(ex.category),
            cardioData = firstCardioFor(ex.category),
        )
    }

/**
 * A new template with this workout's catalog exercises in order. It is named [templateName], or
 * "<workout> Copy" when that is blank.
 */
fun WorkoutSession.toTemplate(templateName: String? = null): WorkoutTemplate = WorkoutTemplate(
    name = templateName?.trim().takeUnless { it.isNullOrBlank() } ?: "$name Copy",
    notes = "Created from workout on ${formatIsoDate(completedDateUtc ?: startedAtUtc)}",
    exercises = entries
        .sortedBy { it.sortOrder }
        .filter { it.exerciseId.isNotBlank() }
        .mapIndexed { index, entry ->
            WorkoutTemplateExercise(
                exerciseId = entry.exerciseId,
                exerciseName = entry.exerciseSnapshotName,
                category = entry.entryType,
                bodyPart = entry.exerciseSnapshotPrimaryBodyPart,
                sortOrder = index,
                notes = entry.notes,
            )
        },
)

private fun firstSetsFor(category: ExerciseCategory): List<StrengthSet> =
    if (category == ExerciseCategory.Strength) listOf(StrengthSet(setNumber = 1)) else emptyList()

private fun firstCardioFor(category: ExerciseCategory): CardioEntryData? =
    if (category == ExerciseCategory.Cardio) CardioEntryData() else null
