package com.lukr99.workout.domain

/** Catalog query filter (ported from MAUI `ExerciseFilter`). */
data class ExerciseFilter(
    val searchText: String = "",
    val bodyPart: String = "",
    val category: ExerciseCategory? = null,
    val equipment: String = "",
    val includeArchived: Boolean = false,
) {
    /** The exercises that match, strength before cardio, then by name. */
    fun apply(exercises: List<Exercise>): List<Exercise> {
        var result = exercises.asSequence()
        if (!includeArchived) result = result.filter { !it.isArchived }

        val needle = searchText.trim()
        if (needle.isNotBlank()) {
            result = result.filter {
                it.name.contains(needle, ignoreCase = true) ||
                    it.primaryBodyPart.contains(needle, ignoreCase = true) ||
                    it.equipment.contains(needle, ignoreCase = true)
            }
        }

        val part = bodyPart.trim()
        if (part.isNotBlank()) {
            result = result.filter {
                it.primaryBodyPart.equals(part, ignoreCase = true) ||
                    it.secondaryBodyParts.any { other -> other.equals(part, ignoreCase = true) }
            }
        }

        category?.let { wanted -> result = result.filter { it.category == wanted } }
        val gear = equipment.trim()
        if (gear.isNotBlank()) {
            result = result.filter {
                it.equipment.split(',').any { item -> item.trim().equals(gear, ignoreCase = true) }
            }
        }
        return result.sortedWith(compareBy({ it.category.ordinal }, { it.name })).toList()
    }
}
