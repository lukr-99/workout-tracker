package com.lukr99.workout.domain.records

data class RecordSource(
    val sessionId: String,
    val sessionName: String,
    val dateUtc: Long,
    val entryId: String,
    val setId: String?,
    val setNumber: Int?,
)
