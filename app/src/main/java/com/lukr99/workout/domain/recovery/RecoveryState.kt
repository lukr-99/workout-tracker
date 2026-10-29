package com.lukr99.workout.domain.recovery

/** How recovered a muscle is now, from its readiness and the [RecoveryConfig] thresholds. */
enum class RecoveryState { Ready, Recovering, Fatigued }
