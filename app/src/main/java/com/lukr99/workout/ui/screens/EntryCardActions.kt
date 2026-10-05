package com.lukr99.workout.ui.screens

import com.lukr99.workout.domain.CardioEntryData

/** Everything one exercise card in the live workout can do. Set callbacks take the set id. */
data class EntryCardActions(
    val onOpenMenu: () -> Unit,
    val onEditSuperset: () -> Unit,
    val onToggleCollapsed: () -> Unit,
    val onToggleWeightUnit: () -> Unit,
    val onStart: () -> Unit,
    val onFinish: () -> Unit,
    val onReopen: () -> Unit,
    val onReps: (String, Int) -> Unit,
    val onWeight: (String, Double) -> Unit,
    val onToggleDone: (String) -> Unit,
    val onOptions: (String) -> Unit,
    val onAddSet: () -> Unit,
    val onCardioChange: (CardioEntryData) -> Unit,
    val onEditNote: () -> Unit,
    val onShowGuide: () -> Unit,
)
