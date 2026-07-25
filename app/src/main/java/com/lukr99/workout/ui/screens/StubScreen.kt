package com.lukr99.workout.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.components.ScreenHeader

/**
 * Shared placeholder body for the Phase 0 screen stubs: the standard [ScreenHeader] over a muted
 * "coming soon" line. Each screen file stays a real `fun XScreen(vm)` so the shape is honest;
 * real content replaces this per feature phase.
 */
@Composable
fun StubScreen(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Top) {
        ScreenHeader(title, subtitle)
        Spacer(Modifier.height(24.dp))
        Text(
            "Nothing here yet — Phase 0 scaffold.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
