package com.lukr99.workout.domain

/*
 * Copies between the catalog, templates and workouts. Each copy takes a snapshot, so a later edit
 * to the catalog or a template never rewrites a workout that already happened.
 */

/** The name a workout gets when it is not started from a template and nobody named it. */
const val DEFAULT_WORKOUT_NAME = "Quick Workout"

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

/**
 * The template's exercises as exercises in workout [sessionId], in template order, with their notes
 * and supersets. A planned exercise starts with its target number of sets, each set to the top of
 * the rep range; an unplanned one starts with a single empty set.
 */
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
            supersetGroup = ex.supersetGroup,
            strengthSets = plannedSetsFor(ex),
            cardioData = firstCardioFor(ex.category),
        )
    }

private fun plannedSetsFor(ex: WorkoutTemplateExercise): List<StrengthSet> {
    if (ex.category != ExerciseCategory.Strength) return emptyList()
    val count = ex.targetSets?.coerceIn(1, 20) ?: return firstSetsFor(ex.category)
    val reps = ex.repsMax ?: ex.repsMin ?: 0
    return (1..count).map { StrengthSet(setNumber = it, reps = reps) }
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

/**
 * This workout with [past]'s exercises, in order and with its supersets, each starting with the
 * sets done last time: the same reps, weight and tags, none of them logged yet. Notes stay behind,
 * since they were about that day. It takes [past]'s name while this one still has the default.
 */
fun WorkoutSession.repeating(past: WorkoutSession): WorkoutSession = copy(
    name = if (name == DEFAULT_WORKOUT_NAME) past.name else name,
    entries = past.entries
        .sortedBy { it.sortOrder }
        .filter { it.exerciseId.isNotBlank() }
        .mapIndexed { index, entry ->
            WorkoutEntry(
                workoutSessionId = id,
                exerciseId = entry.exerciseId,
                exerciseSnapshotName = entry.exerciseSnapshotName,
                exerciseSnapshotCategory = entry.exerciseSnapshotCategory,
                exerciseSnapshotPrimaryBodyPart = entry.exerciseSnapshotPrimaryBodyPart,
                sortOrder = index,
                entryType = entry.entryType,
                supersetGroup = entry.supersetGroup,
                weightUnitOverride = entry.weightUnitOverride,
                strengthSets = entry.strengthSets.sortedBy { it.setNumber }.mapIndexed { i, set ->
                    StrengthSet(
                        setNumber = i + 1,
                        reps = set.reps,
                        weightKg = set.weightKg,
                        isWarmup = set.isWarmup,
                        durationSeconds = set.durationSeconds,
                        setType = set.setType,
                        tags = set.tags,
                    )
                }.ifEmpty { firstSetsFor(entry.entryType) },
                cardioData = firstCardioFor(entry.entryType),
            )
        },
)

/**
 * This workout started from [template] after all: its name, its link to the template (so finishing
 * can offer to update it), and its exercises as a template start would have them.
 */
fun WorkoutSession.switchedTo(template: WorkoutTemplate): WorkoutSession = copy(
    templateId = template.id,
    name = template.name,
    entries = template.toEntries(id),
)

private fun firstSetsFor(category: ExerciseCategory): List<StrengthSet> =
    if (category == ExerciseCategory.Strength) listOf(StrengthSet(setNumber = 1)) else emptyList()

private fun firstCardioFor(category: ExerciseCategory): CardioEntryData? =
    if (category == ExerciseCategory.Cardio) CardioEntryData() else null
