package com.lukr99.workout.domain.recovery

/**
 * Turns the free-text body parts on an exercise into the muscles they train. A known name maps to
 * one [MuscleGroup]. A broad name spreads over several: Legs and Arms count fully for each of their
 * muscles, Full Body counts half for each big muscle. Any other name stays its own part, so a custom
 * body part still shows up, just not on the body map.
 */
object MuscleGroups {
    private val byAlias: Map<String, MuscleGroup> =
        MuscleGroup.entries.flatMap { group -> group.aliases.map { it to group } }.toMap()

    private val broad: Map<String, Map<MuscleGroup, Double>> = mapOf(
        "legs" to full(MuscleGroup.Quads, MuscleGroup.Hamstrings, MuscleGroup.Glutes),
        "lower body" to full(MuscleGroup.Quads, MuscleGroup.Hamstrings, MuscleGroup.Glutes),
        "arms" to full(MuscleGroup.Biceps, MuscleGroup.Triceps),
        "full body" to listOf(
            MuscleGroup.Chest, MuscleGroup.Back, MuscleGroup.Shoulders,
            MuscleGroup.Quads, MuscleGroup.Hamstrings, MuscleGroup.Glutes,
        ).associateWith { 0.5 },
    )

    /** The muscle a single name means, or null for a broad or unknown name. */
    fun groupOf(bodyPart: String): MuscleGroup? = byAlias[bodyPart.trim().lowercase()]

    /**
     * The parts one body part name stands for, keyed by [MuscleGroup.label] for known muscles and by
     * the trimmed name otherwise, with the share of a set each one gets.
     */
    fun resolve(bodyPart: String): Map<String, Double> {
        val name = bodyPart.trim()
        if (name.isBlank()) return emptyMap()
        val key = name.lowercase()
        byAlias[key]?.let { return mapOf(it.label to 1.0) }
        broad[key]?.let { spread -> return spread.mapKeys { it.key.label } }
        return mapOf(name to 1.0)
    }

    private fun full(vararg groups: MuscleGroup) = groups.associateWith { 1.0 }
}
