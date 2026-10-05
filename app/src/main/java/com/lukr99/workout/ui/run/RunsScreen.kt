package com.lukr99.workout.ui.run

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.components.EmptyHint
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.ScreenHeader
import com.lukr99.workout.ui.run.components.MiniRoute
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/**
 * Run Mode hub — the tab that replaces History's old slot. Recent runs + saved routes + a prominent
 * start. R5 adds route management (rename / delete / save offline) and GPX import.
 */
@Composable
fun RunsScreen(
    vm: RunViewModel,
    units: UnitSystem,
    onStartRun: () -> Unit,
    onOpenRun: (String) -> Unit,
    onPlanRoute: () -> Unit,
    onStartRoute: (String) -> Unit,
) {
    val runs by vm.runs.collectAsState()
    val routes by vm.routes.collectAsState()
    val status by vm.status.collectAsState()
    val context = LocalContext.current

    var renaming by remember { mutableStateOf<Route?>(null) }
    var deleting by remember { mutableStateOf<Route?>(null) }

    // Surface transient GPX-import / offline-cache messages as a toast, then clear.
    LaunchedEffect(status) {
        status?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.clearStatus()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { vm.importGpx(it) } }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Runs", weekSummary(runs, units)) }
        item { StartRunButton(onStartRun) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(Icons.Rounded.Map, "Plan a route", Modifier.weight(1f), onPlanRoute)
                SecondaryButton(Icons.Rounded.Upload, "Import GPX", Modifier.weight(1f)) {
                    importLauncher.launch(arrayOf("application/gpx+xml", "application/xml", "text/xml", "*/*"))
                }
            }
        }

        if (routes.isNotEmpty()) {
            item { SectionLabel("Saved routes") }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    routes.forEach { route ->
                        RouteCard(
                            route = route,
                            units = units,
                            onStart = { onStartRoute(route.id) },
                            onRename = { renaming = route },
                            onSaveOffline = { vm.downloadRouteOffline(route) },
                            onDelete = { deleting = route },
                        )
                    }
                }
            }
        }

        item { SectionLabel("Recent runs") }
        if (runs.isEmpty()) {
            item { EmptyHint("No runs yet. Tap Start a run to record your first.") }
        } else {
            items(runs, key = { it.id }) { run -> RunRow(run, units) { onOpenRun(run.id) } }
        }
    }

    renaming?.let { route ->
        RenameRouteDialog(
            initial = route.name,
            onDismiss = { renaming = null },
            onSave = { vm.renameRoute(route, it); renaming = null },
        )
    }
    deleting?.let { route ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete route?") },
            text = { Text("This removes “${route.name.ifBlank { "Route" }}” permanently.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteRoute(route.id); deleting = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RenameRouteDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename route") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Riverside loop") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** "12.3 km this week", counted from Monday. */
private fun weekSummary(runs: List<Run>, units: UnitSystem): String {
    val zone = java.time.ZoneId.systemDefault()
    val monday = java.time.LocalDate.now(zone).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val start = monday.atStartOfDay(zone).toInstant().toEpochMilli()
    val meters = runs.filter { it.startedAtUtc >= start }.sumOf { it.distanceMeters }
    val value = if (units == UnitSystem.Imperial) meters / 1_609.344 else meters / 1_000.0
    val unit = if (units == UnitSystem.Imperial) "mi" else "km"
    return "${"%.1f".format(value).replace(Regex("[.,]0$"), "")} $unit this week"
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = EmberTheme.colors.textSecondary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SecondaryButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.height(50.dp).clip(shape).background(colors.surface).border(1.dp, colors.border, shape)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = colors.primaryText, modifier = Modifier.size(19.dp))
        Text("  $label", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
    }
}

@Composable
private fun RouteCard(
    route: Route,
    units: UnitSystem,
    onStart: () -> Unit,
    onRename: () -> Unit,
    onSaveOffline: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = EmberTheme.colors
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.width(156.dp).clip(shape).background(colors.surface).border(1.dp, colors.border, shape)
            .clickable(onClickLabel = "Run ${route.name.ifBlank { "this route" }}", onClick = onStart).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MiniRoute(route.encodedPolyline, Modifier.fillMaxWidth().height(80.dp), points = route.points.map { it.lat to it.lon })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(route.name.ifBlank { "Route" }, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 1)
                Text(Format.distance(route.distanceMeters, units), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            }
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.MoreVert, "Options for ${route.name.ifBlank { "route" }}", tint = colors.textSecondary)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Save offline") }, onClick = { menu = false; onSaveOffline() })
                    DropdownMenuItem(text = { Text("Delete", color = colors.danger) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun StartRunButton(onStartRun: () -> Unit) {
    val colors = EmberTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.primary)
            .clickable(role = Role.Button, onClick = onStartRun).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(colors.onPrimary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PlayArrow, null, tint = colors.onPrimary, modifier = Modifier.size(28.dp))
        }
        Column {
            Text("Start a run", style = MaterialTheme.typography.titleLarge, color = colors.onPrimary)
            Text("GPS route, pace and splits", style = MaterialTheme.typography.labelLarge, color = colors.onPrimary.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun RunRow(run: Run, units: UnitSystem, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(colors.surface).border(1.dp, colors.border, shape)
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MiniRoute(run.encodedPolyline, Modifier.width(72.dp).height(54.dp))
        Column(Modifier.weight(1f)) {
            Text(run.notes.lineSequence().firstOrNull()?.ifBlank { null } ?: "Run", fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 1)
            Text(Format.relativeDay(run.startedAtUtc), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            Text(
                "${Pace.formatDuration(run.durationSeconds)} \u00b7 ${Pace.formatPace(paceForUnits(run.avgPaceSecPerKm, units))} ${paceLabel(units)}",
                style = Numbers.copy(fontSize = 15.sp),
                color = colors.textSecondary,
            )
        }
        val (value, unit) = Format.distance(run.distanceMeters, units).split(' ').let { it[0] to it[1] }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = Numbers.copy(fontSize = 26.sp), color = colors.textPrimary)
            Text(" $unit", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

private fun paceForUnits(secPerKm: Double, units: UnitSystem): Double =
    if (units == UnitSystem.Imperial) Pace.paceSecPerMile(secPerKm) else secPerKm

private fun paceLabel(units: UnitSystem): String = if (units == UnitSystem.Imperial) "/mi" else "/km"
