package com.lukr99.workout.data

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room `@Relation` read models for composite loads (`@Transaction` in the DAO). The repository
 * maps these to the `domain/` model graph; nothing outside `data/` sees these Room shapes.
 */

data class TemplateWithExercises(
    @Embedded val template: TemplateEntity,
    @Relation(parentColumn = "id", entityColumn = "templateId")
    val exercises: List<TemplateExerciseEntity>,
)
