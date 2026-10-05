package com.lukr99.workout.domain.creation

import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.WeightDisplayUnit

data class EntryDraft(
    val id: String = "",
    val exerciseId: String = "",
    val exerciseName: String = "",
    val category: ExerciseCategory = ExerciseCategory.Strength,
    val bodyPart: String = "",
    val notes: String = "",
    val supersetGroup: Int? = null,
    val weightUnitOverride: WeightDisplayUnit? = null,
    val startedAtUtc: Long? = null,
    val completedAtUtc: Long? = null,
    val strengthSets: List<StrengthSetDraft> = emptyList(),
    val cardio: CardioDraft? = null,
)
