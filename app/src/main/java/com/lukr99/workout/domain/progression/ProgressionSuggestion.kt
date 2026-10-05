package com.lukr99.workout.domain.progression

data class ProgressionSuggestion(
    val exerciseId: String,
    val exerciseName: String = "",
    val scheme: String,
    val status: SuggestionStatus,
    val targets: List<TargetSet> = emptyList(),
    val rationale: String,
    val isDeload: Boolean = false,
    val basedOnSessionIds: List<String> = emptyList(),
    val currentEstimated1RmKg: Double? = null,
)
