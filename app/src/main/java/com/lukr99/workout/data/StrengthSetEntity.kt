package com.lukr99.workout.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lukr99.workout.domain.SetType

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
    /** JSON array of [com.lukr99.workout.domain.SetTag] ordinals. */
    val tagsJson: String = "[]",
)
