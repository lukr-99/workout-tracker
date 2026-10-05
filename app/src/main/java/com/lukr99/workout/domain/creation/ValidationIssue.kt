package com.lukr99.workout.domain.creation

data class ValidationIssue(
    val path: String,
    val message: String,
    val severity: IssueSeverity,
)
