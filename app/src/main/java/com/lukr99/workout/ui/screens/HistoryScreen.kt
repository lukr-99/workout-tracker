package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.WorkoutSessionSummary
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.HistoryViewModel
import com.lukr99.workout.ui.components.EmptyHint
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.ScreenHeader
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.TextMid

/** Past-session list, searchable, drilling into [WorkoutDetailScreen]. */
@Composable
fun HistoryScreen(
    vm: HistoryViewModel,
    units: UnitSystem,
    onOpen: (String) -> Unit,
) {
    val history by vm.history.collectAsState()
    val search by vm.search.collectAsState()

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("History", "Past workouts") }
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = TextMid, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                BasicTextField(
                    value = search, onValueChange = vm::setSearch, singleLine = true, modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = MaterialTheme.colorScheme.onBackground)),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner -> if (search.isEmpty()) Text("Search workouts…", color = TextMid, style = MaterialTheme.typography.bodyLarge); inner() },
                )
            }
        }
        if (history.isEmpty()) {
            item { EmptyHint("No workouts logged yet.") }
        } else {
            items(history, key = { it.id }) { summary ->
                HistoryRow(summary, units) { onOpen(summary.id) }
            }
        }
    }
}

@Composable
private fun HistoryRow(summary: WorkoutSessionSummary, units: UnitSystem, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(summary.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            Text(Format.date(summary.completedDateUtc ?: summary.startedAtUtc), style = MaterialTheme.typography.labelSmall, color = TextMid)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Metric(summary.exerciseCount.toString(), "exercises")
            Metric(summary.strengthSetCount.toString(), "sets")
            Metric(Format.volume(summary.totalVolumeKg, units), Format.unitLabel(units))
            if (summary.cardioMinutes > 0) Metric(summary.cardioMinutes.toString(), "cardio min")
        }
        if (summary.bodyPartsSummary.isNotBlank()) {
            Text(summary.bodyPartsSummary, style = MaterialTheme.typography.labelSmall, color = TextMid)
        }
    }
}

@Composable
private fun Metric(value: String, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = Numbers, color = MaterialTheme.colorScheme.onBackground)
        Text(" $label", style = MaterialTheme.typography.labelSmall, color = TextMid)
    }
}
