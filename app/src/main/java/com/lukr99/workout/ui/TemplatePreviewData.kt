package com.lukr99.workout.ui

import com.lukr99.workout.domain.StrengthSet

/** What the template preview shows besides the template: last time's sets and when it was last done. */
data class TemplatePreviewData(
    val lastSets: Map<String, List<StrengthSet>> = emptyMap(),
    val lastDoneUtc: Long? = null,
)
