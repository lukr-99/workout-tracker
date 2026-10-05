package com.lukr99.workout.domain.progression

data class DeloadPolicy(
    val enabled: Boolean = true,
    val stallSessions: Int = 3,
    val minimumImprovementFraction: Double = 0.005,
    val loadReductionFraction: Double = 0.10,
    val volumeReductionFraction: Double = 0.25,
) {
    init {
        require(stallSessions > 0)
        require(minimumImprovementFraction >= 0)
        require(loadReductionFraction in 0.0..<1.0)
        require(volumeReductionFraction in 0.0..<1.0)
    }
}
