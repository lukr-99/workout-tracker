package com.lukr99.workout.domain.recovery

/**
 * The numbers behind the recovery model. docs/research/muscle-recovery.md explains where each one
 * comes from. Fatigue is counted in hard sets: a working set at [defaultRir] reps in reserve on the
 * main muscle adds 1, and it fades with the muscle's half-life.
 */
data class RecoveryConfig(
    /** Hours for half of a muscle's fatigue to fade. */
    val halfLifeHours: Map<MuscleGroup, Double> = DefaultHalfLifeHours,
    /** Half-life for a custom body part or Cardio. */
    val otherHalfLifeHours: Double = 24.0,
    val lookbackHours: Double = 14 * 24.0,
    val weeklyWindowHours: Double = 7 * 24.0,
    /** The share of a set a secondary muscle gets. */
    val secondaryMuscleContribution: Double = 0.5,
    /** Reps in reserve assumed when a set has no RIR, RPE or failure tag. Its effort is 1. */
    val defaultRir: Double = 2.0,
    /** Effort added for each rep in reserve below [defaultRir], and taken away for each above. */
    val effortPerRir: Double = 0.15,
    val minEffort: Double = 0.5,
    val maxEffort: Double = 1.3,
    /** Extra fatigue for a negative or slow eccentric set, which causes more muscle damage. */
    val eccentricMultiplier: Double = 1.2,
    /** A set this many reps or more from failure is too easy to count as a hard set for the week. */
    val easySetRir: Double = 5.0,
    /** Minutes of cardio that count as one set for the exercise's body parts. */
    val cardioMinutesPerSet: Double = 30.0,
    /** Fatigue, in hard sets, at which readiness is 100 / e, about 37 %. */
    val fatigueSaturationSets: Double = 5.0,
    val readyThreshold: Double = 80.0,
    val fatiguedThreshold: Double = 50.0,
    /** Hard sets per muscle per week that research links to good muscle growth. */
    val weeklyTargetSets: ClosedFloatingPointRange<Double> = 10.0..20.0,
) {
    init {
        require(halfLifeHours.values.all { it > 0 } && otherHalfLifeHours > 0)
        require(lookbackHours > 0 && weeklyWindowHours > 0)
        require(secondaryMuscleContribution in 0.0..1.0)
        require(effortPerRir >= 0 && minEffort > 0 && maxEffort >= minEffort && eccentricMultiplier >= 1)
        require(cardioMinutesPerSet > 0 && fatigueSaturationSets > 0)
        require(readyThreshold in 0.01..99.99 && fatiguedThreshold in 0.0..readyThreshold)
        require(weeklyTargetSets.start >= 0)
    }

    fun halfLifeFor(group: MuscleGroup?): Double = group?.let(halfLifeHours::get) ?: otherHalfLifeHours

    companion object {
        /**
         * Chosen so 6 hard sets at the default effort reach [readyThreshold] at the research's "about
         * 90 % recovered" time: 72 h for quads and hamstrings, 60 h for glutes, 48 h for chest, back
         * and arms, 42 h for calves, 36 h for shoulders and 30 h for abs and forearms. Half-life is that
         * time divided by log2(6 / 1.116), about 2.43.
         */
        val DefaultHalfLifeHours: Map<MuscleGroup, Double> = mapOf(
            MuscleGroup.Chest to 20.0,
            MuscleGroup.Back to 20.0,
            MuscleGroup.Shoulders to 15.0,
            MuscleGroup.Biceps to 20.0,
            MuscleGroup.Triceps to 20.0,
            MuscleGroup.Forearms to 12.0,
            MuscleGroup.Abs to 12.0,
            MuscleGroup.Glutes to 25.0,
            MuscleGroup.Quads to 30.0,
            MuscleGroup.Hamstrings to 30.0,
            MuscleGroup.Calves to 17.0,
        )
    }
}
