package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

/**
 * Domain enums, ported 1:1 from the MAUI `WorkoutTracker.Core/Domain/Models.cs`.
 *
 * **The ordinal values are a wire contract.** They are stored as Int in Room *and* serialized as
 * Int in the `ExportBundle` JSON (see [OrdinalEnumSerializer]), so a `v1.0` export imports 1:1 and
 * any future desktop tool reads the same numbers. Do not reorder or renumber existing members.
 */
@Serializable(with = ExerciseCategorySerializer::class)
enum class ExerciseCategory { Strength, Cardio } // Strength=0, Cardio=1
