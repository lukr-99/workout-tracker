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

/** Origin of a persisted session. Appended-only; ordinal values are part of the v1.2 wire schema. */
@Serializable(with = WorkoutSessionSourceSerializer::class)
enum class WorkoutSessionSource { Local, HealthConnect }

/**
 * Rework-additive set typing (see 03-data-model.md). A superset of Lyfta's `Set Type`; `Warmup`
 * also implies [StrengthSet.isWarmup]. Appended-only — new members go on the end.
 */
@Serializable(with = SetTypeSerializer::class)
enum class SetType { Normal, Warmup, Drop, Failure, Negative, BackOff }

/**
 * Combinable annotations for a performed set. Unlike [SetType], these are deliberately not
 * exclusive: a drop set can be taken to failure and can still end earlier than planned. Keep this
 * enum append-only because tag ordinals are persisted in Room and the portable export.
 */
@Serializable(with = SetTagSerializer::class)
enum class SetTag { Warmup, Drop, ToFailure, Failed, Negative, BackOff }

/** Optional per-entry display/input override. Weight storage remains kilograms. */
@Serializable(with = WeightDisplayUnitSerializer::class)
enum class WeightDisplayUnit { Kilograms, Pounds }
