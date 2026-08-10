package com.lukr99.workout.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.run.Pace
import kotlinx.coroutines.launch
import com.lukr99.workout.domain.run.RunTrace
import com.lukr99.workout.domain.run.TracePoint
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.components.ChartPoint
import com.lukr99.workout.ui.components.EmptyHint
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.ProgressChart
import com.lukr99.workout.ui.components.SectionCard
import com.lukr99.workout.ui.components.StatTile
import com.lukr99.workout.ui.run.components.RunMap
import com.lukr99.workout.ui.run.components.SplitTable

/**
 * Run detail (R2): the full map trace (camera fit to the route), hero stats, a per-km/mi **split
 * table**, **pace + elevation charts**, and edit-notes / delete. Reads a single run (with its trace)
 * via [RunViewModel.openRun]; edits/delete go back through the repository.
 */
@Composable
fun RunDetailScreen(
    vm: RunViewModel,
    runId: String,
    units: UnitSystem,
    onBack: () -> Unit,
) {
    val run by vm.detail.collectAsState()
    val routeName by vm.detailRouteName.collectAsState()
    var editing by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var shareMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun launchShare(intent: android.content.Intent) {
        runCatching { context.startActivity(android.content.Intent.createChooser(intent, "Share run")) }
    }

    LaunchedEffect(runId) { vm.openRun(runId) }

    val current = run
    if (current == null || current.id != runId) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyHint("Loading run…") }
        return
    }

    val splitMeters = if (units == UnitSystem.Imperial) Pace.METERS_PER_MILE else Pace.METERS_PER_KM
    val splits = remember(current.id, units) { Pace.splits(current.trace, splitMeters) }
    val emberArgb = MaterialTheme.colorScheme.primary.toArgb()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(280.dp)) {
            RunMap(
                userLocationEnabled = false,
                traceSegments = RunTrace.segments(current.trace).map { seg -> seg.map { it.lat to it.lon } },
                traceColor = emberArgb,
                fitTrace = true,
                modifier = Modifier.fillMaxSize(),
            )
            RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.align(Alignment.TopStart).padding(12.dp)) { onBack() }
            Row(
                Modifier.align(Alignment.TopEnd).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box {
                    RoundIcon(Icons.Rounded.Share, "Share run", Modifier) { shareMenu = true }
                    DropdownMenu(expanded = shareMenu, onDismissRequest = { shareMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Share image") },
                            onClick = {
                                shareMenu = false
                                scope.launch {
                                    runCatching { vm.shareCardIntent(runId, units == UnitSystem.Imperial) }
                                        .onSuccess { launchShare(it) }
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Export GPX") },
                            onClick = {
                                shareMenu = false
                                scope.launch {
                                    runCatching { vm.gpxShareIntent(runId) }.onSuccess { launchShare(it) }
                                }
                            },
                        )
                    }
                }
                RoundIcon(Icons.Rounded.Delete, "Delete run", Modifier) { confirmingDelete = true }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Title + date + edit.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        current.notes.ifBlank { "Run" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        Format.date(current.startedAtUtc),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    routeName?.let { name ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Map, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                "  Planned route · $name",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                RoundIcon(Icons.Rounded.Edit, "Edit notes", Modifier, surface = true) { editing = true }
            }

            // Hero stats (2×2).
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Distance", Format.distance(current.distanceMeters, units), Modifier.weight(1f))
                StatTile("Moving time", Pace.formatDuration(current.movingSeconds), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Avg pace", paceLabel(current.avgPaceSecPerKm, units), Modifier.weight(1f))
                StatTile("Elevation", "${current.elevationGainM.toInt()} m", Modifier.weight(1f))
            }

            if (splits.isNotEmpty()) {
                SectionCard(title = "Splits") { SplitTable(splits, units) }
                SectionCard(title = "Pace") {
                    ProgressChart(
                        points = splits.filter { it.isFull }.map {
                            val p = if (units == UnitSystem.Imperial) Pace.paceSecPerMile(it.paceSecPerKm) else it.paceSecPerKm
                            ChartPoint(label = "${it.index}", value = p)
                        },
                        height = 150.dp,
                        valueFormat = { "${Pace.formatPace(it)} /${if (units == UnitSystem.Imperial) "mi" else "km"}" },
                    )
                }
            }

            val elevationPoints = remember(current.id) { elevationChart(current.trace) }
            if (elevationPoints.size >= 2) {
                SectionCard(title = "Elevation") {
                    ProgressChart(
                        points = elevationPoints,
                        height = 150.dp,
                        valueFormat = { "${it.toInt()} m" },
                    )
                }
            }
        }
    }

    if (editing) {
        NotesDialog(
            initial = current.notes,
            onDismiss = { editing = false },
            onSave = { vm.updateNotes(current.id, it); editing = false },
        )
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete run?") },
            text = { Text("This permanently removes the run and its trace.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    vm.deleteRun(current.id)
                    onBack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun NotesDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Run notes") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Morning run, felt strong…") },
                singleLine = false,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RoundIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier,
    surface: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = if (surface) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    Box(
        modifier.size(if (surface) 40.dp else 44.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

private fun paceLabel(secPerKm: Double, units: UnitSystem): String {
    val perUnit = if (units == UnitSystem.Imperial) Pace.paceSecPerMile(secPerKm) else secPerKm
    return "${Pace.formatPace(perUnit)} /${if (units == UnitSystem.Imperial) "mi" else "km"}"
}

/** Downsample the trace's elevation profile to ~40 points labelled by cumulative distance. */
private fun elevationChart(trace: List<TracePoint>): List<ChartPoint> {
    val withElev = trace.filter { it.elevationM != null }
    if (withElev.size < 2) return emptyList()
    var cum = 0.0
    val series = ArrayList<Pair<Double, Double>>(withElev.size)
    for (i in withElev.indices) {
        if (i > 0 && !withElev[i].segmentStart) {
            cum += Pace.haversineMeters(
                withElev[i - 1].lat, withElev[i - 1].lon, withElev[i].lat, withElev[i].lon,
            )
        }
        series += cum to (withElev[i].elevationM ?: 0.0)
    }
    val step = (series.size / 40).coerceAtLeast(1)
    return series.filterIndexed { i, _ -> i % step == 0 || i == series.lastIndex }
        .map { (distM, elev) -> ChartPoint(label = "%.1f km".format(distM / Pace.METERS_PER_KM), value = elev) }
}
