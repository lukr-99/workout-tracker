package com.lukr99.workout.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * The Room query/mutation surface, mirroring the behaviour of the MAUI `WorkoutTrackerRepository`.
 * Flow-returning reads drive the reactive UI; composite reads are `@Transaction` + `@Relation`.
 * All writes are upserts; deletes rely on `ForeignKey.CASCADE` for children. Only
 * [WorkoutRepository] calls this.
 */
@Dao
interface WorkoutDao {

    // --- Exercises (catalog) -------------------------------------------------------------------

    /** Full catalog ordered Strength-before-Cardio then by name (matches MAUI ordering). */
    @Query("SELECT * FROM exercises ORDER BY category, name")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY category, name")
    suspend fun getAllExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExercise(id: String): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun countExercises(): Int

    @Upsert
    suspend fun upsertExercise(exercise: ExerciseEntity)

    @Upsert
    suspend fun upsertExercises(exercises: List<ExerciseEntity>)

    /** Archive, never delete — history keeps its snapshot references intact. */
    @Query("UPDATE exercises SET isArchived = 1 WHERE id = :id")
    suspend fun archiveExercise(id: String)

    // --- Templates -----------------------------------------------------------------------------

    @Transaction
    @Query("SELECT * FROM templates ORDER BY name")
    fun observeTemplates(): Flow<List<TemplateWithExercises>>

    @Transaction
    @Query("SELECT * FROM templates ORDER BY name")
    suspend fun getAllTemplates(): List<TemplateWithExercises>

    @Transaction
    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplate(id: String): TemplateWithExercises?

    @Upsert
    suspend fun upsertTemplate(template: TemplateEntity)

    @Upsert
    suspend fun upsertTemplateExercises(exercises: List<TemplateExerciseEntity>)

    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun deleteTemplateExercises(templateId: String)

    /** Cascade removes `template_exercises`. */
    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    // --- Sessions ------------------------------------------------------------------------------

    @Transaction
    @Query("SELECT * FROM sessions WHERE status = 0 ORDER BY startedAtUtc DESC LIMIT 1")
    fun observeActiveSession(): Flow<SessionWithEntries?>

    @Transaction
    @Query("SELECT * FROM sessions WHERE status = 0 ORDER BY startedAtUtc DESC LIMIT 1")
    suspend fun getActiveSession(): SessionWithEntries?

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: String): SessionWithEntries?

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeSession(id: String): Flow<SessionWithEntries?>

    /** Completed history, most-recent first (matches MAUI `GetWorkoutHistoryAsync`). */
    @Transaction
    @Query("SELECT * FROM sessions WHERE status = 1 ORDER BY completedDateUtc DESC")
    fun observeCompletedSessions(): Flow<List<SessionWithEntries>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE status = 1 ORDER BY completedDateUtc DESC")
    suspend fun getCompletedSessions(): List<SessionWithEntries>

    /** Everything except discarded — the export/backup surface (completed history + any active). */
    @Transaction
    @Query("SELECT * FROM sessions WHERE status != 2 ORDER BY startedAtUtc DESC")
    suspend fun getExportableSessions(): List<SessionWithEntries>

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY startedAtUtc DESC")
    suspend fun getAllSessions(): List<SessionWithEntries>

    /**
     * The newest non-blank note on [exerciseId] from a completed workout, skipping
     * [excludeSessionId] (the live one), for the "Last time" line while logging. Unless
     * [anyLocation], only workouts at [locationId] count (null matches "Unknown location").
     */
    @Query(
        """
        SELECT entries.notes AS note,
               COALESCE(sessions.completedDateUtc, sessions.startedAtUtc) AS atUtc
        FROM entries INNER JOIN sessions ON sessions.id = entries.workoutSessionId
        WHERE entries.exerciseId = :exerciseId
          AND sessions.status = 1
          AND sessions.id != :excludeSessionId
          AND TRIM(entries.notes) != ''
          AND (:anyLocation OR sessions.locationId IS :locationId)
        ORDER BY atUtc DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestEntryNote(
        exerciseId: String,
        excludeSessionId: String,
        anyLocation: Boolean,
        locationId: String?,
    ): PreviousNoteRow?

    @Upsert
    suspend fun upsertSession(session: SessionEntity)

    @Upsert
    suspend fun upsertEntries(entries: List<EntryEntity>)

    @Upsert
    suspend fun upsertStrengthSets(sets: List<StrengthSetEntity>)

    @Upsert
    suspend fun upsertCardio(cardio: List<CardioDataEntity>)

    /** Cascade removes this session's `entries` → `strength_sets` / `cardio_data`. */
    @Query("DELETE FROM entries WHERE workoutSessionId = :sessionId")
    suspend fun deleteEntriesForSession(sessionId: String)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    /** Moves the given workouts to [locationId] (null is "Unknown location"). */
    @Query("UPDATE sessions SET locationId = :locationId WHERE id IN (:sessionIds)")
    suspend fun setSessionLocation(sessionIds: List<String>, locationId: String?): Int

    /** Gives every kept workout without a location [locationId]. Returns how many moved. */
    @Query("UPDATE sessions SET locationId = :locationId WHERE locationId IS NULL AND status != 2")
    suspend fun assignUnknownSessionLocations(locationId: String): Int

    @Query("SELECT COUNT(*) FROM sessions WHERE locationId IS NULL AND status = 1")
    suspend fun countCompletedSessionsWithoutLocation(): Int

    /** The place of the newest kept workout that has one, if that place is not archived. */
    @Query(
        """
        SELECT sessions.locationId FROM sessions
        INNER JOIN locations ON locations.id = sessions.locationId
        WHERE sessions.status != 2 AND locations.isArchived = 0
        ORDER BY sessions.startedAtUtc DESC
        LIMIT 1
        """,
    )
    suspend fun getLastUsedLocationId(): String?

    // --- Locations -----------------------------------------------------------------------------

    @Query("SELECT * FROM locations ORDER BY kind, name")
    fun observeLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations ORDER BY kind, name")
    suspend fun getLocations(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getLocation(id: String): LocationEntity?

    @Upsert
    suspend fun upsertLocation(location: LocationEntity)

    /** Adds rows that are missing and leaves existing ones (a renamed Home stays renamed). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissingLocations(locations: List<LocationEntity>)

    @Query("UPDATE locations SET isArchived = :archived WHERE id = :id")
    suspend fun setLocationArchived(id: String, archived: Boolean)

    @Query("DELETE FROM locations")
    suspend fun deleteAllLocations()

    // --- Machine notes per exercise and location -----------------------------------------------

    @Query("SELECT * FROM exercise_location_notes WHERE exerciseId = :exerciseId AND locationId = :locationId")
    fun observeLocationNote(exerciseId: String, locationId: String): Flow<ExerciseLocationNoteEntity?>

    @Query("SELECT * FROM exercise_location_notes WHERE locationId = :locationId AND exerciseId IN (:exerciseIds)")
    suspend fun getLocationNotes(exerciseIds: List<String>, locationId: String): List<ExerciseLocationNoteEntity>

    @Query("SELECT * FROM exercise_location_notes WHERE exerciseId = :exerciseId")
    fun observeLocationNotesFor(exerciseId: String): Flow<List<ExerciseLocationNoteEntity>>

    @Query("SELECT * FROM exercise_location_notes ORDER BY exerciseId, locationId")
    suspend fun getAllLocationNotes(): List<ExerciseLocationNoteEntity>

    @Upsert
    suspend fun upsertLocationNote(note: ExerciseLocationNoteEntity)

    @Query("DELETE FROM exercise_location_notes WHERE exerciseId = :exerciseId AND locationId = :locationId")
    suspend fun deleteLocationNote(exerciseId: String, locationId: String)

    /** Keeps the note already on this device when the same pair is restored again. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissingLocationNotes(notes: List<ExerciseLocationNoteEntity>)

    @Query("DELETE FROM exercise_location_notes")
    suspend fun deleteAllLocationNotes()

    // --- Per-location tracking -----------------------------------------------------------------

    @Query("UPDATE exercises SET trackPerLocation = :perLocation WHERE id = :exerciseId")
    suspend fun setTrackPerLocation(exerciseId: String, perLocation: Boolean)

    @Query("UPDATE exercises SET splitPromptMuted = 1 WHERE id = :exerciseId")
    suspend fun muteSplitPrompt(exerciseId: String)

    // --- Erase everything (replace-restore, delete all data) ---------------------------------

    /** Cascade removes entries, strength sets and cardio data. */
    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    /** Cascade removes template exercises. */
    @Query("DELETE FROM templates")
    suspend fun deleteAllTemplates()

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun countTemplates(): Int

    @Query("SELECT COUNT(*) FROM sessions WHERE status != 2")
    suspend fun countKeptSessions(): Int
}
