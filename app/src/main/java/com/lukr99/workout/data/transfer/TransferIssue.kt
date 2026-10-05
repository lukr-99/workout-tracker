package com.lukr99.workout.data.transfer

data class TransferIssue(
    val code: String,
    val message: String,
    val severity: TransferIssueSeverity,
    val row: Int? = null,
    val field: String? = null,
)
