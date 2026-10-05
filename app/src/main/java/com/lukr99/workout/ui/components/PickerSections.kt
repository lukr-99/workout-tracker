package com.lukr99.workout.ui.components

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseFilter

/**
 * What the exercise picker lists. Recent exercises come first while nothing is typed; a search
 * lists every match in one block. [createName] is the typed name when no exercise has exactly that
 * name, so the picker can offer "Create" without leaving the workout.
 */
data class PickerSections(
    val recent: List<Exercise>,
    val all: List<Exercise>,
    val createName: String?,
) {
    companion object {
        fun of(exercises: List<Exercise>, filter: ExerciseFilter, recentIds: List<String>): PickerSections {
            val matches = filter.apply(exercises)
            val query = filter.searchText.trim()
            val createName = query.takeIf { q -> q.isNotEmpty() && exercises.none { it.name.trim().equals(q, ignoreCase = true) } }
            if (query.isNotEmpty() || recentIds.isEmpty()) return PickerSections(emptyList(), matches, createName)
            val byId = matches.associateBy(Exercise::id)
            val recent = recentIds.mapNotNull(byId::get)
            val recentSet = recent.map(Exercise::id).toSet()
            return PickerSections(recent, matches.filterNot { it.id in recentSet }, createName)
        }
    }
}
