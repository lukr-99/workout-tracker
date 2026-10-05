package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

/**
 * Combinable annotations for a performed set. Unlike [SetType], these are deliberately not
 * exclusive: a drop set can be taken to failure and can still end earlier than planned. Keep this
 * enum append-only because tag ordinals are persisted in Room and the portable export.
 */
@Serializable(with = SetTagSerializer::class)
enum class SetTag { Warmup, Drop, ToFailure, Failed, Negative, BackOff }
