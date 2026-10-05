package com.lukr99.workout.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource

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
    val localImagePath: String? = null,
    /** v8: how-to steps, one per line. */
    val instructions: String = "",
    /** v8: optional http(s) guide or video link. */
    val videoUrl: String? = null,
)
