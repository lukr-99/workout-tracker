package com.lukr99.workout.data.run

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "run_points",
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("runId")],
)
data class RunPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String,
    val t: Long,
    val lat: Double,
    val lon: Double,
    val elevationM: Double?,
    val speedMps: Double?,
    val hrBpm: Int?,
    val accuracyM: Double?,
    /** True when this point starts a new segment after a manual pause (see `TracePoint.segmentStart`). */
    val segmentStart: Boolean = false,
)
