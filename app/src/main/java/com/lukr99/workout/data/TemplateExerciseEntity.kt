package com.lukr99.workout.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lukr99.workout.domain.ExerciseCategory

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("templateId")],
)
data class TemplateExerciseEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val exerciseId: String,
    val exerciseName: String,
    val category: ExerciseCategory,
    val bodyPart: String,
    val sortOrder: Int,
    val notes: String,
)
