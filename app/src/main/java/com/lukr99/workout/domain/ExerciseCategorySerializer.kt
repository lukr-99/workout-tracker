package com.lukr99.workout.domain

object ExerciseCategorySerializer :
    OrdinalEnumSerializer<ExerciseCategory>("ExerciseCategory", ExerciseCategory.entries.toTypedArray())
