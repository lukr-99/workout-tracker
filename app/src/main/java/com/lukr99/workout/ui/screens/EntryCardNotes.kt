package com.lukr99.workout.ui.screens

import com.lukr99.workout.domain.PreviousEntryNote

/** What a live exercise card shows next to its sets: notes and whether a guide can be opened. */
data class EntryCardNotes(
    val exerciseNote: String = "",
    val previous: PreviousEntryNote? = null,
    val hasGuide: Boolean = false,
)
