package com.lukr99.workout.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.WorkoutSessionSource

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
