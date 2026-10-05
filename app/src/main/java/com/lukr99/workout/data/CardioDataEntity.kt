package com.lukr99.workout.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

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
