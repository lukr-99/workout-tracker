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
 * The single API over workout persistence: the **only** class that touches Room for exercises,
 * templates and workouts. It keeps the history-safe rules:
 *
 * - **Snapshot on log.** [newEntryForExercise] copies the exercise's name, category and body part
 *   onto the entry, so later catalog edits never rewrite the past.
 * - **Template to workout.** [createWorkoutSession] copies template exercises into entries. The
 *   workout keeps no live link to the template.
 * - **Archive, not delete.** [archiveExercise] flags rows. Catalog exercises are never removed.
 *   Locations follow the same rule ([setLocationArchived]).
 * - **Scope by location.** Last time's sets, notes and progress for an exercise tracked per
 *   location only read workouts at that place ([LocationScope]). The rows never change.
 *
 * The rules themselves live elsewhere so they can be tested without a database: entity mapping in
 * `WorkoutEntityMapping.kt`, the copies in `domain/WorkoutSnapshots.kt`, catalog filtering in
 * [ExerciseFilter] and the external catalog merge in [ExternalExerciseMerge].
 */
class WorkoutRepository(
    private val dao: WorkoutDao,
    private val transactions: TransactionRunner = DirectTransactionRunner,
) {

    // --- Seeding -------------------------------------------------------------------------------

    /**
     * Insert the starter catalog once, on an empty DB (mirrors MAUI `InitializeAsync`), and the
     * built-in Home and Outdoors locations whenever they are missing.
     */
    suspend fun ensureSeeded() {
        if (dao.countExercises() == 0) {
            dao.upsertExercises(Seed.exercises().map { it.toEntity() })
        }
        dao.insertMissingLocations(Location.builtIns.map { it.toEntity() })
    }

    // --- Exercises -----------------------------------------------------------------------------

    /** Catalog stream with the MAUI in-memory filter (search / body-part / category / archived). */
    fun observeExercises(filter: ExerciseFilter = ExerciseFilter()): Flow<List<Exercise>> =
        dao.observeExercises().map { rows -> filter.apply(rows.map { it.toDomain() }) }

    suspend fun getExercises(filter: ExerciseFilter = ExerciseFilter()): List<Exercise> =
        filter.apply(dao.getAllExercises().map { it.toDomain() })

    suspend fun getExercise(id: String): Exercise? = dao.getExercise(id)?.toDomain()

    suspend fun saveExercise(exercise: Exercise): Exercise {
        val normalized = exercise.normalized()
        dao.upsertExercise(normalized.toEntity())
        return normalized
    }

    suspend fun archiveExercise(id: String) = dao.archiveExercise(id)

    /**
     * Track [exerciseId] per location or shared. Only a flag changes: its history is read through a
     * different [LocationScope], so nothing is lost either way.
     */
    suspend fun setTrackPerLocation(exerciseId: String, perLocation: Boolean) =
        dao.setTrackPerLocation(exerciseId, perLocation)

    /** "Don't ask again" for the per-location prompt on [exerciseId]. */
    suspend fun muteSplitPrompt(exerciseId: String) = dao.muteSplitPrompt(exerciseId)

    /** Which workouts count for [exerciseId] in a workout at [locationId]. */
    suspend fun locationScope(exerciseId: String, locationId: String?): LocationScope =
        LocationScope.of(getExercise(exerciseId), locationId)

    /**
     * Compatibility count for older callers. New sync flows should use
     * [mergeExternalExercisesDetailed] to distinguish inserts, safe updates, and skips.
     */
    suspend fun mergeExternalExercises(exercises: List<Exercise>): Int =
        mergeExternalExercisesDetailed(exercises).changed

    /** Additively merge an external catalog. [ExternalExerciseMerge] decides what changes. */
    suspend fun mergeExternalExercisesDetailed(
        exercises: List<Exercise>,
    ): ExternalExerciseMergeSummary = inTransaction {
        val plan = ExternalExerciseMerge.plan(dao.getAllExercises().map { it.toDomain() }, exercises)
        plan.upserts.forEach { dao.upsertExercise(it.toEntity()) }
        plan.summary
    }

    /**
     * Match every existing row by a conservative normalized name and fill remote artwork only when
     * it is missing. No user-owned catalog field is changed.
     */
    suspend fun backfillMissingExerciseImages(candidates: List<Exercise>): Int = inTransaction {
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

    /** A fresh entry with the exercise's identity snapshotted onto it (the "snapshot on log" rule). */
    fun newEntryForExercise(exercise: Exercise, sortOrder: Int = 0): WorkoutEntry =
        exercise.toNewEntry(sortOrder)

    // --- Templates -----------------------------------------------------------------------------

    fun observeTemplates(): Flow<List<WorkoutTemplate>> =
        dao.observeTemplates().map { rows -> rows.map { it.toDomain() } }

    suspend fun getTemplates(): List<WorkoutTemplate> =
        dao.getAllTemplates().map { it.toDomain() }

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

    suspend fun deleteTemplate(id: String) = dao.deleteTemplate(id)

    /** When a workout from this template was last finished, or null if never. */
    suspend fun getTemplateLastDone(templateId: String): Long? =
        dao.getCompletedSessions().map { it.toDomain() }
            .filter { it.templateId == templateId }
            .maxOfOrNull { it.completedDateUtc ?: it.startedAtUtc }

    /** When each template was last finished, by template id, from one pass over finished workouts. */
    suspend fun getTemplatesLastDone(): Map<String, Long> =
        dao.getCompletedSessions().map { it.toDomain() }
            .filter { !it.templateId.isNullOrBlank() }
            .groupBy { it.templateId.orEmpty() }
            .mapValues { (_, sessions) -> sessions.maxOf { it.completedDateUtc ?: it.startedAtUtc } }

    /**
     * Last time's sets for each of [exerciseIds], from the newest finished workout that logged it.
     * An exercise tracked per location only looks at workouts at [locationId].
     */
    suspend fun getLastSets(exerciseIds: Collection<String>, locationId: String?): Map<String, List<StrengthSet>> {
        val ids = exerciseIds.filter(String::isNotBlank).toSet()
        if (ids.isEmpty()) return emptyMap()
        val sessions = getSessions()
        val exercises = dao.getAllExercises().filter { it.id in ids }.associate { it.id to it.toDomain() }
        return ids.associateWith { id -> LocationScope.of(exercises[id], locationId).apply(sessions).lastSetsFor(id) }
            .filterValues { it.isNotEmpty() }
    }

    // --- Sessions ------------------------------------------------------------------------------

    /**
     * Start a session, instantiating from a template if given. Returns any existing active one. The
     * new workout is at the last used location ([getLastUsedLocationId]).
     */
    suspend fun createWorkoutSession(templateId: String? = null, name: String? = null): WorkoutSession {
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

    /**
     * The latest "Last time" note for each of [exerciseIds], from finished workouts other than
     * [currentSessionId]. Exercises without an earlier note are left out of the map. An exercise
     * tracked per location only reads notes from workouts at [locationId].
     */
    suspend fun getPreviousEntryNotes(
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

    fun observeActiveSession(): Flow<WorkoutSession?> =
        dao.observeActiveSession().map { it?.toDomain() }

    suspend fun getActiveSession(): WorkoutSession? = dao.getActiveSession()?.toDomain()

    suspend fun getSession(id: String): WorkoutSession? = dao.getSession(id)?.toDomain()

    /** Full session graph for data services. Discarded sessions are opt-in. */
    suspend fun getSessions(includeDiscarded: Boolean = false): List<WorkoutSession> {
        val sessions = if (includeDiscarded) dao.getAllSessions() else dao.getExportableSessions()
        return sessions.map { it.toDomain() }
    }

    fun observeSession(id: String): Flow<WorkoutSession?> =
        dao.observeSession(id).map { it?.toDomain() }

    suspend fun saveWorkoutSession(session: WorkoutSession): WorkoutSession = inTransaction {
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

    suspend fun deleteWorkoutSession(id: String) = dao.deleteSession(id)

    // --- Locations -----------------------------------------------------------------------------

    /** Every location, archived ones included. Callers pick with `forPicker()`. */
    fun observeLocations(): Flow<List<Location>> =
        dao.observeLocations().map { rows -> rows.map { it.toDomain() } }

    suspend fun getLocations(): List<Location> = dao.getLocations().map { it.toDomain() }

    suspend fun getLocation(id: String): Location? = dao.getLocation(id)?.toDomain()

    /** Add or rename a location. A blank name is refused. */
    suspend fun saveLocation(location: Location): Location {
        val name = location.name.trim()
        require(name.isNotEmpty()) { "A location needs a name." }
        val saved = location.copy(id = location.id.ifBlank { newId() }, name = name)
        dao.upsertLocation(saved.toEntity())
        return saved
    }

    /** Archived places leave the picker; workouts there keep pointing at them. */
    suspend fun setLocationArchived(id: String, archived: Boolean) = dao.setLocationArchived(id, archived)

    /** The place of the newest workout that has one, unless that place is archived. */
    suspend fun getLastUsedLocationId(): String? = dao.getLastUsedLocationId()

    /** Move [sessionIds] to [locationId] (null is "Unknown location"). Returns how many moved. */
    suspend fun moveWorkoutsToLocation(sessionIds: Collection<String>, locationId: String?): Int {
        val ids = sessionIds.filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return 0
        return inTransaction { ids.chunked(SQL_VARIABLE_CHUNK).sumOf { dao.setSessionLocation(it, locationId) } }
    }

    /** Give every workout that has no location [locationId]. Returns how many moved. */
    suspend fun assignUnknownWorkoutsTo(locationId: String): Int = dao.assignUnknownSessionLocations(locationId)

    /** Finished workouts with no location. */
    suspend fun countWorkoutsWithoutLocation(): Int = dao.countCompletedSessionsWithoutLocation()

    /**
     * Removes every workout, template and exercise. Callers wrap it in a transaction together with
     * whatever replaces the data, and reseed with [ensureSeeded] when nothing replaces it.
     */
    suspend fun deleteAllWorkoutData() {
        dao.deleteAllSessions()
        dao.deleteAllTemplates()
        dao.deleteAllExercises()
        dao.deleteAllLocations()
        dao.insertMissingLocations(Location.builtIns.map { it.toEntity() })
    }

    suspend fun countExercises(): Int = dao.countExercises()

    suspend fun countTemplates(): Int = dao.countTemplates()

    /** Finished and live workouts; discarded ones are not counted. */
    suspend fun countWorkouts(): Int = dao.countKeptSessions()

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

    suspend fun getExerciseProgress(
        exerciseId: String,
        scope: LocationScope = LocationScope.Shared,
    ): List<ExerciseAnalyticsPoint> = Progression.forExercise(scope.apply(completedSessions()), exerciseId)

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
        return saveTemplate(session.toTemplate(templateName))
    }

    private suspend fun completedSessions(): List<WorkoutSession> =
        dao.getCompletedSessions().map { it.toDomain() }

    // --- Export / import (own bundle; round-trip + restore) ------------------------------------

    suspend fun createExportBundle(): ExportBundle = ExportBundle(
        exercises = dao.getAllExercises().map { it.toDomain() },
        templates = dao.getAllTemplates().map { it.toDomain() },
        sessions = dao.getExportableSessions().map { it.toDomain() }.sortedByDescending { it.startedAtUtc },
        locations = getLocations(),
    )

    /** Restore a supported bundle into the DB, preserving ids (upsert). */
    suspend fun importBundle(bundle: ExportBundle) = inTransaction {
        dao.upsertExercises(bundle.exercises.map { it.toEntity() })
        bundle.locations.forEach { dao.upsertLocation(it.toEntity()) }
        for (template in bundle.templates) saveTemplate(template)
        for (session in bundle.sessions) saveWorkoutSession(session)
    }

    /** Execute a multi-record service operation atomically. */
    suspend fun <T> inTransaction(block: suspend WorkoutRepository.() -> T): T =
        transactions.run { block(this@WorkoutRepository) }

    private companion object {
        /** Stays under SQLite's bound-variable limit on older Android versions (999). */
        const val SQL_VARIABLE_CHUNK = 500
    }
}
