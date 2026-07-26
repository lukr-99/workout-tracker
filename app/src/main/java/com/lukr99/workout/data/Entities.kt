package com.lukr99.workout.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.WorkoutSessionSource

/**
 * Room persistence shapes, ported 1:1 from the MAUI `WorkoutTracker.Core/Data/Records.cs`.
 *
 * These are an implementation detail behind [WorkoutRepository]; the app's shared vocabulary is the
 * `domain/` model, which the repository maps to/from. Table + column names, string-GUID PKs, enum
 * **ordinal** ints (via `Converters`), and epoch-millis `Long` timestamps are all preserved so a
 * `v1.0` JSON export round-trips. Rework-additive columns (03-data-model.md) are nullable/defaulted.
 *
 * Exercise references from `template_exercises`/`entries` are **not** foreign keys — history is
 * immutable-by-snapshot and catalog exercises are archived, never deleted, so past rows must never
 * cascade. Child rows of templates/sessions/entries *do* cascade.
 */

@Entity(
    tableName = "exercises",
    indices = [Index("name"), Index("externalSourceId")],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: ExerciseCategory,
    val primaryBodyPart: String,
    val secondaryBodyPartsJson: String,
    val equipment: String,
    val notes: String,
    val source: ExerciseSource,
    val externalSourceId: String?,
    val isArchived: Boolean,
    val defaultRestSeconds: Int? = null,
    val imageUrl: String? = null,
    val imageAttribution: String? = null,
)

@Entity(
    tableName = "templates",
    indices = [Index("name")],
)
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String,
)

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("templateId")],
)
data class TemplateExerciseEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val exerciseId: String,
    val exerciseName: String,
    val category: ExerciseCategory,
    val bodyPart: String,
    val sortOrder: Int,
    val notes: String,
)

@Entity(
    tableName = "sessions",
    indices = [Index("templateId"), Index("name"), Index("status"), Index("externalKey")],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val templateId: String?,
    val name: String,
    val status: WorkoutSessionStatus,
    val startedAtUtc: Long,
    val endedAtUtc: Long?,
    val completedDateUtc: Long?,
    val durationSeconds: Long,
    val notes: String,
    val perceivedEffort: Int? = null,
    val bodyweightKg: Double? = null,
    val source: WorkoutSessionSource = WorkoutSessionSource.Local,
    val externalKey: String? = null,
)

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutSessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutSessionId")],
)
data class EntryEntity(
    @PrimaryKey val id: String,
    val workoutSessionId: String,
    val exerciseId: String,
    val exerciseSnapshotName: String,
    val exerciseSnapshotCategory: ExerciseCategory,
    val exerciseSnapshotPrimaryBodyPart: String,
    val sortOrder: Int,
    val entryType: ExerciseCategory,
    val notes: String,
    val supersetGroup: Int? = null,
)

@Entity(
    tableName = "strength_sets",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutEntryId")],
)
data class StrengthSetEntity(
    @PrimaryKey val id: String,
    val workoutEntryId: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val rir: Double?,
    val rpe: Double?,
    val performedAtUtc: Long?,
    val notes: String,
    val isWarmup: Boolean = false,
    val isPr: Boolean = false,
    val durationSeconds: Int? = null,
    val setType: SetType = SetType.Normal,
)

@Entity(
    tableName = "cardio_data",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CardioDataEntity(
    @PrimaryKey val workoutEntryId: String,
    val durationSeconds: Int,
    val distanceKm: Double?,
    val calories: Double?,
    val notes: String,
)
