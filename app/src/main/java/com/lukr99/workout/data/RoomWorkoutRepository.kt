package com.lukr99.workout.data

import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.data.images.ExerciseNameNormalizer
import com.lukr99.workout.domain.Analytics
import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.DEFAULT_WORKOUT_NAME
import com.lukr99.workout.domain.DashboardSnapshot
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseAnalyticsPoint
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.ExerciseLocationNote
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.LocationScope
import com.lukr99.workout.domain.PreviousEntryNote
import com.lukr99.workout.domain.Progression
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.WorkoutSessionSummary
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.lastSetsFor
import com.lukr99.workout.domain.newId
import com.lukr99.workout.domain.normalized
import com.lukr99.workout.domain.toEntries
import com.lukr99.workout.domain.toNewEntry
import com.lukr99.workout.domain.toTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The Room-backed [WorkoutRepository]: the **only** class that touches Room for exercises, templates
 * and workouts. The history-safe rules are documented on [WorkoutRepository]. The rules themselves
 * live elsewhere so they can be tested without a database: entity mapping in
 * `WorkoutEntityMapping.kt`, the copies in `domain/WorkoutSnapshots.kt`, catalog filtering in
 * [ExerciseFilter] and the external catalog merge in [ExternalExerciseMerge].
 */
class RoomWorkoutRepository(
    private val dao: WorkoutDao,
    private val transactions: TransactionRunner = DirectTransactionRunner,
) : WorkoutRepository {

    // --- Seeding -------------------------------------------------------------------------------

    override suspend fun ensureSeeded() {
        if (dao.countExercises() == 0) {
            dao.upsertExercises(Seed.exercises().map { it.toEntity() })
        }
        dao.insertMissingLocations(Location.builtIns.map { it.toEntity() })
    }

    // --- Exercises -----------------------------------------------------------------------------

    override fun observeExercises(filter: ExerciseFilter): Flow<List<Exercise>> =
        dao.observeExercises().map { rows -> filter.apply(rows.map { it.toDomain() }) }

    override suspend fun getExercises(filter: ExerciseFilter): List<Exercise> =
        filter.apply(dao.getAllExercises().map { it.toDomain() })

    override suspend fun getExercise(id: String): Exercise? = dao.getExercise(id)?.toDomain()

    override suspend fun saveExercise(exercise: Exercise): Exercise {
        val normalized = exercise.normalized()
        dao.upsertExercise(normalized.toEntity())
        return normalized
    }

    override suspend fun archiveExercise(id: String) = dao.archiveExercise(id)

    override suspend fun setTrackPerLocation(exerciseId: String, perLocation: Boolean) =
        dao.setTrackPerLocation(exerciseId, perLocation)

    override suspend fun muteSplitPrompt(exerciseId: String) = dao.muteSplitPrompt(exerciseId)

    override suspend fun locationScope(exerciseId: String, locationId: String?): LocationScope =
        LocationScope.of(getExercise(exerciseId), locationId)

    override suspend fun mergeExternalExercises(exercises: List<Exercise>): Int =
        mergeExternalExercisesDetailed(exercises).changed

    override suspend fun mergeExternalExercisesDetailed(
        exercises: List<Exercise>,
    ): ExternalExerciseMergeSummary = inTransaction {
        val plan = ExternalExerciseMerge.plan(dao.getAllExercises().map { it.toDomain() }, exercises)
        plan.upserts.forEach { dao.upsertExercise(it.toEntity()) }
        plan.summary
    }

    override suspend fun backfillMissingExerciseImages(candidates: List<Exercise>): Int = inTransaction {
        val imagesByName = candidates.asSequence()
            .filter { !it.imageUrl.isNullOrBlank() }
            .associateBy { ExerciseNameNormalizer.normalize(it.name) }
        var filled = 0
        dao.getAllExercises().forEach { row ->
            if (!row.imageUrl.isNullOrBlank()) return@forEach
            val candidate = imagesByName[ExerciseNameNormalizer.normalize(row.name)]
                ?: return@forEach
            dao.upsertExercise(
                row.copy(
                    imageUrl = candidate.imageUrl,
                    imageAttribution = candidate.imageAttribution,
                ),
            )
            filled++
        }
        filled
    }

    override fun newEntryForExercise(exercise: Exercise, sortOrder: Int): WorkoutEntry =
        exercise.toNewEntry(sortOrder)

    // --- Templates -----------------------------------------------------------------------------

    override fun observeTemplates(): Flow<List<WorkoutTemplate>> =
        dao.observeTemplates().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getTemplates(): List<WorkoutTemplate> =
        dao.getAllTemplates().map { it.toDomain() }

    override suspend fun getTemplate(id: String): WorkoutTemplate? = dao.getTemplate(id)?.toDomain()

    override suspend fun saveTemplate(template: WorkoutTemplate): WorkoutTemplate {
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
                    targetSets = ex.targetSets?.coerceIn(1, 20),
                    repsMin = ex.repsMin?.coerceIn(1, 100),
                    repsMax = ex.repsMax?.coerceIn(1, 100),
                    restSeconds = ex.restSeconds?.coerceIn(0, 3_600),
                    supersetGroup = ex.supersetGroup,
                )
            }
        if (children.isNotEmpty()) dao.upsertTemplateExercises(children)

        return getTemplate(id) ?: template.copy(id = id, name = name)
    }

    override suspend fun deleteTemplate(id: String) = dao.deleteTemplate(id)

    override suspend fun getTemplateLastDone(templateId: String): Long? =
        dao.getCompletedSessions().map { it.toDomain() }
            .filter { it.templateId == templateId }
            .maxOfOrNull { it.completedDateUtc ?: it.startedAtUtc }

    override suspend fun getTemplatesLastDone(): Map<String, Long> =
        dao.getCompletedSessions().map { it.toDomain() }
            .filter { !it.templateId.isNullOrBlank() }
            .groupBy { it.templateId.orEmpty() }
            .mapValues { (_, sessions) -> sessions.maxOf { it.completedDateUtc ?: it.startedAtUtc } }

    override suspend fun getLastSets(exerciseIds: Collection<String>, locationId: String?): Map<String, List<StrengthSet>> {
        val ids = exerciseIds.filter(String::isNotBlank).toSet()
        if (ids.isEmpty()) return emptyMap()
        val sessions = getSessions()
        val exercises = dao.getAllExercises().filter { it.id in ids }.associate { it.id to it.toDomain() }
        return ids.associateWith { id -> LocationScope.of(exercises[id], locationId).apply(sessions).lastSetsFor(id) }
            .filterValues { it.isNotEmpty() }
    }

    // --- Sessions ------------------------------------------------------------------------------

    override suspend fun createWorkoutSession(templateId: String?, name: String?): WorkoutSession {
        dao.getActiveSession()?.let { return it.toDomain() }

        val sessionId = newId()
        var session = WorkoutSession(
            id = sessionId,
            templateId = templateId,
            name = name?.trim().takeUnless { it.isNullOrBlank() } ?: DEFAULT_WORKOUT_NAME,
            startedAtUtc = System.currentTimeMillis(),
            status = WorkoutSessionStatus.Active,
            locationId = getLastUsedLocationId(),
        )

        if (!templateId.isNullOrBlank()) {
            getTemplate(templateId)?.let { template ->
                session = session.copy(
                    name = name?.trim().takeUnless { it.isNullOrBlank() } ?: template.name,
                    entries = template.toEntries(sessionId),
                )
            }
        }

        return saveWorkoutSession(session)
    }

    override suspend fun getPreviousEntryNotes(
        exerciseIds: Collection<String>,
        currentSessionId: String,
        locationId: String?,
    ): Map<String, PreviousEntryNote> = exerciseIds
        .filter(String::isNotBlank)
        .distinct()
        .mapNotNull { id ->
            val scope = locationScope(id, locationId)
            dao.getLatestEntryNote(id, currentSessionId, !scope.perLocation, scope.locationId)?.let { row ->
                id to PreviousEntryNote(exerciseId = id, text = row.note.trim(), atUtc = row.atUtc)
            }
        }
        .toMap()

    override fun observeActiveSession(): Flow<WorkoutSession?> =
        dao.observeActiveSession().map { it?.toDomain() }

    override suspend fun getActiveSession(): WorkoutSession? = dao.getActiveSession()?.toDomain()

    override suspend fun getSession(id: String): WorkoutSession? = dao.getSession(id)?.toDomain()

    override suspend fun getSessions(includeDiscarded: Boolean): List<WorkoutSession> {
        val sessions = if (includeDiscarded) dao.getAllSessions() else dao.getExportableSessions()
        return sessions.map { it.toDomain() }
    }

    override fun observeSession(id: String): Flow<WorkoutSession?> =
        dao.observeSession(id).map { it?.toDomain() }

    override suspend fun saveWorkoutSession(session: WorkoutSession): WorkoutSession = inTransaction {
        val id = session.id.ifBlank { newId() }
        var normalized = session.copy(
            id = id,
            name = session.name.ifBlank { DEFAULT_WORKOUT_NAME }.trim(),
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
        getSession(id) ?: normalized
    }

    override suspend fun deleteWorkoutSession(id: String) = dao.deleteSession(id)

    // --- Machine notes ---------------------------------------------------------------------------

    override fun observeLocationNote(exerciseId: String, locationId: String): Flow<String> =
        dao.observeLocationNote(exerciseId, locationId).map { it?.note.orEmpty() }

    override fun observeLocationNotesFor(exerciseId: String): Flow<Map<String, String>> =
        dao.observeLocationNotesFor(exerciseId).map { rows -> rows.associate { it.locationId to it.note } }

    override suspend fun getLocationNotes(exerciseIds: Collection<String>, locationId: String): Map<String, String> {
        val ids = exerciseIds.distinct()
        if (ids.isEmpty()) return emptyMap()
        return ids.chunked(SQL_VARIABLE_CHUNK)
            .flatMap { dao.getLocationNotes(it, locationId) }
            .associate { it.exerciseId to it.note }
    }

    override suspend fun getAllLocationNotes(): List<ExerciseLocationNote> = dao.getAllLocationNotes().map { it.toDomain() }

    override suspend fun saveLocationNote(exerciseId: String, locationId: String, note: String) {
        val text = note.trim()
        if (text.isEmpty()) dao.deleteLocationNote(exerciseId, locationId)
        else dao.upsertLocationNote(ExerciseLocationNoteEntity(exerciseId, locationId, text))
    }

    override suspend fun restoreLocationNotes(notes: List<ExerciseLocationNote>) {
        notes.filter { it.note.isNotBlank() }.chunked(SQL_VARIABLE_CHUNK)
            .forEach { dao.insertMissingLocationNotes(it.map(ExerciseLocationNote::toEntity)) }
    }

    // --- Locations -----------------------------------------------------------------------------

    override fun observeLocations(): Flow<List<Location>> =
        dao.observeLocations().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getLocations(): List<Location> = dao.getLocations().map { it.toDomain() }

    override suspend fun getLocation(id: String): Location? = dao.getLocation(id)?.toDomain()

    override suspend fun saveLocation(location: Location): Location {
        val name = location.name.trim()
        require(name.isNotEmpty()) { "A location needs a name." }
        val saved = location.copy(id = location.id.ifBlank { newId() }, name = name)
        dao.upsertLocation(saved.toEntity())
        return saved
    }

    override suspend fun setLocationArchived(id: String, archived: Boolean) = dao.setLocationArchived(id, archived)

    override suspend fun getLastUsedLocationId(): String? = dao.getLastUsedLocationId()

    override suspend fun moveWorkoutsToLocation(sessionIds: Collection<String>, locationId: String?): Int {
        val ids = sessionIds.filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return 0
        return inTransaction { ids.chunked(SQL_VARIABLE_CHUNK).sumOf { dao.setSessionLocation(it, locationId) } }
    }

    override suspend fun assignUnknownWorkoutsTo(locationId: String): Int = dao.assignUnknownSessionLocations(locationId)

    override suspend fun countWorkoutsWithoutLocation(): Int = dao.countCompletedSessionsWithoutLocation()

    override suspend fun deleteAllWorkoutData() {
        dao.deleteAllSessions()
        dao.deleteAllTemplates()
        dao.deleteAllExercises()
        dao.deleteAllLocationNotes()
        dao.deleteAllLocations()
        dao.insertMissingLocations(Location.builtIns.map { it.toEntity() })
    }

    override suspend fun countExercises(): Int = dao.countExercises()

    override suspend fun countTemplates(): Int = dao.countTemplates()

    override suspend fun countWorkouts(): Int = dao.countKeptSessions()

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

    override fun observeHistory(searchText: String?): Flow<List<WorkoutSessionSummary>> =
        dao.observeCompletedSessions().map { rows -> summarize(rows.map { it.toDomain() }, searchText) }

    override suspend fun getWorkoutHistory(searchText: String?): List<WorkoutSessionSummary> =
        summarize(dao.getCompletedSessions().map { it.toDomain() }, searchText)

    private fun summarize(sessions: List<WorkoutSession>, searchText: String?): List<WorkoutSessionSummary> {
        val needle = searchText?.trim().orEmpty()
        return sessions
            .filter { needle.isBlank() || it.name.contains(needle, ignoreCase = true) }
            .map { Analytics.summarize(it) }
    }

    override suspend fun getAnalyticsOverview() = Analytics.overview(completedSessions())

    override suspend fun getConsistencySnapshot() =
        Analytics.consistency(completedSessions().map { it.completedDateUtc ?: it.startedAtUtc })

    override suspend fun getExerciseProgress(
        exerciseId: String,
        scope: LocationScope,
    ): List<ExerciseAnalyticsPoint> = Progression.forExercise(scope.apply(completedSessions()), exerciseId)

    override suspend fun getDashboardSnapshot(): DashboardSnapshot {
        val completed = completedSessions()
        return DashboardSnapshot(
            activeWorkout = getActiveSession(),
            analytics = Analytics.overview(completed),
            consistency = Analytics.consistency(completed.map { it.completedDateUtc ?: it.startedAtUtc }),
            recentWorkouts = completed.map { Analytics.summarize(it) }.take(5),
        )
    }

    override suspend fun duplicateWorkoutAsTemplate(sessionId: String, templateName: String?): WorkoutTemplate {
        val session = getSession(sessionId) ?: error("Workout session not found.")
        return saveTemplate(session.toTemplate(templateName))
    }

    private suspend fun completedSessions(): List<WorkoutSession> =
        dao.getCompletedSessions().map { it.toDomain() }

    // --- Export / import (own bundle; round-trip + restore) ------------------------------------

    override suspend fun createExportBundle(): ExportBundle = ExportBundle(
        exercises = dao.getAllExercises().map { it.toDomain() },
        templates = dao.getAllTemplates().map { it.toDomain() },
        sessions = dao.getExportableSessions().map { it.toDomain() }.sortedByDescending { it.startedAtUtc },
        locations = getLocations(),
        locationNotes = getAllLocationNotes(),
    )

    override suspend fun importBundle(bundle: ExportBundle) = inTransaction {
        dao.upsertExercises(bundle.exercises.map { it.toEntity() })
        bundle.locations.forEach { dao.upsertLocation(it.toEntity()) }
        bundle.locationNotes.forEach { dao.upsertLocationNote(it.toEntity()) }
        for (template in bundle.templates) saveTemplate(template)
        for (session in bundle.sessions) saveWorkoutSession(session)
    }

    override suspend fun <T> inTransaction(block: suspend WorkoutRepository.() -> T): T =
        transactions.run { block(this@RoomWorkoutRepository) }

    private companion object {
        /** Stays under SQLite's bound-variable limit on older Android versions (999). */
        const val SQL_VARIABLE_CHUNK = 500
    }
}
