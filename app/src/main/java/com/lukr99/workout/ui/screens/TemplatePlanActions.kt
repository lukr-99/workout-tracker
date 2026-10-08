package com.lukr99.workout.ui.screens

import androidx.compose.ui.Modifier

/** What one template exercise card can do. */
internal class TemplatePlanActions(
    val onChange: (TemplateRow) -> Unit,
    val onEditReps: () -> Unit,
    val onEditNote: () -> Unit,
    val onMoveUp: (() -> Unit)?,
    val onMoveDown: (() -> Unit)?,
    val onToggleSuperset: (() -> Unit)?,
    val onRemove: () -> Unit,
    /** Dissolves the superset this row starts; only the first row of a superset has it. */
    val onUngroup: (() -> Unit)? = null,
    /** The drag handle's gesture, from the editor's reorder state. */
    val dragHandle: Modifier = Modifier,
)
