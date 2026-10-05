package com.lukr99.workout.domain.recovery

data class RecoveryConfig(
    val halfLifeHours: Double = 36.0,
    val lookbackHours: Double = 14 * 24.0,
    val weeklyWindowHours: Double = 7 * 24.0,
    val secondaryMuscleContribution: Double = 0.5,
    val setLoadUnits: Double = 1.0,
    val volumeKgPerLoadUnit: Double = 1_000.0,
    val cardioMinutesPerLoadUnit: Double = 30.0,
    val fatigueSaturationLoad: Double = 3.0,
    val readyThreshold: Double = 80.0,
) {
    init {
        require(halfLifeHours > 0 && lookbackHours > 0 && weeklyWindowHours > 0)
        require(secondaryMuscleContribution in 0.0..1.0)
        require(setLoadUnits >= 0 && volumeKgPerLoadUnit > 0 && cardioMinutesPerLoadUnit > 0)
        require(fatigueSaturationLoad > 0)
        require(readyThreshold in 0.01..99.99)
    }
}
