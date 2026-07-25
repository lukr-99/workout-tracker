package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.newId
import java.security.MessageDigest
import java.util.Locale

/** Turns a parsed payload into deterministic insert/update/merge/skip actions. */
internal object ImportPlanner {
    fun plan(
        payload: ImportedPayload,
        context: ImportContext,
        options: ImportOptions,
    ): ImportPreview {
        val exerciseRemap = mutableMapOf<String, String>()
        val exercises = planExercises(payload.exercises, context.exercises, options, exerciseRemap)
        val templates = planTemplates(payload.templates, context.templates, options, exerciseRemap)
        val remappedSessions = payload.sessions.map { session -> session.remapExerciseIds(exerciseRemap) }
        val sessions = planSessions(remappedSessions, context.sessions, options)
        val summary = ImportSummary(
            sourceRows = payload.sourceRows,
            parsedSessions = payload.sessions.size,
            insertedSessions = sessions.count { it.action in setOf(PlannedAction.Insert, PlannedAction.KeepBoth) },
            changedSessions = sessions.count {
                it.action in setOf(PlannedAction.Update, PlannedAction.Merge, PlannedAction.Replace)
            },
            skippedSessions = sessions.count { it.action == PlannedAction.Skip },
            insertedExercises = exercises.count {
                it.action in setOf(PlannedAction.Insert, PlannedAction.KeepBoth)
            },
            matchedExercises = exercises.count {
                it.action !in setOf(PlannedAction.Insert, PlannedAction.KeepBoth)
            },
            templates = templates.count { it.action != PlannedAction.Skip },
            setCount = payload.sessions.sumOf { session ->
                session.entries.sumOf { it.strengthSets.size } +
                    session.entries.count { it.cardioData != null }
            },
            dateFromUtc = payload.sessions.minOfOrNull(WorkoutSession::startedAtUtc),
            dateToUtc = payload.sessions.maxOfOrNull(WorkoutSession::startedAtUtc),
            metadata = payload.metadata,
        )
        return ImportPreview(
            ImportPlan(
                format = payload.format,
                exercises = exercises,
                templates = templates,
                sessions = sessions,
                issues = payload.issues,
                sourceLabel = payload.sourceLabel,
            ),
            summary,
        )
    }

    private fun planExercises(
        incoming: List<Exercise>,
        existing: List<Exercise>,
        options: ImportOptions,
        remap: MutableMap<String, String>,
    ): List<PlannedExercise> {
        val byId = existing.associateBy(Exercise::id).toMutableMap()
        val byExternal = existing.mapNotNull { exercise ->
            exercise.externalSourceId?.normalizeKey()?.let { it to exercise }
        }.toMap().toMutableMap()
        val byName = existing.associateBy { it.name.normalizeKey() }.toMutableMap()

        return incoming.map { raw ->
            val target = byId[raw.id]
                ?: raw.externalSourceId?.normalizeKey()?.let(byExternal::get)
                ?: byName[raw.name.normalizeKey()]
            val planned = when {
                target == null -> PlannedExercise(raw, PlannedAction.Insert, reason = "New exercise")
                options.catalogConflictPolicy == ConflictPolicy.Skip ->
                    PlannedExercise(target, PlannedAction.Skip, target.id, "Catalog match")
                options.catalogConflictPolicy == ConflictPolicy.Replace -> {
                    val replacement = raw.copy(id = target.id)
                    PlannedExercise(replacement, PlannedAction.Replace, target.id, "Catalog match")
                }
                options.catalogConflictPolicy == ConflictPolicy.KeepBoth -> {
                    val copy = raw.copy(id = newId(), name = uniqueCopyName(raw.name, byName.keys))
                    PlannedExercise(copy, PlannedAction.KeepBoth, reason = "Kept both catalog records")
                }
                else -> {
                    val merged = mergeExercise(target, raw)
                    val action = if (merged == target) PlannedAction.Skip else PlannedAction.Merge
                    PlannedExercise(merged, action, target.id, "Catalog match")
                }
            }
            remap[raw.id] = planned.value.id
            if (planned.action != PlannedAction.Skip || target != null) {
                byId[planned.value.id] = planned.value
                byName[planned.value.name.normalizeKey()] = planned.value
                planned.value.externalSourceId?.normalizeKey()?.let { byExternal[it] = planned.value }
            }
            planned
        }
    }

    private fun planTemplates(
        incoming: List<WorkoutTemplate>,
        existing: List<WorkoutTemplate>,
        options: ImportOptions,
        exerciseRemap: Map<String, String>,
    ): List<PlannedTemplate> {
        val byId = existing.associateBy(WorkoutTemplate::id)
        val byName = existing.associateBy { it.name.normalizeKey() }
        return incoming.map { template ->
            val remapped = template.copy(
                exercises = template.exercises.map { child ->
                    child.copy(exerciseId = exerciseRemap[child.exerciseId] ?: child.exerciseId)
                },
            )
            val target = byId[template.id] ?: byName[template.name.normalizeKey()]
            when {
                target == null -> PlannedTemplate(remapped, PlannedAction.Insert, reason = "New template")
                options.catalogConflictPolicy == ConflictPolicy.Skip ->
                    PlannedTemplate(target, PlannedAction.Skip, target.id, "Template match")
                options.catalogConflictPolicy == ConflictPolicy.Replace ->
                    PlannedTemplate(remapped.copy(id = target.id), PlannedAction.Replace, target.id, "Template match")
                options.catalogConflictPolicy == ConflictPolicy.KeepBoth ->
                    PlannedTemplate(cloneTemplate(remapped), PlannedAction.KeepBoth, reason = "Kept both templates")
                else -> PlannedTemplate(
                    mergeTemplate(target, remapped),
                    PlannedAction.Merge,
                    target.id,
                    "Template match",
                )
            }
        }
    }

    private fun planSessions(
        incoming: List<WorkoutSession>,
        existing: List<WorkoutSession>,
        options: ImportOptions,
    ): List<PlannedSession> {
        val byId = existing.associateBy(WorkoutSession::id)
        val byFingerprint = existing.associateBy(SessionFingerprint::of)
        val byIdentity = existing.groupBy(SessionFingerprint::identity)
        return incoming.map { session ->
            val exact = byFingerprint[SessionFingerprint.of(session)]
            val target = byId[session.id] ?: exact
                ?: byIdentity[SessionFingerprint.identity(session)]?.firstOrNull()
            when {
                target == null -> PlannedSession(session, PlannedAction.Insert, reason = "New session")
                exact != null && options.sessionConflictPolicy != ConflictPolicy.KeepBoth ->
                    PlannedSession(target, PlannedAction.Skip, target.id, "Exact duplicate")
                options.sessionConflictPolicy == ConflictPolicy.Skip ->
                    PlannedSession(target, PlannedAction.Skip, target.id, "Date/title conflict")
                options.sessionConflictPolicy == ConflictPolicy.Replace ->
                    PlannedSession(
                        session.reparent(target.id),
                        PlannedAction.Replace,
                        target.id,
                        "Date/title conflict",
                    )
                options.sessionConflictPolicy == ConflictPolicy.KeepBoth ->
                    PlannedSession(cloneSession(session), PlannedAction.KeepBoth, reason = "Kept both sessions")
                else -> PlannedSession(
                    mergeSession(target, session),
                    PlannedAction.Merge,
                    target.id,
                    "Date/title conflict",
                )
            }
        }
    }

    private fun mergeExercise(existing: Exercise, incoming: Exercise): Exercise = existing.copy(
        name = incoming.name.ifBlank { existing.name },
        category = incoming.category,
        primaryBodyPart = incoming.primaryBodyPart.ifBlank { existing.primaryBodyPart },
        secondaryBodyParts = (existing.secondaryBodyParts + incoming.secondaryBodyParts)
            .filter(String::isNotBlank)
            .distinctBy { it.normalizeKey() },
        equipment = incoming.equipment.ifBlank { existing.equipment },
        notes = listOf(existing.notes, incoming.notes).filter(String::isNotBlank).distinct()
            .joinToString("\n"),
        externalSourceId = existing.externalSourceId ?: incoming.externalSourceId,
        isArchived = existing.isArchived && incoming.isArchived,
        defaultRestSeconds = incoming.defaultRestSeconds ?: existing.defaultRestSeconds,
    )

    private fun mergeTemplate(
        existing: WorkoutTemplate,
        incoming: WorkoutTemplate,
    ): WorkoutTemplate {
        val seen = mutableSetOf<String>()
        val children = (existing.exercises + incoming.exercises)
            .filter { seen.add((it.exerciseId.ifBlank { it.exerciseName }).normalizeKey()) }
            .mapIndexed { index, child -> child.copy(sortOrder = index) }
        return existing.copy(
            name = incoming.name.ifBlank { existing.name },
            notes = listOf(existing.notes, incoming.notes).filter(String::isNotBlank).distinct()
                .joinToString("\n"),
            exercises = children,
        )
    }

    private fun mergeSession(
        existing: WorkoutSession,
        incoming: WorkoutSession,
    ): WorkoutSession {
        val existingByExercise = existing.entries.associateBy {
            (it.exerciseId.ifBlank { it.exerciseSnapshotName }).normalizeKey()
        }.toMutableMap()
        val mergedEntries = existing.entries.toMutableList()
        incoming.entries.forEach { incomingEntry ->
            val key = (incomingEntry.exerciseId.ifBlank { incomingEntry.exerciseSnapshotName }).normalizeKey()
            val old = existingByExercise[key]
            if (old == null) {
                val appended = incomingEntry.reparent(existing.id, newId()).copy(sortOrder = mergedEntries.size)
                mergedEntries += appended
                existingByExercise[key] = appended
            } else {
                val merged = mergeEntry(old, incomingEntry)
                mergedEntries[mergedEntries.indexOfFirst { it.id == old.id }] = merged
                existingByExercise[key] = merged
            }
        }
        return existing.copy(
            name = incoming.name.ifBlank { existing.name },
            startedAtUtc = minOf(existing.startedAtUtc, incoming.startedAtUtc),
            endedAtUtc = listOfNotNull(existing.endedAtUtc, incoming.endedAtUtc).maxOrNull(),
            completedDateUtc = listOfNotNull(existing.completedDateUtc, incoming.completedDateUtc).maxOrNull(),
            durationSeconds = maxOf(existing.durationSeconds, incoming.durationSeconds),
            notes = listOf(existing.notes, incoming.notes).filter(String::isNotBlank).distinct()
                .joinToString("\n"),
            perceivedEffort = incoming.perceivedEffort ?: existing.perceivedEffort,
            bodyweightKg = incoming.bodyweightKg ?: existing.bodyweightKg,
            entries = mergedEntries.mapIndexed { index, entry -> entry.copy(sortOrder = index) },
        )
    }

    private fun mergeEntry(existing: WorkoutEntry, incoming: WorkoutEntry): WorkoutEntry {
        val signatures = existing.strengthSets.mapTo(mutableSetOf()) { it.signature() }
        val sets = existing.strengthSets.toMutableList()
        incoming.strengthSets.forEach { set ->
            if (signatures.add(set.signature())) {
                sets += set.copy(id = newId(), workoutEntryId = existing.id, setNumber = sets.size + 1)
            }
        }
        return existing.copy(
            notes = listOf(existing.notes, incoming.notes).filter(String::isNotBlank).distinct()
                .joinToString("\n"),
            supersetGroup = incoming.supersetGroup ?: existing.supersetGroup,
            strengthSets = sets.mapIndexed { index, set -> set.copy(setNumber = index + 1) },
            cardioData = mergeCardio(existing.cardioData, incoming.cardioData, existing.id),
        )
    }

    private fun mergeCardio(
        existing: CardioEntryData?,
        incoming: CardioEntryData?,
        entryId: String,
    ): CardioEntryData? = when {
        existing == null -> incoming?.copy(workoutEntryId = entryId)
        incoming == null -> existing
        else -> existing.copy(
            durationSeconds = maxOf(existing.durationSeconds, incoming.durationSeconds),
            distanceKm = listOfNotNull(existing.distanceKm, incoming.distanceKm).maxOrNull(),
            calories = listOfNotNull(existing.calories, incoming.calories).maxOrNull(),
            notes = listOf(existing.notes, incoming.notes).filter(String::isNotBlank).distinct()
                .joinToString("\n"),
        )
    }

    private fun WorkoutSession.remapExerciseIds(remap: Map<String, String>): WorkoutSession = copy(
        entries = entries.map { entry -> entry.copy(exerciseId = remap[entry.exerciseId] ?: entry.exerciseId) },
    )

    private fun WorkoutSession.reparent(sessionId: String): WorkoutSession = copy(
        id = sessionId,
        entries = entries.map { it.reparent(sessionId, it.id) },
    )

    private fun WorkoutEntry.reparent(sessionId: String, entryId: String): WorkoutEntry = copy(
        id = entryId,
        workoutSessionId = sessionId,
        strengthSets = strengthSets.map { it.copy(workoutEntryId = entryId) },
        cardioData = cardioData?.copy(workoutEntryId = entryId),
    )

    private fun cloneSession(source: WorkoutSession): WorkoutSession {
        val sessionId = newId()
        return source.copy(
            id = sessionId,
            entries = source.entries.map { entry -> entry.reparent(sessionId, newId()) }
                .map { entry ->
                    entry.copy(strengthSets = entry.strengthSets.map { it.copy(id = newId()) })
                },
        )
    }

    private fun cloneTemplate(source: WorkoutTemplate): WorkoutTemplate = source.copy(
        id = newId(),
        name = "${source.name} (imported)",
        exercises = source.exercises.map { it.copy(id = newId()) },
    )

    private fun uniqueCopyName(name: String, existingKeys: Set<String>): String {
        var candidate = "$name (imported)"
        var suffix = 2
        while (candidate.normalizeKey() in existingKeys) candidate = "$name (imported $suffix)".also { suffix++ }
        return candidate
    }

    private fun StrengthSet.signature(): String = listOf(
        reps,
        String.format(Locale.ROOT, "%.6f", weightKg),
        durationSeconds ?: 0,
        setType.ordinal,
        isWarmup,
    ).joinToString(":")

    private fun String.normalizeKey(): String = lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}

object SessionFingerprint {
    fun identity(session: WorkoutSession): String =
        "${session.name.normalize()}|${session.startedAtUtc / 60_000}"

    fun of(session: WorkoutSession): String {
        val raw = buildString {
            append(identity(session)).append('|').append(session.durationSeconds)
            session.entries.sortedBy(WorkoutEntry::sortOrder).forEach { entry ->
                append('|').append(entry.exerciseSnapshotName.normalize())
                append(':').append(entry.entryType.ordinal)
                entry.strengthSets.sortedBy(StrengthSet::setNumber).forEach { set ->
                    append(':').append(set.reps)
                    append('@').append(String.format(Locale.ROOT, "%.6f", set.weightKg))
                    append('/').append(set.durationSeconds ?: 0)
                    append('/').append(set.setType.ordinal)
                }
                entry.cardioData?.let {
                    append(":c").append(it.durationSeconds)
                    append('/').append(String.format(Locale.ROOT, "%.6f", it.distanceKm ?: 0.0))
                }
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun String.normalize(): String = lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()
}
