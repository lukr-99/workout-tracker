package com.lukr99.workout.data

import androidx.room.Dao
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
}
