package com.lukr99.workout.ui.screens

/** What one template exercise card can do. */
internal class TemplatePlanActions(
    val onChange: (TemplateRow) -> Unit,
    val onEditReps: () -> Unit,
    val onEditNote: () -> Unit,
    val onMoveUp: (() -> Unit)?,
    val onMoveDown: (() -> Unit)?,
    val onToggleSuperset: (() -> Unit)?,
    val onRemove: () -> Unit,
)
