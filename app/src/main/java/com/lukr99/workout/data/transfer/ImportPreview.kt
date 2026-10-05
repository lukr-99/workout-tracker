package com.lukr99.workout.data.transfer

data class ImportPreview(
    val plan: ImportPlan,
    val summary: ImportSummary,
) {
    val canCommit: Boolean
        get() = plan.issues.none { it.severity == TransferIssueSeverity.Error }
}
