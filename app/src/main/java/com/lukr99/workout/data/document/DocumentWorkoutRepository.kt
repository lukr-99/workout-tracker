package com.lukr99.workout.data.document

import com.lukr99.workout.data.ExternalExerciseMerge
import com.lukr99.workout.data.ExternalExerciseMergeSummary
import com.lukr99.workout.data.Seed
import com.lukr99.workout.data.WorkoutRepository
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
import com.lukr99.workout.domain.nowMillis
import com.lukr99.workout.domain.toEntries
import com.lukr99.workout.domain.toNewEntry
import com.lukr99.workout.domain.toTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The [WorkoutRepository] over a [DocumentDatabase]: the web edition's storage for exercises,
 * templates, workouts, locations and machine notes. It keeps the same rules and the same orders as
 * the Room implementation, so the shared screens and services behave the same on both platforms
 * (the contract tests in app/src/test run both).
 *
 * Every write is a [DocumentChange] whose ids and times are chosen first, so it can be applied
 * again after a conflict with another tab.
 */
class DocumentWorkoutRepository(
    private val database: DocumentDatabase,
    private val now: () -> Long = ::nowMillis,
) : WorkoutRepository {

    private suspend fun read(): WorkoutDocument = database.current()

    private fun <T> observe(select: (WorkoutDocument) -> T): Flow<T> =
        database.document.map(select).distinctUntilChanged()

    // --- Seeding -------------------------------------------------------------------------------

    override suspend fun ensureSeeded() {
        val seeds = Seed.exercises()
        database.update { document ->
            document.edit {
                val catalog = if (exercises.isEmpty()) seeds else exercises
                val missing = Location.builtIns.filter { builtIn -> locations.none { it.id == builtIn.id } }
                copy(exercises = catalog, locations = locations + missing)
            }
        }
    }

    // --- Exercises -----------------------------------------------------------------------------

    override fun observeExercises(filter: ExerciseFilter): Flow<List<Exercise>> =
        observe { filter.apply(it.data.exercises) }

    override suspend fun getExercises(filter: ExerciseFilter): List<Exercise> = filter.apply(read().data.exercises)

    override suspend fun getExercise(id: String): Exercise? = read().data.exercises.firstOrNull { it.id == id }

    override suspend fun saveExercise(exercise: Exercise): Exercise {
        val normalized = exercise.normalized()
        database.update { it.edit { upsertExercise(normalized) } }
        return normalized
    }

    override suspend fun archiveExercise(id: String) =
        database.update { it.edit { mapExercise(id) { exercise -> exercise.copy(isArchived = true) } } }

    override suspend fun setTrackPerLocation(exerciseId: String, perLocation: Boolean) =
        database.update { it.edit { mapExercise(exerciseId) { exercise -> exercise.copy(trackPerLocation = perLocation) } } }

    override suspend fun muteSplitPrompt(exerciseId: String) =
        database.update { it.edit { mapExercise(exerciseId) { exercise -> exercise.copy(splitPromptMuted = true) } } }

    override suspend fun locationScope(exerciseId: String, locationId: String?): LocationScope =
        LocationScope.of(getExercise(exerciseId), locationId)

    override suspend fun mergeExternalExercises(exercises: List<Exercise>): Int =
        mergeExternalExercisesDetailed(exercises).changed

    override suspend fun mergeExternalExercisesDetailed(
        exercises: List<Exercise>,
    ): ExternalExerciseMergeSummary = inTransaction {
        val plan = ExternalExerciseMerge.plan(read().sortedExercises(), exercises)
        database.update { document -> document.edit { copy(exercises = this.exercises.upsertAll(plan.upserts) { it.id }) } }
        plan.summary
    }

    override suspend fun backfillMissingExerciseImages(candidates: List<Exercise>): Int {
        val imagesByName = candidates.asSequence()
            .filter { !it.imageUrl.isNullOrBlank() }
            .associateBy { ExerciseNameNormalizer.normalize(it.name) }
        fun fill(row: Exercise): Exercise {
            if (!row.imageUrl.isNullOrBlank()) return row
            val candidate = imagesByName[ExerciseNameNormalizer.normalize(row.name)] ?: return row
            return row.copy(imageUrl = candidate.imageUrl, imageAttribution = candidate.imageAttribution)
        }
        return inTransaction {
            val filled = read().data.exercises.count { fill(it) != it }
            database.update { document -> document.edit { copy(exercises = exercises.map(::fill)) } }
            filled
        }
    }

    override fun newEntryForExercise(exercise: Exercise, sortOrder: Int): WorkoutEntry = exercise.toNewEntry(sortOrder)

    // --- Templates -----------------------------------------------------------------------------

    override fun observeTemplates(): Flow<List<WorkoutTemplate>> = observe { it.sortedTemplates() }

    override suspend fun getTemplates(): List<WorkoutTemplate> = read().sortedTemplates()

    override suspend fun getTemplate(id: String): WorkoutTemplate? = read().data.templates.firstOrNull { it.id == id }

    override suspend fun saveTemplate(template: WorkoutTemplate): WorkoutTemplate {
        val id = template.id.ifBlank { newId() }
        val saved = WorkoutTemplate(
            id = id,
            name = template.name.ifBlank { "Untitled Template" }.trim(),
            notes = template.notes,
            exercises = template.exercises
                .sortedBy { it.sortOrder }
                .mapIndexed { index, ex ->
                    ex.copy(
                        id = ex.id.ifBlank { newId() },
                        sortOrder = index,
                        targetSets = ex.targetSets?.coerceIn(1, 20),
                        repsMin = ex.repsMin?.coerceIn(1, 100),
                        repsMax = ex.repsMax?.coerceIn(1, 100),
                        restSeconds = ex.restSeconds?.coerceIn(0, 3_600),
                    )
                },
        )
        database.update { it.edit { upsertTemplate(saved) } }
        return getTemplate(id) ?: saved
    }

    override suspend fun deleteTemplate(id: String) =
        database.update { document -> document.edit { copy(templates = templates.filterNot { it.id == id }) } }

    override suspend fun getTemplateLastDone(templateId: String): Long? =
        read().completedSessions()
            .filter { it.templateId == templateId }
            .maxOfOrNull { it.completedDateUtc ?: it.startedAtUtc }

    override suspend fun getTemplatesLastDone(): Map<String, Long> =
        read().completedSessions()
            .filter { !it.templateId.isNullOrBlank() }
            .groupBy { it.templateId.orEmpty() }
            .mapValues { (_, sessions) -> sessions.maxOf { it.completedDateUtc ?: it.startedAtUtc } }

    override suspend fun getLastSets(exerciseIds: Collection<String>, locationId: String?): Map<String, List<StrengthSet>> {
        val ids = exerciseIds.filter(String::isNotBlank).toSet()
        if (ids.isEmpty()) return emptyMap()
        val document = read()
        val sessions = document.keptSessions()
        val exercises = document.data.exercises.filter { it.id in ids }.associateBy { it.id }
        return ids.associateWith { id -> LocationScope.of(exercises[id], locationId).apply(sessions).lastSetsFor(id) }
            .filterValues { it.isNotEmpty() }
    }

    // --- Sessions ------------------------------------------------------------------------------

    override suspend fun createWorkoutSession(templateId: String?, name: String?): WorkoutSession {
        getActiveSession()?.let { return it }

        val sessionId = newId()
        var session = WorkoutSession(
            id = sessionId,
            templateId = templateId,
            name = name?.trim().takeUnless { it.isNullOrBlank() } ?: DEFAULT_WORKOUT_NAME,
            startedAtUtc = now(),
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
        val normalized = normalize(session)
        // Only one live workout: if another tab started one meanwhile, that one stays and is returned.
        database.update { document ->
            if (document.activeSession() != null) document else document.edit { upsertSession(normalized) }
        }
        return getActiveSession() ?: normalized
    }

    override suspend fun getPreviousEntryNotes(
        exerciseIds: Collection<String>,
        currentSessionId: String,
        locationId: String?,
    ): Map<String, PreviousEntryNote> {
        val document = read()
        val finished = document.data.sessions.filter {
            it.status == WorkoutSessionStatus.Completed && it.id != currentSessionId
        }
        return exerciseIds
            .filter(String::isNotBlank)
            .distinct()
            .mapNotNull { id ->
                val scope = LocationScope.of(document.data.exercises.firstOrNull { it.id == id }, locationId)
                finished.asSequence()
                    .filter { !scope.perLocation || it.locationId == scope.locationId }
                    .flatMap { session ->
                        session.entries.asSequence()
                            .filter { it.exerciseId == id && it.notes.isNotBlank() }
                            .map { entry -> entry.notes to (session.completedDateUtc ?: session.startedAtUtc) }
                    }
                    .maxByOrNull { it.second }
                    ?.let { (note, at) -> id to PreviousEntryNote(exerciseId = id, text = note.trim(), atUtc = at) }
            }
            .toMap()
    }

    override fun observeActiveSession(): Flow<WorkoutSession?> = observe { it.activeSession() }

    override suspend fun getActiveSession(): WorkoutSession? = read().activeSession()

    override suspend fun getSession(id: String): WorkoutSession? = read().data.sessions.firstOrNull { it.id == id }

    override suspend fun getSessions(includeDiscarded: Boolean): List<WorkoutSession> =
        if (includeDiscarded) read().allSessions() else read().keptSessions()

    override fun observeSession(id: String): Flow<WorkoutSession?> =
        observe { document -> document.data.sessions.firstOrNull { it.id == id } }

    override suspend fun saveWorkoutSession(session: WorkoutSession): WorkoutSession {
        val normalized = normalize(session)
        database.update { it.edit { upsertSession(normalized) } }
        return getSession(normalized.id) ?: normalized
    }

    override suspend fun deleteWorkoutSession(id: String) =
        database.update { document -> document.edit { copy(sessions = sessions.filterNot { it.id == id }) } }

    /** The shape Room gives a saved workout back in: ids filled, children numbered and linked. */
    private fun normalize(session: WorkoutSession): WorkoutSession {
        val id = session.id.ifBlank { newId() }
        var normalized = session.copy(id = id, name = session.name.ifBlank { DEFAULT_WORKOUT_NAME }.trim())
        if (normalized.status == WorkoutSessionStatus.Completed) {
            val ended = normalized.endedAtUtc ?: now()
            val completed = normalized.completedDateUtc ?: ended
            val duration = if (normalized.durationSeconds > 0) normalized.durationSeconds
            else ((ended - normalized.startedAtUtc) / 1000).coerceAtLeast(0)
            normalized = normalized.copy(endedAtUtc = ended, completedDateUtc = completed, durationSeconds = duration)
        }
        val entries = normalized.entries.sortedBy { it.sortOrder }.mapIndexed { index, entry ->
            val entryId = entry.id.ifBlank { newId() }
            val strength = entry.entryType == ExerciseCategory.Strength
            entry.copy(
                id = entryId,
                workoutSessionId = id,
                exerciseSnapshotName = entry.exerciseSnapshotName.ifBlank { "Exercise" },
                sortOrder = index,
                strengthSets = if (!strength) emptyList() else entry.strengthSets.mapIndexed { setIndex, set ->
                    set.copy(id = set.id.ifBlank { newId() }, workoutEntryId = entryId, setNumber = setIndex + 1)
                },
                cardioData = if (strength) null else (entry.cardioData ?: CardioEntryData()).copy(workoutEntryId = entryId),
            )
        }
        return normalized.copy(entries = entries)
    }

    // --- Machine notes ---------------------------------------------------------------------------

    override fun observeLocationNote(exerciseId: String, locationId: String): Flow<String> = observe { document ->
        document.data.locationNotes.firstOrNull { it.exerciseId == exerciseId && it.locationId == locationId }?.note.orEmpty()
    }

    override fun observeLocationNotesFor(exerciseId: String): Flow<Map<String, String>> = observe { document ->
        document.data.locationNotes.filter { it.exerciseId == exerciseId }.associate { it.locationId to it.note }
    }

    override suspend fun getLocationNotes(exerciseIds: Collection<String>, locationId: String): Map<String, String> {
        val ids = exerciseIds.toSet()
        if (ids.isEmpty()) return emptyMap()
        return read().data.locationNotes.filter { it.locationId == locationId && it.exerciseId in ids }
            .associate { it.exerciseId to it.note }
    }

    override suspend fun getAllLocationNotes(): List<ExerciseLocationNote> = read().sortedLocationNotes()

    override suspend fun saveLocationNote(exerciseId: String, locationId: String, note: String) {
        val text = note.trim()
        database.update { document ->
            document.edit {
                if (text.isEmpty()) {
                    copy(locationNotes = locationNotes.filterNot { it.exerciseId == exerciseId && it.locationId == locationId })
                } else {
                    upsertLocationNote(ExerciseLocationNote(exerciseId, locationId, text))
                }
            }
        }
    }

    override suspend fun restoreLocationNotes(notes: List<ExerciseLocationNote>) {
        val incoming = notes.filter { it.note.isNotBlank() }
        if (incoming.isEmpty()) return
        database.update { document ->
            document.edit {
                val missing = incoming.fold(locationNotes) { list, note ->
                    if (list.any { it.exerciseId == note.exerciseId && it.locationId == note.locationId }) list else list + note
                }
                copy(locationNotes = missing)
            }
        }
    }

    // --- Locations -----------------------------------------------------------------------------

    override fun observeLocations(): Flow<List<Location>> = observe { it.sortedLocations() }

    override suspend fun getLocations(): List<Location> = read().sortedLocations()

    override suspend fun getLocation(id: String): Location? = read().data.locations.firstOrNull { it.id == id }

    override suspend fun saveLocation(location: Location): Location {
        val name = location.name.trim()
        require(name.isNotEmpty()) { "A location needs a name." }
        val saved = location.copy(id = location.id.ifBlank { newId() }, name = name)
        database.update { it.edit { upsertLocation(saved) } }
        return saved
    }

    override suspend fun setLocationArchived(id: String, archived: Boolean) = database.update { document ->
        document.edit { copy(locations = locations.map { if (it.id == id) it.copy(isArchived = archived) else it }) }
    }

    override suspend fun getLastUsedLocationId(): String? {
        val document = read()
        val open = document.data.locations.filterNot { it.isArchived }.mapTo(mutableSetOf()) { it.id }
        return document.keptSessions().firstOrNull { it.locationId in open }?.locationId
    }

    override suspend fun moveWorkoutsToLocation(sessionIds: Collection<String>, locationId: String?): Int {
        val ids = sessionIds.filter(String::isNotBlank).toSet()
        if (ids.isEmpty()) return 0
        return inTransaction {
            val moved = read().data.sessions.count { it.id in ids }
            database.update { document ->
                document.edit { mapSessions { if (it.id in ids) it.copy(locationId = locationId) else it } }
            }
            moved
        }
    }

    override suspend fun assignUnknownWorkoutsTo(locationId: String): Int {
        fun unknown(session: WorkoutSession) = session.locationId == null && session.status != WorkoutSessionStatus.Discarded
        return inTransaction {
            val moved = read().data.sessions.count(::unknown)
            database.update { document ->
                document.edit { mapSessions { if (unknown(it)) it.copy(locationId = locationId) else it } }
            }
            moved
        }
    }

    override suspend fun countWorkoutsWithoutLocation(): Int =
        read().data.sessions.count { it.locationId == null && it.status == WorkoutSessionStatus.Completed }

    override suspend fun deleteAllWorkoutData() = database.update { document ->
        document.edit {
            copy(
                exercises = emptyList(),
                templates = emptyList(),
                sessions = emptyList(),
                locations = Location.builtIns,
                locationNotes = emptyList(),
            )
        }
    }

    override suspend fun countExercises(): Int = read().data.exercises.size

    override suspend fun countTemplates(): Int = read().data.templates.size

    override suspend fun countWorkouts(): Int = read().data.sessions.count { it.status != WorkoutSessionStatus.Discarded }

    // --- History + analytics -------------------------------------------------------------------

    override fun observeHistory(searchText: String?): Flow<List<WorkoutSessionSummary>> =
        observe { summarize(it.completedSessions(), searchText) }

    override suspend fun getWorkoutHistory(searchText: String?): List<WorkoutSessionSummary> =
        summarize(read().completedSessions(), searchText)

    private fun summarize(sessions: List<WorkoutSession>, searchText: String?): List<WorkoutSessionSummary> {
        val needle = searchText?.trim().orEmpty()
        return sessions
            .filter { needle.isBlank() || it.name.contains(needle, ignoreCase = true) }
            .map { Analytics.summarize(it) }
    }

    override suspend fun getAnalyticsOverview() = Analytics.overview(read().completedSessions())

    override suspend fun getConsistencySnapshot() =
        Analytics.consistency(read().completedSessions().map { it.completedDateUtc ?: it.startedAtUtc })

    override suspend fun getExerciseProgress(
        exerciseId: String,
        scope: LocationScope,
    ): List<ExerciseAnalyticsPoint> = Progression.forExercise(scope.apply(read().completedSessions()), exerciseId)

    override suspend fun getDashboardSnapshot(): DashboardSnapshot {
        val document = read()
        val completed = document.completedSessions()
        return DashboardSnapshot(
            activeWorkout = document.activeSession(),
            analytics = Analytics.overview(completed),
            consistency = Analytics.consistency(completed.map { it.completedDateUtc ?: it.startedAtUtc }),
            recentWorkouts = completed.map { Analytics.summarize(it) }.take(5),
        )
    }

    override suspend fun duplicateWorkoutAsTemplate(sessionId: String, templateName: String?): WorkoutTemplate {
        val session = getSession(sessionId) ?: error("Workout session not found.")
        return saveTemplate(session.toTemplate(templateName))
    }

    // --- Export / import -----------------------------------------------------------------------

    override suspend fun createExportBundle(): ExportBundle {
        val document = read()
        return ExportBundle(
            exercises = document.sortedExercises(),
            templates = document.sortedTemplates(),
            sessions = document.keptSessions(),
            locations = document.sortedLocations(),
            locationNotes = document.sortedLocationNotes(),
        )
    }

    override suspend fun importBundle(bundle: ExportBundle) = inTransaction {
        database.update { document ->
            document.edit {
                copy(
                    exercises = exercises.upsertAll(bundle.exercises) { it.id },
                    locations = locations.upsertAll(bundle.locations) { it.id },
                    locationNotes = locationNotes.upsertAll(bundle.locationNotes) { it.exerciseId to it.locationId },
                )
            }
        }
        for (template in bundle.templates) saveTemplate(template)
        for (session in bundle.sessions) saveWorkoutSession(session)
    }

    override suspend fun <T> inTransaction(block: suspend WorkoutRepository.() -> T): T =
        database.transaction { block(this) }
}
