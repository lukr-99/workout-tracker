package com.lukr99.workout.domain.creation

import com.lukr99.workout.domain.WorkoutSessionSource
import com.lukr99.workout.domain.WorkoutSessionStatus

data class SessionDraft(
    val id: String = "",
    val templateId: String? = null,
    val name: String = "",
    val startedAtUtc: Long? = null,
    val endedAtUtc: Long? = null,
    val completedDateUtc: Long? = null,
    val durationSeconds: Long = 0,
    val notes: String = "",
    val status: WorkoutSessionStatus = WorkoutSessionStatus.Active,
    val perceivedEffort: Int? = null,
    val bodyweightKg: Double? = null,
    val source: WorkoutSessionSource = WorkoutSessionSource.Local,
    val externalKey: String? = null,
    val entries: List<EntryDraft> = emptyList(),
)
