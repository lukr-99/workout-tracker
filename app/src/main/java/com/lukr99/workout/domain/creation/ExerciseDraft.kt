package com.lukr99.workout.domain.creation

import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource

data class ExerciseDraft(
    val id: String = "",
    val name: String = "",
    val category: ExerciseCategory = ExerciseCategory.Strength,
    val primaryBodyPart: String = "",
    val secondaryBodyParts: List<String> = emptyList(),
    val equipment: String = "",
    val notes: String = "",
    val source: ExerciseSource = ExerciseSource.Custom,
    val externalSourceId: String? = null,
    val isArchived: Boolean = false,
    val defaultRestSeconds: Int? = null,
    val imageUrl: String? = null,
    val imageAttribution: String? = null,
    val localImagePath: String? = null,
    val instructions: String = "",
    val videoUrl: String? = null,
)
