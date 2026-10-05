package com.lukr99.workout.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Body parts as wrapping chips. Used once for the primary part (pick one) and once for the
 * secondary parts (pick any). Matching ignores case, so "chest" and "Chest" are the same chip.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BodyPartChips(
    options: List<String>,
    isSelected: (String) -> Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { part ->
            FilterChip(part, isSelected(part), { onToggle(part) })
        }
    }
}
