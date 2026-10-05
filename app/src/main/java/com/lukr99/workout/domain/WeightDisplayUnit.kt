package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

/** Optional per-entry display/input override. Weight storage remains kilograms. */
@Serializable(with = WeightDisplayUnitSerializer::class)
enum class WeightDisplayUnit { Kilograms, Pounds }
