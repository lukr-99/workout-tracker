package com.lukr99.workout.data.run

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "route_points",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routeId")],
)
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: String,
    val seq: Int,
    val lat: Double,
    val lon: Double,
    val elevationM: Double?,
)
