package com.lukr99.workout.domain

object ExerciseSourceSerializer :
    OrdinalEnumSerializer<ExerciseSource>("ExerciseSource", ExerciseSource.entries.toTypedArray())
