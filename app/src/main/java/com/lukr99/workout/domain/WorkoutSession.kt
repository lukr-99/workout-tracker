package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutSession(
    val id: String = newId(),
    val templateId: String? = null,
    val name: String = "",
    @Serializable(with = InstantMillisSerializer::class)
    val startedAtUtc: Long = nowMillis(),
    @Serializable(with = InstantMillisSerializer::class)
    val endedAtUtc: Long? = null,
    @Serializable(with = InstantMillisSerializer::class)
    val completedDateUtc: Long? = null,
    val durationSeconds: Long = 0,
    val notes: String = "",
    val status: WorkoutSessionStatus = WorkoutSessionStatus.Active,
    // Rework-additive (03-data-model.md).
    val perceivedEffort: Int? = null,
    val bodyweightKg: Double? = null,
    // Export v1.2 / integration provenance. Existing 1.0/1.1 data defaults to local.
    val source: WorkoutSessionSource = WorkoutSessionSource.Local,
    val externalKey: String? = null,
    // Export v1.12: where it happened (a Location id). Null is "Unknown location".
    val locationId: String? = null,
    val entries: List<WorkoutEntry> = emptyList(),
)
