package com.lukr99.workout.domain.creation

data class CreationResult<T>(
    val value: T,
    val issues: List<ValidationIssue> = emptyList(),
) {
    val isValid: Boolean get() = issues.none { it.severity == IssueSeverity.Error }

    fun requireValid(): T {
        require(isValid) { issues.filter { it.severity == IssueSeverity.Error }.joinToString { it.message } }
        return value
    }
}
