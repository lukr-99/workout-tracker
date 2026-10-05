package com.lukr99.workout.domain

object WorkoutSessionStatusSerializer :
    OrdinalEnumSerializer<WorkoutSessionStatus>("WorkoutSessionStatus", WorkoutSessionStatus.entries.toTypedArray())
