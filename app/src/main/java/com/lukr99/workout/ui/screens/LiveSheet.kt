package com.lukr99.workout.ui.screens

import com.lukr99.workout.domain.Exercise

/** The one sheet or dialog open over the live workout, if any. */
sealed interface LiveSheet {
    data object AddExercise : LiveSheet
    data class QuickCreate(val name: String) : LiveSheet
    data class Replace(val entryId: String) : LiveSheet
    data class Menu(val entryId: String) : LiveSheet
    data class SetOptions(val entryId: String, val setId: String) : LiveSheet
    data class Note(val target: NoteTarget) : LiveSheet
    data class Guide(val exercise: Exercise) : LiveSheet
    data class Superset(val groupId: Int) : LiveSheet
    data object ConfirmFinish : LiveSheet
    data object ConfirmDiscard : LiveSheet
}
