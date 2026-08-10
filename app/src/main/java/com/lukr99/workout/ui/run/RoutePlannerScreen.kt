package com.lukr99.workout.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.run.components.RunMap
import kotlinx.coroutines.launch

/**
 * Route planner (R3): tap the map to drop waypoints; the [RoutePlannerViewModel] snaps them to
 * roads/paths and draws the snapped ember line + waypoint markers. Undo/clear, then name + save the
 * route ([com.lukr99.workout.domain.run.Route]). Starting a run from a saved route lands in a later slice.
 */
@Composable
fun RoutePlannerScreen(
    vm: RoutePlannerViewModel,
    units: UnitSystem,
    onBack: () -> Unit,
) {
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val waypoints by vm.waypoints.collectAsState()
    val snapped by vm.snapped.collectAsState()
    val snapping by vm.snapping.collectAsState()
    val ember = MaterialTheme.colorScheme.primary.toArgb()
    var naming by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        RunMap(
            userLocationEnabled = true,
            traceSegments = snapped?.points?.map { it.lat to it.lon }?.let { listOf(it) } ?: emptyList(),
            traceColor = ember,
            waypoints = waypoints.map { it.lat to it.lon },
            onMapTap = { lat, lon -> vm.addWaypoint(lat, lon) },
            modifier = Modifier.fillMaxSize(),
        )

        CircleIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.align(Alignment.TopStart).padding(12.dp)) { onBack() }

        // Hint / snapping chip.
        Text(
            when {
                snapping -> "Snapping to roads…"
                waypoints.isEmpty() -> "Tap the map to drop waypoints"
                waypoints.size == 1 -> "Tap again to draw a route"
                else -> Format.distance(vm.distanceMeters, units)
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )

        if (snapping) {
            CircularProgressIndicator(
                Modifier.align(Alignment.Center).size(36.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Bottom controls.
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PillButton(
                label = "Undo",
                icon = Icons.Rounded.Undo,
                container = MaterialTheme.colorScheme.surface,
                content = MaterialTheme.colorScheme.onSurface,
                enabled = waypoints.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) { vm.undo() }
            PillButton(
                label = "Save route",
                icon = null,
                container = MaterialTheme.colorScheme.primary,
                content = MaterialTheme.colorScheme.onPrimary,
                enabled = (snapped?.points?.size ?: 0) >= 2 && !snapping,
                modifier = Modifier.weight(1.4f),
            ) { naming = true }
        }
    }

    if (naming) {
        NameRouteDialog(
            defaultDistance = Format.distance(vm.distanceMeters, units),
            onDismiss = { naming = false },
            onSave = { name ->
                naming = false
                scope.launch {
                    val saved = vm.save(name)
                    toast(if (saved != null) "Route saved" else "Couldn't save route")
                    if (saved != null) onBack()
                }
            },
        )
    }
}

@Composable
private fun NameRouteDialog(defaultDistance: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save route") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(defaultDistance, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("River loop") },
                )
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier.clip(RoundedCornerShape(26.dp)).background(container.copy(alpha = alpha))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, label, tint = content, modifier = Modifier.size(20.dp))
            Text("  ", fontSize = 2.sp)
        }
        Text(label, color = content, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CircleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.size(44.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
    }
}
