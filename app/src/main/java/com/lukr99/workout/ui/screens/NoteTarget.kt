package com.lukr99.workout.ui.screens

/** Which note the note sheet is editing: the whole workout, one exercise in it, or one set. */
sealed interface NoteTarget {
    data object Workout : NoteTarget
    data class Entry(val entryId: String) : NoteTarget
    data class Set(val entryId: String, val setId: String) : NoteTarget
}
