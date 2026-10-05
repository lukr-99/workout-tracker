package com.lukr99.workout.ui

import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.updatedFrom

/** Finds the template a workout started from and applies the owner's [TemplateChoice] after finishing. */
class TemplateSync(private val repo: WorkoutRepository) {

    suspend fun templateOf(workout: WorkoutSession): WorkoutTemplate? =
        workout.templateId?.takeIf(String::isNotBlank)?.let { repo.getTemplate(it) }

    suspend fun apply(workout: WorkoutSession, template: WorkoutTemplate, choice: TemplateChoice, newName: String) {
        when (choice) {
            TemplateChoice.Keep -> Unit
            TemplateChoice.Update -> repo.saveTemplate(template.updatedFrom(workout))
            TemplateChoice.SaveAsNew -> {
                val updated = template.updatedFrom(workout)
                repo.saveTemplate(
                    updated.copy(
                        id = "",
                        name = newName.trim().ifBlank { "${template.name} copy" },
                        // Fresh ids, so the copy never takes rows from the original template.
                        exercises = updated.exercises.map { it.copy(id = "") },
                    ),
                )
            }
        }
    }
}
