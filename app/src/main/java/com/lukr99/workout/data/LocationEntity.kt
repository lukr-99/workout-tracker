package com.lukr99.workout.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lukr99.workout.domain.LocationKind

/** v10: a place where workouts happen. Archived, never deleted, like exercises. */
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: LocationKind,
    val latitude: Double?,
    val longitude: Double?,
    val isArchived: Boolean,
)
