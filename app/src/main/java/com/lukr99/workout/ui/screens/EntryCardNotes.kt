package com.lukr99.workout.ui.screens

import com.lukr99.workout.domain.PreviousEntryNote
import com.lukr99.workout.domain.StrengthSet

/** What a live exercise card shows next to its sets: notes, last time's sets and whether a guide can be opened. */
data class EntryCardNotes(
    val exerciseNote: String = "",
    val previous: PreviousEntryNote? = null,
    val hasGuide: Boolean = false,
    /** The sets from the newest finished workout with this exercise, for the Previous column. */
    val lastTimeSets: List<StrengthSet> = emptyList(),
)
