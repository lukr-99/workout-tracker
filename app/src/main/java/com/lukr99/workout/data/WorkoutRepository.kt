package com.lukr99.workout.data

import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.domain.AnalyticsOverview
import com.lukr99.workout.domain.DashboardSnapshot
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseAnalyticsPoint
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.ExerciseLocationNote
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.LocationScope
import com.lukr99.workout.domain.PreviousEntryNote
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutConsistencySnapshot
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionSummary
import com.lukr99.workout.domain.WorkoutTemplate
import kotlinx.coroutines.flow.Flow

/**
 * The single API over workout persistence: the only way ui and services reach exercises, templates
 * and workouts. [RoomWorkoutRepository] is the Android implementation, the only class that touches
 * Room for them. Every implementation keeps the history-safe rules:
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
interface WorkoutRepository {

    /**
     * Insert the starter catalog once, on an empty DB (mirrors MAUI `InitializeAsync`), and the
     * built-in Home and Outdoors locations whenever they are missing.
     */
    suspend fun ensureSeeded()

    /** Catalog stream with the MAUI in-memory filter (search / body-part / category / archived). */
    fun observeExercises(filter: ExerciseFilter = ExerciseFilter()): Flow<List<Exercise>>

    suspend fun getExercises(filter: ExerciseFilter = ExerciseFilter()): List<Exercise>

    suspend fun getExercise(id: String): Exercise?

    suspend fun saveExercise(exercise: Exercise): Exercise

    suspend fun archiveExercise(id: String)

    /**
     * Track [exerciseId] per location or shared. Only a flag changes: its history is read through a
     * different [LocationScope], so nothing is lost either way.
     */
    suspend fun setTrackPerLocation(exerciseId: String, perLocation: Boolean)

    /** "Don't ask again" for the per-location prompt on [exerciseId]. */
    suspend fun muteSplitPrompt(exerciseId: String)

    /** Which workouts count for [exerciseId] in a workout at [locationId]. */
    suspend fun locationScope(exerciseId: String, locationId: String?): LocationScope

    /**
     * Compatibility count for older callers. New sync flows should use
     * [mergeExternalExercisesDetailed] to distinguish inserts, safe updates, and skips.
     */
    suspend fun mergeExternalExercises(exercises: List<Exercise>): Int

    /** Additively merge an external catalog. [ExternalExerciseMerge] decides what changes. */
    suspend fun mergeExternalExercisesDetailed(
        exercises: List<Exercise>,
    ): ExternalExerciseMergeSummary

    /**
     * Match every existing row by a conservative normalized name and fill remote artwork only when
     * it is missing. No user-owned catalog field is changed.
     */
    suspend fun backfillMissingExerciseImages(candidates: List<Exercise>): Int

    /** A fresh entry with the exercise's identity snapshotted onto it (the "snapshot on log" rule). */
    fun newEntryForExercise(exercise: Exercise, sortOrder: Int = 0): WorkoutEntry

    fun observeTemplates(): Flow<List<WorkoutTemplate>>

    suspend fun getTemplates(): List<WorkoutTemplate>

    suspend fun getTemplate(id: String): WorkoutTemplate?

    suspend fun saveTemplate(template: WorkoutTemplate): WorkoutTemplate

    suspend fun deleteTemplate(id: String)

    /** When a workout from this template was last finished, or null if never. */
    suspend fun getTemplateLastDone(templateId: String): Long?

    /** When each template was last finished, by template id, from one pass over finished workouts. */
    suspend fun getTemplatesLastDone(): Map<String, Long>

    /**
     * Last time's sets for each of [exerciseIds], from the newest finished workout that logged it.
     * An exercise tracked per location only looks at workouts at [locationId].
     */
    suspend fun getLastSets(exerciseIds: Collection<String>, locationId: String?): Map<String, List<StrengthSet>>

    /**
     * Start a session, instantiating from a template if given. Returns any existing active one. The
     * new workout is at the last used location ([getLastUsedLocationId]).
     */
    suspend fun createWorkoutSession(templateId: String? = null, name: String? = null): WorkoutSession

    /**
     * The latest "Last time" note for each of [exerciseIds], from finished workouts other than
     * [currentSessionId]. Exercises without an earlier note are left out of the map. An exercise
     * tracked per location only reads notes from workouts at [locationId].
     */
    suspend fun getPreviousEntryNotes(
        exerciseIds: Collection<String>,
        currentSessionId: String,
        locationId: String?,
    ): Map<String, PreviousEntryNote>

    fun observeActiveSession(): Flow<WorkoutSession?>

    suspend fun getActiveSession(): WorkoutSession?

    suspend fun getSession(id: String): WorkoutSession?

    /** Full session graph for data services. Discarded sessions are opt-in. */
    suspend fun getSessions(includeDiscarded: Boolean = false): List<WorkoutSession>

    fun observeSession(id: String): Flow<WorkoutSession?>

    suspend fun saveWorkoutSession(session: WorkoutSession): WorkoutSession

    suspend fun deleteWorkoutSession(id: String)

    /** The note for [exerciseId] at [locationId], empty when there is none. Updates as it is saved. */
    fun observeLocationNote(exerciseId: String, locationId: String): Flow<String>

    /** Every note for [exerciseId] by location id, for the exercise editor. */
    fun observeLocationNotesFor(exerciseId: String): Flow<Map<String, String>>

    /** Notes at [locationId] for the given exercises. Exercises without one are left out. */
    suspend fun getLocationNotes(exerciseIds: Collection<String>, locationId: String): Map<String, String>

    suspend fun getAllLocationNotes(): List<ExerciseLocationNote>

    /** Save the note for a pair. A blank note removes it, so no empty rows pile up. */
    suspend fun saveLocationNote(exerciseId: String, locationId: String, note: String)

    /** Restore notes from a backup; a pair that already has a note here keeps it. */
    suspend fun restoreLocationNotes(notes: List<ExerciseLocationNote>)

    /** Every location, archived ones included. Callers pick with `forPicker()`. */
    fun observeLocations(): Flow<List<Location>>

    suspend fun getLocations(): List<Location>

    suspend fun getLocation(id: String): Location?

    /** Add or rename a location. A blank name is refused. */
    suspend fun saveLocation(location: Location): Location

    /** Archived places leave the picker; workouts there keep pointing at them. */
    suspend fun setLocationArchived(id: String, archived: Boolean)

    /** The place of the newest workout that has one, unless that place is archived. */
    suspend fun getLastUsedLocationId(): String?

    /** Move [sessionIds] to [locationId] (null is "Unknown location"). Returns how many moved. */
    suspend fun moveWorkoutsToLocation(sessionIds: Collection<String>, locationId: String?): Int

    /** Give every workout that has no location [locationId]. Returns how many moved. */
    suspend fun assignUnknownWorkoutsTo(locationId: String): Int

    /** Finished workouts with no location. */
    suspend fun countWorkoutsWithoutLocation(): Int

    /**
     * Removes every workout, template and exercise. Callers wrap it in a transaction together with
     * whatever replaces the data, and reseed with [ensureSeeded] when nothing replaces it.
     */
    suspend fun deleteAllWorkoutData()

    suspend fun countExercises(): Int

    suspend fun countTemplates(): Int

    /** Finished and live workouts; discarded ones are not counted. */
    suspend fun countWorkouts(): Int

    fun observeHistory(searchText: String? = null): Flow<List<WorkoutSessionSummary>>

    suspend fun getWorkoutHistory(searchText: String? = null): List<WorkoutSessionSummary>

    suspend fun getAnalyticsOverview(): AnalyticsOverview

    suspend fun getConsistencySnapshot(): WorkoutConsistencySnapshot

    suspend fun getExerciseProgress(
        exerciseId: String,
        scope: LocationScope = LocationScope.Shared,
    ): List<ExerciseAnalyticsPoint>

    suspend fun getDashboardSnapshot(): DashboardSnapshot

    /** Turn a completed session into a reusable template (ported from MAUI). */
    suspend fun duplicateWorkoutAsTemplate(sessionId: String, templateName: String? = null): WorkoutTemplate

    suspend fun createExportBundle(): ExportBundle

    /** Restore a supported bundle into the DB, preserving ids (upsert). */
    suspend fun importBundle(bundle: ExportBundle)

    /** Execute a multi-record service operation atomically. */
    suspend fun <T> inTransaction(block: suspend WorkoutRepository.() -> T): T

}
