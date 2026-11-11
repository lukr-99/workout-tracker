package com.lukr99.workout.ui.android

import com.lukr99.workout.data.claude.Proposal

/**
 * A proposal in a few lines, for Home and Settings, Claude: a template's exercises with their plan
 * ("Bench Press: 4 x 6-8, rest 3:00, RIR 2"), or an exercise's body part, category and equipment.
 */
internal fun proposalDetails(proposal: Proposal): String = runCatching {
    if (proposal.isTemplate) {
        proposal.template().exercises.joinToString("\n") { item ->
            val reps = when {
                item.repsMin != null && item.repsMax != null && item.repsMin != item.repsMax -> "${item.repsMin}-${item.repsMax}"
                else -> (item.repsMin ?: item.repsMax)?.toString()
            }
            val plan = listOfNotNull(
                item.targetSets?.let { sets -> if (reps != null) "$sets x $reps" else "$sets sets" } ?: reps?.let { "$it reps" },
                item.restSeconds?.let { "rest ${it / 60}:${(it % 60).toString().padStart(2, '0')}" },
                item.supersetGroup?.let { "superset $it" },
                item.notes.takeIf(String::isNotBlank),
            )
            if (plan.isEmpty()) item.name else "${item.name}: ${plan.joinToString(", ")}"
        }
    } else {
        val exercise = proposal.exercise()
        listOf(exercise.primaryBodyPart, exercise.category, exercise.equipment).filter(String::isNotBlank).joinToString(", ")
    }
}.getOrDefault("")
