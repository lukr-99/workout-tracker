package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

/**
 * Rework-additive set typing (see 03-data-model.md). A superset of Lyfta's `Set Type`; `Warmup`
 * also implies [StrengthSet.isWarmup]. Appended-only — new members go on the end.
 */
@Serializable(with = SetTypeSerializer::class)
enum class SetType { Normal, Warmup, Drop, Failure, Negative, BackOff }
