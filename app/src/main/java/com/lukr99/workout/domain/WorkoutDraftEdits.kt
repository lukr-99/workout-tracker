package com.lukr99.workout.domain

/*
 * Edits to a live workout while it is logged. Each one returns a new workout and leaves the input
 * alone. Times are passed in, so the rules can be tested without a clock.
 */

/** This workout with [transform] applied to exercise [entryId]. Other exercises stay the same. */
fun WorkoutSession.withEntry(entryId: String, transform: (WorkoutEntry) -> WorkoutEntry): WorkoutSession =
    copy(entries = entries.map { if (it.id == entryId) transform(it) else it })

/** This workout with [transform] applied to set [setId] of exercise [entryId]. */
fun WorkoutSession.withSet(
    entryId: String,
    setId: String,
    transform: (StrengthSet) -> StrengthSet,
): WorkoutSession = withEntry(entryId) { entry ->
    entry.copy(strengthSets = entry.strengthSets.map { if (it.id == setId) transform(it) else it })
}

/** Moves exercise [entryId] one place up or down. Nothing changes at either end of the list. */
fun WorkoutSession.withEntryMoved(entryId: String, up: Boolean): WorkoutSession {
    val list = entries.toMutableList()
    val from = list.indexOfFirst { it.id == entryId }
    val to = if (up) from - 1 else from + 1
    if (from < 0 || to !in list.indices) return this
    list.add(to, list.removeAt(from))
    return copy(entries = list.mapIndexed { index, entry -> entry.copy(sortOrder = index) })
}

/** Stamps the start of exercise [entryId], unless it already has one. */
fun WorkoutSession.withEntryStarted(entryId: String, now: Long): WorkoutSession =
    withEntry(entryId) { it.copy(startedAtUtc = it.startedAtUtc ?: now) }

/** Marks exercise [entryId] finished at [now]. A missing start falls back to its first done set. */
fun WorkoutSession.withEntryFinished(entryId: String, now: Long): WorkoutSession =
    withEntry(entryId) { it.finishedAt(now) }

/** Adds a set that copies the reps and weight of the last one. */
fun WorkoutEntry.withSetAdded(): WorkoutEntry {
    val last = strengthSets.lastOrNull()
    val next = StrengthSet(
        id = newId(),
        workoutEntryId = id,
        setNumber = strengthSets.size + 1,
        reps = last?.reps ?: 0,
        weightKg = last?.weightKg ?: 0.0,
        setType = SetType.Normal,
    )
    return copy(strengthSets = strengthSets + next)
}

/** Switches the weight unit of one exercise between kilograms and pounds. */
fun WorkoutEntry.withWeightUnitToggled(poundsByDefault: Boolean): WorkoutEntry {
    val currentlyPounds = when (weightUnitOverride) {
        WeightDisplayUnit.Pounds -> true
        WeightDisplayUnit.Kilograms -> false
        null -> poundsByDefault
    }
    return copy(weightUnitOverride = if (currentlyPounds) WeightDisplayUnit.Kilograms else WeightDisplayUnit.Pounds)
}

/** Turns [tag] on or off and keeps the old single set type in step for export. */
fun StrengthSet.withTagToggled(tag: SetTag): StrengthSet {
    val tags = effectiveTags.toMutableSet().apply { if (!add(tag)) remove(tag) }.toSet()
    return copy(tags = tags, isWarmup = SetTag.Warmup in tags, setType = legacySetType(tags))
}

/**
 * Marks set [setId] done at [doneAt], or not done when [doneAt] is null. Marking a set done also
 * starts its exercise if it had not started yet.
 */
fun WorkoutSession.withSetDone(entryId: String, setId: String, doneAt: Long?): WorkoutSession =
    withEntry(entryId) { entry ->
        entry.copy(
            startedAtUtc = if (doneAt != null) entry.startedAtUtc ?: doneAt else entry.startedAtUtc,
            strengthSets = entry.strengthSets.map { if (it.id == setId) it.copy(performedAtUtc = doneAt) else it },
        )
    }

/** The ids of every set already marked done. */
val WorkoutSession.setIdsMarkedDone: Set<String>
    get() = entries.flatMap { it.strengthSets }.filter { it.performedAtUtc != null }.map { it.id }.toSet()

/**
 * The finished workout as it is saved: exercises without sets or cardio data are dropped, and every
 * exercise gets a start and an end.
 */
fun WorkoutSession.completedAt(now: Long): WorkoutSession = copy(
    status = WorkoutSessionStatus.Completed,
    endedAtUtc = now,
    completedDateUtc = now,
    entries = entries
        .filter { it.strengthSets.isNotEmpty() || it.cardioData != null }
        .map { entry -> entry.finishedAt(now).copy(completedAtUtc = entry.completedAtUtc ?: now) },
)

/**
 * Fresh copies of the sets from the most recent finished workout that logged [exerciseId], to
 * prefill a new exercise. Empty when there is none.
 */
fun List<WorkoutSession>.lastSetsFor(exerciseId: String): List<StrengthSet> {
    if (exerciseId.isBlank()) return emptyList()
    val sets = asSequence()
        .filter { it.status == WorkoutSessionStatus.Completed }
        .sortedByDescending { it.completedDateUtc ?: it.startedAtUtc }
        .mapNotNull { session ->
            session.entries.firstOrNull { it.exerciseId == exerciseId && it.strengthSets.isNotEmpty() }
        }
        .firstOrNull()
        ?.strengthSets
        ?: return emptyList()
    return sets.mapIndexed { i, s ->
        StrengthSet(
            id = newId(),
            setNumber = i + 1,
            reps = s.reps,
            weightKg = s.weightKg,
            isWarmup = s.isWarmup,
            setType = s.setType,
            tags = s.tags,
        )
    }
}

private fun WorkoutEntry.finishedAt(now: Long): WorkoutEntry = copy(
    startedAtUtc = startedAtUtc ?: strengthSets.mapNotNull(StrengthSet::performedAtUtc).minOrNull() ?: now,
    completedAtUtc = now,
)

private fun legacySetType(tags: Set<SetTag>): SetType = when {
    SetTag.Warmup in tags -> SetType.Warmup
    SetTag.Drop in tags -> SetType.Drop
    SetTag.ToFailure in tags -> SetType.Failure
    SetTag.Negative in tags -> SetType.Negative
    SetTag.BackOff in tags -> SetType.BackOff
    else -> SetType.Normal
}
