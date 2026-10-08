package com.lukr99.workout.ui

import com.lukr99.workout.domain.ProgressContext
import com.lukr99.workout.domain.recovery.RecoverySnapshot

data class ProgressUiState(
    val loaded: Boolean = false,
    val overview: ProgressOverview = ProgressOverview(),
    val context: ProgressContext = ProgressContext(),
    val weeklyVolume: List<WeeklyVolume> = emptyList(),
    val exercises: List<ExerciseProgressSummary> = emptyList(),
    val recovery: RecoverySnapshot? = null,
)
