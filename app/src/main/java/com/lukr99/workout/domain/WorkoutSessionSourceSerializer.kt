package com.lukr99.workout.domain

object WorkoutSessionSourceSerializer :
    OrdinalEnumSerializer<WorkoutSessionSource>("WorkoutSessionSource", WorkoutSessionSource.entries.toTypedArray())
