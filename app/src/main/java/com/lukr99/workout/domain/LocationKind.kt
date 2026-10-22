package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

/**
 * What kind of place a [Location] is. Stored and exported as its ordinal (see
 * [OrdinalEnumSerializer]), so never reorder the members.
 */
@Serializable(with = LocationKindSerializer::class)
enum class LocationKind { Gym, Home, Outdoors } // Gym=0, Home=1, Outdoors=2
