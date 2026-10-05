package com.lukr99.workout.data.run

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routes",
    indices = [Index("name")],
)
data class RouteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val distanceMeters: Double,
    val elevationGainM: Double,
    val encodedPolyline: String,
    val createdAtUtc: Long,
    val notes: String,
)
