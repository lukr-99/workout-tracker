package com.lukr99.workout.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.WeightDisplayUnit

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
    val weightUnitOverride: WeightDisplayUnit? = null,
    val startedAtUtc: Long? = null,
    val completedAtUtc: Long? = null,
)
