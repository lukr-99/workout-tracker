package com.lukr99.workout.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.WorkoutSessionSummary
import com.lukr99.workout.domain.nameOf
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.HistoryViewModel
import com.lukr99.workout.ui.LocationsViewModel
import com.lukr99.workout.ui.components.EmptyState
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.LocationPickerSheet
import com.lukr99.workout.ui.components.SearchField
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/**
 * Past-session list, searchable, drilling into [WorkoutDetailScreen]. A long press starts picking
 * workouts to move to another location together, which fixes history logged before locations.
 */
@Composable
fun HistoryScreen(
    vm: HistoryViewModel,
    locationsVm: LocationsViewModel,
    units: UnitSystem,
    onOpen: (String) -> Unit,
) {
    val history by vm.history.collectAsState()
    val search by vm.search.collectAsState()
    val locations by locationsVm.locations.collectAsState()
    val toast = LocalToast.current
    var picked by remember { mutableStateOf(emptySet<String>()) }
    var moving by remember { mutableStateOf(false) }
    fun toggle(id: String) { picked = if (id in picked) picked - id else picked + id }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            if (picked.isEmpty()) {
                SearchField(value = search, onValueChange = vm::setSearch, placeholder = "Search workouts")
            } else {
                SelectionBar(
                    count = picked.size,
                    onMove = { moving = true },
                    onSelectAll = { picked = history.mapTo(mutableSetOf()) { it.id } },
                    onCancel = { picked = emptySet() },
                )
            }
        }
        if (history.isEmpty()) {
            item {
                if (search.isBlank()) {
                    EmptyState(Icons.Rounded.History, "No workouts yet", "Workouts you finish show up here, newest first. Tap Start below to log one.")
                } else {
                    EmptyState(Icons.Rounded.SearchOff, "No workouts match “${search.trim()}”", "Search looks at workout names. Try a shorter word.")
                }
            }
        } else {
            items(history, key = { it.id }) { summary ->
                HistoryRow(
                    summary = summary,
                    units = units,
                    locations = locations,
                    picked = summary.id in picked,
                    onClick = { if (picked.isEmpty()) onOpen(summary.id) else toggle(summary.id) },
                    onLongClick = { toggle(summary.id) },
                )
            }
        }
    }

    if (moving) {
        LocationPickerSheet(
            title = "Move ${picked.size} ${if (picked.size == 1) "workout" else "workouts"} to",
            locations = locations,
            selectedId = null,
            showSelection = false,
            onPick = { locationId ->
                val ids = picked
                locationsVm.moveWorkouts(ids, locationId) { moved ->
                    toast("Moved $moved ${if (moved == 1) "workout" else "workouts"} to ${locations.nameOf(locationId)}")
                }
                picked = emptySet()
            },
            onAddGym = { name ->
                val ids = picked
                locationsVm.addGym(name) { gym ->
                    locationsVm.moveWorkouts(ids, gym.id) { moved -> toast("Moved $moved to ${gym.name}") }
                }
                picked = emptySet()
            },
            onDismiss = { moving = false },
        )
    }
}

@Composable
private fun SelectionBar(count: Int, onMove: () -> Unit, onSelectAll: () -> Unit, onCancel: () -> Unit) {
    val colors = EmberTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.primarySoft).padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onCancel) { Icon(Icons.Rounded.Close, "Stop selecting", tint = colors.textPrimary) }
        Text("$count selected", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
        TextButton(onClick = onSelectAll) { Text("All") }
        TextButton(onClick = onMove) {
            Icon(Icons.Rounded.Place, null, modifier = Modifier.padding(end = 4.dp))
            Text("Move to location", fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    summary: WorkoutSessionSummary,
    units: UnitSystem,
    locations: List<Location>,
    picked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (picked) colors.primarySoft else MaterialTheme.colorScheme.surface)
            .then(if (picked) Modifier.border(1.dp, colors.primary, shape) else Modifier)
            .combinedClickable(role = Role.Button, onLongClickLabel = "Select", onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (picked) Icon(Icons.Rounded.CheckCircle, "Selected", tint = colors.primaryText, modifier = Modifier.padding(end = 8.dp))
            Text(summary.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            Text(Format.date(summary.completedDateUtc ?: summary.startedAtUtc), style = MaterialTheme.typography.labelSmall, color = EmberTheme.colors.textSecondary)
        }
        Text(locations.nameOf(summary.locationId), style = MaterialTheme.typography.labelSmall, color = EmberTheme.colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Metric(summary.exerciseCount.toString(), "exercises")
            Metric(summary.strengthSetCount.toString(), "sets")
            Metric(Format.volume(summary.totalVolumeKg, units), Format.unitLabel(units))
            if (summary.cardioMinutes > 0) Metric(summary.cardioMinutes.toString(), "cardio min")
        }
        if (summary.bodyPartsSummary.isNotBlank()) {
            Text(summary.bodyPartsSummary, style = MaterialTheme.typography.labelSmall, color = EmberTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun Metric(value: String, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = Numbers, color = MaterialTheme.colorScheme.onBackground)
        Text(" $label", style = MaterialTheme.typography.labelSmall, color = EmberTheme.colors.textSecondary)
    }
}
