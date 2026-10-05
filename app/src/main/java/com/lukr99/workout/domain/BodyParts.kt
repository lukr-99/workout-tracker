package com.lukr99.workout.domain

/**
 * The body part names the exercise editor offers. They match what the seed catalog and the
 * recovery heatmap already understand, so a picked part lights up the right muscles. Free text is
 * still allowed for anything else.
 */
object BodyParts {
    val common: List<String> = listOf(
        "Chest",
        "Back",
        "Shoulders",
        "Biceps",
        "Triceps",
        "Forearms",
        "Abs",
        "Glutes",
        "Quads",
        "Hamstrings",
        "Calves",
        "Legs",
        "Arms",
        "Full Body",
        "Cardio",
    )

    /** [common] first, then any other parts already used in the catalog, without duplicates. */
    fun options(catalog: List<Exercise>): List<String> {
        val extra = catalog.asSequence()
            .flatMap { sequenceOf(it.primaryBodyPart) + it.secondaryBodyParts.asSequence() }
            .map(String::trim)
            .filter(String::isNotBlank)
            .filter { part -> common.none { it.equals(part, ignoreCase = true) } }
            .distinctBy(String::lowercase)
            .sorted()
            .toList()
        return common + extra
    }
}
