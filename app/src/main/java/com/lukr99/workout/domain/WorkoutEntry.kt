package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutEntry(
    val id: String = newId(),
    val workoutSessionId: String = "",
    val exerciseId: String = "",
    val exerciseSnapshotName: String = "",
    val exerciseSnapshotCategory: ExerciseCategory = ExerciseCategory.Strength,
    val exerciseSnapshotPrimaryBodyPart: String = "",
    val sortOrder: Int = 0,
    val entryType: ExerciseCategory = ExerciseCategory.Strength,
    val notes: String = "",
    // Rework-additive (03-data-model.md).
    val supersetGroup: Int? = null,
    // Per-exercise logging lifecycle and display preference (storage stays metric).
    val weightUnitOverride: WeightDisplayUnit? = null,
    @Serializable(with = InstantMillisSerializer::class)
    val startedAtUtc: Long? = null,
    @Serializable(with = InstantMillisSerializer::class)
    val completedAtUtc: Long? = null,
    val strengthSets: List<StrengthSet> = emptyList(),
    val cardioData: CardioEntryData? = null,
) {
    val isStrength: Boolean get() = entryType == ExerciseCategory.Strength
}
