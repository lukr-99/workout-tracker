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

@Serializable(with = ExerciseSourceSerializer::class)
enum class ExerciseSource { Seeded, Synced, Custom } // Seeded=0, Synced=1, Custom=2

@Serializable(with = WorkoutSessionStatusSerializer::class)
enum class WorkoutSessionStatus { Active, Completed, Discarded } // Active=0, Completed=1, Discarded=2

/**
 * Rework-additive set typing (see 03-data-model.md). A superset of Lyfta's `Set Type`; `Warmup`
 * also implies [StrengthSet.isWarmup]. Appended-only — new members go on the end.
 */
@Serializable(with = SetTypeSerializer::class)
enum class SetType { Normal, Warmup, Drop, Failure, Negative, BackOff }
