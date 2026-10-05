package com.lukr99.workout.data.transfer

import java.time.ZoneId

data class ImportOptions(
    val formatHint: DataFormat? = null,
    val sourceWeightUnit: WeightUnit = WeightUnit.Auto,
    val sourceTimeZoneId: String = ZoneId.systemDefault().id,
    val sessionConflictPolicy: ConflictPolicy = ConflictPolicy.Skip,
    val catalogConflictPolicy: ConflictPolicy = ConflictPolicy.Merge,
    val exerciseMatchMode: ExerciseMatchMode = ExerciseMatchMode.Aliases,
    val fuzzyMatchThreshold: Double = 0.84,
    val exerciseAliases: Map<String, String> = emptyMap(),
    val exerciseCategoryOverrides: Map<String, com.lukr99.workout.domain.ExerciseCategory> =
        emptyMap(),
    val strict: Boolean = false,
    val mode: RestoreMode = RestoreMode.Merge,
)
