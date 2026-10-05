package com.lukr99.workout.ui

import com.lukr99.workout.domain.StrengthSet

/** The sets a newly added exercise starts with, and why, when a progression suggested them. */
data class EntryPrefill(
    val sets: List<StrengthSet>,
    val rationale: String? = null,
)
