package com.lukr99.workout.data

import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.domain.Analytics
import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.DashboardSnapshot
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseAnalyticsPoint
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.Progression
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.WorkoutSessionSummary
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.WorkoutTemplateExercise
import com.lukr99.workout.domain.newId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * The single API over persistence — the **only** class that touches Room and files
 * (see 01-architecture.md). It maps the Room `@Entity`/`@Relation` shapes to/from the portable
 * `domain/` model, applies the MAUI behaviour surface, and enforces the history-safe rules from
 * 03-data-model.md:
 *
 * - **Snapshot on log** — [newEntryForExercise] copies the exercise's name/category/body-part onto
 *   the entry, so later catalog edits never rewrite the past.
 * - **Template → session** — [createWorkoutSession] copies template exercises into entries; the
 *   session keeps no live FK to the template.
 * - **Archive, not delete** — [archiveExercise] flags rows; catalog exercises are never removed.
 *
 * Ported from `WorkoutTracker.Core/Data/WorkoutTrackerRepository.cs`.
 */
class WorkoutRepository(private val dao: WorkoutDao) {

    private val json = Json { ignoreUnknownKeys = true }
    private val stringListSerializer = ListSerializer(String.serializer())

    // --- Seeding -------------------------------------------------------------------------------

    /** Insert the starter catalog once, on an empty DB (mirrors MAUI `InitializeAsync`). */
    suspend fun ensureSeeded() {
        if (dao.countExercises() == 0) {
            dao.upsertExercises(Seed.exercises().map { it.toEntity() })
        }
    }

    // --- Exercises -----------------------------------------------------------------------------

    /** Catalog stream with the MAUI in-memory filter (search / body-part / category / archived). */
    fun observeExercises(filter: ExerciseFilter = ExerciseFilter()): Flow<List<Exercise>> =
        dao.observeExercises().map { rows -> rows.map { it.toDomain() }.applyFilter(filter) }

    suspend fun getExercises(filter: ExerciseFilter = ExerciseFilter()): List<Exercise> =
        dao.getAllExercises().map { it.toDomain() }.applyFilter(filter)

    suspend fun getExercise(id: String): Exercise? = dao.getExercise(id)?.toDomain()

    suspend fun saveExercise(exercise: Exercise): Exercise {
        val normalized = exercise.normalized()
        dao.upsertExercise(normalized.toEntity())
        return normalized
    }

    suspend fun archiveExercise(id: String) = dao.archiveExercise(id)

    /** Merge external (wger-style) exercises by `externalSourceId`, tagging them `Synced`. */
    suspend fun mergeExternalExercises(exercises: List<Exercise>): Int {
        val byExternalId = dao.getAllExercises()
            .filter { !it.externalSourceId.isNullOrBlank() }
            .associateBy({ it.externalSourceId!!.lowercase() }, { it.id })

        var merged = 0
        for (raw in exercises) {
            val normalized = raw.normalized().copy(source = ExerciseSource.Synced)
            val existingId = normalized.externalSourceId
                ?.takeIf { it.isNotBlank() }
                ?.let { byExternalId[it.lowercase()] }
            dao.upsertExercise(normalized.copy(id = existingId ?: normalized.id).toEntity())
            merged++
        }
        return merged
    }

    /** A fresh entry with the exercise's identity snapshotted onto it (the "snapshot on log" rule). */
    fun newEntryForExercise(exercise: Exercise, sortOrder: Int = 0): WorkoutEntry = WorkoutEntry(
        exerciseId = exercise.id,
        exerciseSnapshotName = exercise.name,
        exerciseSnapshotCategory = exercise.category,
        exerciseSnapshotPrimaryBodyPart = exercise.primaryBodyPart,
        sortOrder = sortOrder,
        entryType = exercise.category,
        strengthSets = if (exercise.category == ExerciseCategory.Strength) listOf(StrengthSet(setNumber = 1)) else emptyList(),
        cardioData = if (exercise.category == ExerciseCategory.Cardio) CardioEntryData() else null,
    )

    // --- Templates -----------------------------------------------------------------------------

    fun observeTemplates(): Flow<List<WorkoutTemplate>> =
        dao.observeTemplates().map { rows -> rows.map { it.toDomain() } }

    suspend fun getTemplate(id: String): WorkoutTemplate? = dao.getTemplate(id)?.toDomain()

    suspend fun saveTemplate(template: WorkoutTemplate): WorkoutTemplate {
        val id = template.id.ifBlank { newId() }
        val name = template.name.ifBlank { "Untitled Template" }.trim()

        dao.upsertTemplate(TemplateEntity(id = id, name = name, notes = template.notes))
        dao.deleteTemplateExercises(id)

        val children = template.exercises
            .sortedBy { it.sortOrder }
            .mapIndexed { index, ex ->
                TemplateExerciseEntity(
                    id = ex.id.ifBlank { newId() },
                    templateId = id,
                    exerciseId = ex.exerciseId,
                    exerciseName = ex.exerciseName,
                    category = ex.category,
                    bodyPart = ex.bodyPart,
                    sortOrder = index,
                    notes = ex.notes,
                )
            }
        if (children.isNotEmpty()) dao.upsertTemplateExercises(children)

        return getTemplate(id) ?: template.copy(id = id, name = name)
    }

    suspend fun deleteTemplate(id: String) = dao.deleteTemplate(id)

    // --- Sessions ------------------------------------------------------------------------------

    /** Start a session, instantiating from a template if given. Returns any existing active one. */
    suspend fun createWorkoutSession(templateId: String? = null, name: String? = null): WorkoutSession {
        dao.getActiveSession()?.let { return it.toDomain() }

        val sessionId = newId()
        var session = WorkoutSession(
            id = sessionId,
            templateId = templateId,
            name = name?.trim().takeUnless { it.isNullOrBlank() } ?: "Quick Workout",
            startedAtUtc = System.currentTimeMillis(),
            status = WorkoutSessionStatus.Active,
        )

        if (!templateId.isNullOrBlank()) {
            getTemplate(templateId)?.let { template ->
                session = session.copy(
                    name = name?.trim().takeUnless { it.isNullOrBlank() } ?: template.name,
                    entries = template.exercises.sortedBy { it.sortOrder }.mapIndexed { index, ex ->
                        WorkoutEntry(
                            workoutSessionId = sessionId,
                            exerciseId = ex.exerciseId,
                            exerciseSnapshotName = ex.exerciseName,
                            exerciseSnapshotCategory = ex.category,
                            exerciseSnapshotPrimaryBodyPart = ex.bodyPart,
                            sortOrder = index,
                            entryType = ex.category,
                            notes = ex.notes,
                            strengthSets = if (ex.category == ExerciseCategory.Strength) listOf(StrengthSet(setNumber = 1)) else emptyList(),
                            cardioData = if (ex.category == ExerciseCategory.Cardio) CardioEntryData() else null,
                        )
                    },
                )
            }
        }

        return saveWorkoutSession(session)
    }

    fun observeActiveSession(): Flow<WorkoutSession?> =
        dao.observeActiveSession().map { it?.toDomain() }

    suspend fun getActiveSession(): WorkoutSession? = dao.getActiveSession()?.toDomain()

    suspend fun getSession(id: String): WorkoutSession? = dao.getSession(id)?.toDomain()

    fun observeSession(id: String): Flow<WorkoutSession?> =
        dao.observeSession(id).map { it?.toDomain() }

    suspend fun saveWorkoutSession(session: WorkoutSession): WorkoutSession {
        val id = session.id.ifBlank { newId() }
        var normalized = session.copy(
            id = id,
            name = session.name.ifBlank { "Quick Workout" }.trim(),
        )

        if (normalized.status == WorkoutSessionStatus.Completed) {
            val ended = normalized.endedAtUtc ?: System.currentTimeMillis()
            val completed = normalized.completedDateUtc ?: ended
            val duration = if (normalized.durationSeconds > 0) normalized.durationSeconds
            else ((ended - normalized.startedAtUtc) / 1000).coerceAtLeast(0)
            normalized = normalized.copy(endedAtUtc = ended, completedDateUtc = completed, durationSeconds = duration)
        }

        dao.upsertSession(normalized.toEntity())
        replaceSessionChildren(normalized)
        return getSession(id) ?: normalized
    }

    suspend fun deleteWorkoutSession(id: String) = dao.deleteSession(id)

    private suspend fun replaceSessionChildren(session: WorkoutSession) {
        dao.deleteEntriesForSession(session.id) // cascade clears old sets + cardio

        val entryRows = mutableListOf<EntryEntity>()
        val setRows = mutableListOf<StrengthSetEntity>()
        val cardioRows = mutableListOf<CardioDataEntity>()

        session.entries.sortedBy { it.sortOrder }.forEachIndexed { index, entry ->
            val entryId = entry.id.ifBlank { newId() }
            entryRows += entry.copy(id = entryId, workoutSessionId = session.id, sortOrder = index).toEntity()

            if (entry.entryType == ExerciseCategory.Strength) {
                entry.strengthSets.forEachIndexed { setIndex, set ->
                    setRows += set.copy(
                        id = set.id.ifBlank { newId() },
                        workoutEntryId = entryId,
                        setNumber = setIndex + 1,
                    ).toEntity()
                }
            } else {
                val cardio = entry.cardioData ?: CardioEntryData()
                cardioRows += cardio.copy(workoutEntryId = entryId).toEntity()
            }
        }

        if (entryRows.isNotEmpty()) dao.upsertEntries(entryRows)
        if (setRows.isNotEmpty()) dao.upsertStrengthSets(setRows)
        if (cardioRows.isNotEmpty()) dao.upsertCardio(cardioRows)
    }

    // --- History + analytics -------------------------------------------------------------------

    fun observeHistory(searchText: String? = null): Flow<List<WorkoutSessionSummary>> =
        dao.observeCompletedSessions().map { rows -> summarize(rows.map { it.toDomain() }, searchText) }

    suspend fun getWorkoutHistory(searchText: String? = null): List<WorkoutSessionSummary> =
        summarize(dao.getCompletedSessions().map { it.toDomain() }, searchText)

    private fun summarize(sessions: List<WorkoutSession>, searchText: String?): List<WorkoutSessionSummary> {
        val needle = searchText?.trim().orEmpty()
        return sessions
            .filter { needle.isBlank() || it.name.contains(needle, ignoreCase = true) }
            .map { Analytics.summarize(it) }
    }

    suspend fun getAnalyticsOverview() = Analytics.overview(completedSessions())

    suspend fun getConsistencySnapshot() =
        Analytics.consistency(completedSessions().map { it.completedDateUtc ?: it.startedAtUtc })

    suspend fun getExerciseProgress(exerciseId: String): List<ExerciseAnalyticsPoint> =
        Progression.forExercise(completedSessions(), exerciseId)

    suspend fun getDashboardSnapshot(): DashboardSnapshot {
        val completed = completedSessions()
        return DashboardSnapshot(
            activeWorkout = getActiveSession(),
            analytics = Analytics.overview(completed),
            consistency = Analytics.consistency(completed.map { it.completedDateUtc ?: it.startedAtUtc }),
            recentWorkouts = completed.map { Analytics.summarize(it) }.take(5),
        )
    }

    /** Turn a completed session into a reusable template (ported from MAUI). */
    suspend fun duplicateWorkoutAsTemplate(sessionId: String, templateName: String? = null): WorkoutTemplate {
        val session = getSession(sessionId) ?: error("Workout session not found.")
        val template = WorkoutTemplate(
            name = templateName?.trim().takeUnless { it.isNullOrBlank() } ?: "${session.name} Copy",
            notes = "Created from workout on ${com.lukr99.workout.domain.formatIsoDate(session.completedDateUtc ?: session.startedAtUtc)}",
            exercises = session.entries
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
        return saveTemplate(template)
    }

    private suspend fun completedSessions(): List<WorkoutSession> =
        dao.getCompletedSessions().map { it.toDomain() }

    // --- Export / import (own bundle; round-trip + restore) ------------------------------------

    suspend fun createExportBundle(): ExportBundle = ExportBundle(
        exercises = dao.getAllExercises().map { it.toDomain() },
        templates = dao.getAllTemplates().map { it.toDomain() },
        sessions = dao.getExportableSessions().map { it.toDomain() }.sortedByDescending { it.startedAtUtc },
    )

    /** Restore a bundle into the DB, preserving ids (upsert). Accepts `1.0` and `1.1`. */
    suspend fun importBundle(bundle: ExportBundle) {
        dao.upsertExercises(bundle.exercises.map { it.toEntity() })
        for (template in bundle.templates) saveTemplate(template)
        for (session in bundle.sessions) saveWorkoutSession(session)
    }

    // --- Mapping: entity <-> domain ------------------------------------------------------------

    private fun ExerciseEntity.toDomain() = Exercise(
        id = id,
        name = name,
        category = category,
        primaryBodyPart = primaryBodyPart,
        secondaryBodyParts = decodeList(secondaryBodyPartsJson),
        equipment = equipment,
        notes = notes,
        source = source,
        externalSourceId = externalSourceId,
        isArchived = isArchived,
        defaultRestSeconds = defaultRestSeconds,
    )

    private fun Exercise.toEntity() = ExerciseEntity(
        id = id,
        name = name,
        category = category,
        primaryBodyPart = primaryBodyPart,
        secondaryBodyPartsJson = encodeList(secondaryBodyParts),
        equipment = equipment,
        notes = notes,
        source = source,
        externalSourceId = externalSourceId,
        isArchived = isArchived,
        defaultRestSeconds = defaultRestSeconds,
    )

    private fun TemplateWithExercises.toDomain() = WorkoutTemplate(
        id = template.id,
        name = template.name,
        notes = template.notes,
        exercises = exercises.sortedBy { it.sortOrder }.map {
            WorkoutTemplateExercise(
                id = it.id,
                exerciseId = it.exerciseId,
                exerciseName = it.exerciseName,
                category = it.category,
                bodyPart = it.bodyPart,
                sortOrder = it.sortOrder,
                notes = it.notes,
            )
        },
    )

    private fun WorkoutSession.toEntity() = SessionEntity(
        id = id,
        templateId = templateId,
        name = name,
        status = status,
        startedAtUtc = startedAtUtc,
        endedAtUtc = endedAtUtc,
        completedDateUtc = completedDateUtc,
        durationSeconds = durationSeconds,
        notes = notes,
        perceivedEffort = perceivedEffort,
        bodyweightKg = bodyweightKg,
    )

    private fun WorkoutEntry.toEntity() = EntryEntity(
        id = id,
        workoutSessionId = workoutSessionId,
        exerciseId = exerciseId,
        exerciseSnapshotName = exerciseSnapshotName.ifBlank { "Exercise" },
        exerciseSnapshotCategory = exerciseSnapshotCategory,
        exerciseSnapshotPrimaryBodyPart = exerciseSnapshotPrimaryBodyPart,
        sortOrder = sortOrder,
        entryType = entryType,
        notes = notes,
        supersetGroup = supersetGroup,
    )

    private fun StrengthSet.toEntity() = StrengthSetEntity(
        id = id,
        workoutEntryId = workoutEntryId,
        setNumber = setNumber,
        reps = reps,
        weightKg = weightKg,
        rir = rir,
        rpe = rpe,
        performedAtUtc = performedAtUtc,
        notes = notes,
        isWarmup = isWarmup,
        isPr = isPr,
        durationSeconds = durationSeconds,
        setType = setType,
    )

    private fun CardioEntryData.toEntity() = CardioDataEntity(
        workoutEntryId = workoutEntryId,
        durationSeconds = durationSeconds,
        distanceKm = distanceKm,
        calories = calories,
        notes = notes,
    )

    private fun SessionWithEntries.toDomain() = WorkoutSession(
        id = session.id,
        templateId = session.templateId,
        name = session.name,
        startedAtUtc = session.startedAtUtc,
        endedAtUtc = session.endedAtUtc,
        completedDateUtc = session.completedDateUtc,
        durationSeconds = session.durationSeconds,
        notes = session.notes,
        status = session.status,
        perceivedEffort = session.perceivedEffort,
        bodyweightKg = session.bodyweightKg,
        entries = entries.sortedBy { it.entry.sortOrder }.map { it.toDomain() },
    )

    private fun EntryWithSets.toDomain() = WorkoutEntry(
        id = entry.id,
        workoutSessionId = entry.workoutSessionId,
        exerciseId = entry.exerciseId,
        exerciseSnapshotName = entry.exerciseSnapshotName,
        exerciseSnapshotCategory = entry.exerciseSnapshotCategory,
        exerciseSnapshotPrimaryBodyPart = entry.exerciseSnapshotPrimaryBodyPart,
        sortOrder = entry.sortOrder,
        entryType = entry.entryType,
        notes = entry.notes,
        supersetGroup = entry.supersetGroup,
        strengthSets = strengthSets.sortedBy { it.setNumber }.map { it.toDomain() },
        cardioData = cardio?.toDomain(),
    )

    private fun StrengthSetEntity.toDomain() = StrengthSet(
        id = id,
        workoutEntryId = workoutEntryId,
        setNumber = setNumber,
        reps = reps,
        weightKg = weightKg,
        rir = rir,
        rpe = rpe,
        performedAtUtc = performedAtUtc,
        notes = notes,
        isWarmup = isWarmup,
        isPr = isPr,
        durationSeconds = durationSeconds,
        setType = setType,
    )

    private fun CardioDataEntity.toDomain() = CardioEntryData(
        workoutEntryId = workoutEntryId,
        durationSeconds = durationSeconds,
        distanceKm = distanceKm,
        calories = calories,
        notes = notes,
    )

    // --- Helpers -------------------------------------------------------------------------------

    private fun List<Exercise>.applyFilter(filter: ExerciseFilter): List<Exercise> {
        var result = asSequence()
        if (!filter.includeArchived) result = result.filter { !it.isArchived }

        val needle = filter.searchText.trim()
        if (needle.isNotBlank()) {
            result = result.filter {
                it.name.contains(needle, ignoreCase = true) ||
                    it.primaryBodyPart.contains(needle, ignoreCase = true) ||
                    it.equipment.contains(needle, ignoreCase = true)
            }
        }

        val bodyPart = filter.bodyPart.trim()
        if (bodyPart.isNotBlank()) {
            result = result.filter {
                it.primaryBodyPart.equals(bodyPart, ignoreCase = true) ||
                    it.secondaryBodyParts.any { part -> part.equals(bodyPart, ignoreCase = true) }
            }
        }

        filter.category?.let { category -> result = result.filter { it.category == category } }
        return result.sortedWith(compareBy({ it.category.ordinal }, { it.name })).toList()
    }

    private fun Exercise.normalized(): Exercise = copy(
        id = id.ifBlank { newId() },
        name = name.ifBlank { "Custom Exercise" }.trim(),
        primaryBodyPart = primaryBodyPart.ifBlank {
            if (category == ExerciseCategory.Cardio) "Cardio" else "Full Body"
        }.trim(),
        secondaryBodyParts = secondaryBodyParts
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .distinctBy { it.lowercase() },
    )

    private fun encodeList(list: List<String>): String = json.encodeToString(stringListSerializer, list)
    private fun decodeList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList()
        else runCatching { json.decodeFromString(stringListSerializer, value) }.getOrDefault(emptyList())
}
