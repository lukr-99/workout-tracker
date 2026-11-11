package com.lukr99.workout.data.claude

import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.WorkoutTemplateExercise

/**
 * Claude's proposals waiting for the owner (docs/claude-connector.md, "The inbox"). Accepting writes
 * the exercise or template here first and then tells the server, so a failed call loses nothing: the
 * proposal shows again, and accepting it twice writes the same rows.
 */
class ProposalInbox(
    private val api: ClaudeApi,
    private val repository: WorkoutRepository,
) {
    suspend fun pending(): List<Proposal> = api.pendingProposals()

    suspend fun accept(proposal: Proposal) {
        if (proposal.isTemplate) applyTemplate(proposal.template()) else applyExercise(proposal.exercise())
        api.decide(proposal.id, accepted = true)
    }

    suspend fun reject(proposal: Proposal) = api.decide(proposal.id, accepted = false)

    /** Saves the template, replacing the one it is a new version of, with any exercise it brings. */
    suspend fun applyTemplate(template: ProposedTemplate) {
        template.exercises.mapNotNull(ProposedTemplateExercise::newExercise).forEach { applyExercise(it) }
        val existing = repository.getTemplate(template.templateId)
        repository.saveTemplate(
            WorkoutTemplate(
                id = template.templateId,
                name = template.name,
                notes = template.notes,
                exercises = template.exercises.mapIndexed { index, item ->
                    WorkoutTemplateExercise(
                        // Keep the row ids of a template this replaces, so nothing else sees new rows.
                        id = existing?.exercises?.getOrNull(index)?.id ?: WorkoutTemplateExercise().id,
                        exerciseId = item.exerciseId,
                        exerciseName = item.name,
                        category = category(item.category),
                        bodyPart = item.bodyPart,
                        sortOrder = index,
                        notes = item.notes,
                        targetSets = item.targetSets,
                        repsMin = item.repsMin,
                        repsMax = item.repsMax,
                        restSeconds = item.restSeconds,
                        supersetGroup = item.supersetGroup,
                    )
                },
            ),
        )
    }

    /** Adds the exercise as the owner's own, unless it is already there. */
    suspend fun applyExercise(exercise: ProposedExercise) {
        if (repository.getExercise(exercise.exerciseId) != null) return
        repository.saveExercise(
            Exercise(
                id = exercise.exerciseId,
                name = exercise.name,
                category = category(exercise.category),
                primaryBodyPart = exercise.primaryBodyPart,
                secondaryBodyParts = exercise.secondaryBodyParts,
                equipment = exercise.equipment,
                notes = exercise.notes,
                source = ExerciseSource.Custom,
                defaultRestSeconds = exercise.defaultRestSeconds,
                instructions = exercise.instructions,
            ),
        )
    }

    private fun category(name: String) = if (name == "cardio") ExerciseCategory.Cardio else ExerciseCategory.Strength
}
