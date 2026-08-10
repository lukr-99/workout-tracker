package com.lukr99.workout.ui.run

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lukr99.workout.data.run.RunCues
import com.lukr99.workout.domain.run.LiveRunState
import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.RouteDeviation
import com.lukr99.workout.domain.run.RunStats
import com.lukr99.workout.domain.run.RunTrace
import com.lukr99.workout.domain.run.RunTracker
import com.lukr99.workout.domain.run.SplitCue
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.MusicMiniControls
import com.lukr99.workout.ui.components.rememberReduceMotion
import com.lukr99.workout.ui.run.components.RunMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Live-run screen (R1). Renders the dark map with the growing **ember trace**, big live metrics
 * (distance · moving time · pace), and start/countdown/pause/resume/finish controls. Recording,
 * persistence, and screen-off survival are owned by [LiveRunViewModel] → the
 * [com.lukr99.workout.data.location.RunSessionController] + foreground service; this screen only
 * requests location just-in-time and reflects/drives the shared run state, so closing it leaves the
 * run recording in the background and reopening re-attaches to it.
 */
@Composable
fun LiveRunScreen(
    vm: LiveRunViewModel,
    units: UnitSystem,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsState()
    val trace by vm.trace.collectAsState()
    val planned by vm.plannedRoute.collectAsState()
    val emberColor = MaterialTheme.colorScheme.primary

    fun hasFineLocation(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    var locationGranted by remember { mutableStateOf(hasFineLocation()) }
    var recenterSignal by remember { mutableIntStateOf(0) }
    var compassSignal by remember { mutableIntStateOf(0) }
    // Default matches the map's initial follow mode (locked to heading). The compass button flips it.
    var headingFollow by remember { mutableStateOf(true) }
    var mapBearing by remember { mutableFloatStateOf(0f) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var confirmingFinish by remember { mutableStateOf(false) }
    var finishSummary by remember { mutableStateOf<RunStats.RunSummary?>(null) }
    val reduceMotion = rememberReduceMotion()

    // Audio/haptic cues (split announcements, countdown ticks, finish flourish). Freed on leave.
    val cues = remember { RunCues(context) }
    DisposableEffect(Unit) { onDispose { cues.release() } }

    // Fire a unit-aware split cue whenever the run crosses a km/mi mark. The pure trigger
    // ([SplitCue]) decides which marks were crossed between the last and current distance.
    val splitMeters = if (units == UnitSystem.Imperial) Pace.METERS_PER_MILE else Pace.METERS_PER_KM
    var lastCuedMeters by remember { mutableDoubleStateOf(0.0) }
    LaunchedEffect(state.distanceMeters, state.phase) {
        val curr = state.distanceMeters
        if (state.phase == RunTracker.Phase.Recording) {
            val imperial = units == UnitSystem.Imperial
            val pace = if (imperial) Pace.paceSecPerMile(state.avgPaceSecPerKm) else state.avgPaceSecPerKm
            SplitCue.crossedMarks(lastCuedMeters, curr, splitMeters).forEach { mark ->
                cues.splitCue(mark, imperial, pace)
            }
        }
        lastCuedMeters = curr
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        locationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || hasFineLocation()
        if (locationGranted && !state.isActive) countdown = 3
    }

    fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    // Countdown 3-2-1 → start recording, with a haptic tick per number and a start flourish.
    LaunchedEffect(countdown) {
        val c = countdown ?: return@LaunchedEffect
        if (c <= 0) {
            cues.startCue()
            lastCuedMeters = 0.0
            vm.start()
            countdown = null
        } else {
            cues.countdownTick()
            delay(1_000)
            countdown = c - 1
        }
    }

    // Anything that isn't actively recording/paused (Idle, or a just-finished run) shows the Start
    // affordance rather than dead recording controls.
    val idle = !state.isActive

    // Off-route distance (informational only — a planned route never constrains the run).
    val offRouteMeters = remember(state.lastLat, state.lastLon, planned) {
        val lat = state.lastLat
        val lon = state.lastLon
        if (planned.isEmpty() || lat == null || lon == null) null
        else RouteDeviation.distanceToRouteMeters(lat, lon, planned)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        RunMap(
            userLocationEnabled = locationGranted,
            recenterSignal = recenterSignal,
            compassSignal = compassSignal,
            headingFollow = headingFollow,
            traceSegments = RunTrace.segments(trace).map { seg -> seg.map { it.lat to it.lon } },
            traceColor = emberColor.toArgb(),
            plannedRoute = planned,
            onBearingChanged = { mapBearing = it },
            modifier = Modifier.fillMaxSize(),
        )

        CircleIconButton(Icons.Rounded.Close, "Close", Modifier.align(Alignment.TopStart).padding(12.dp)) { onClose() }

        // Live metrics panel (top) once recording.
        if (!idle) {
            MetricsPanel(
                state = state,
                units = units,
                offRouteMeters = offRouteMeters,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp, start = 64.dp, end = 16.dp),
            )
        }

        // Right-edge side stack: the small music button above the recenter control.
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MusicMiniControls()
            if (locationGranted) {
                // Compass toggle: one tap locks the map to the phone's heading (road ahead up), the
                // next taps back to north-up. Ember tint means it's currently locked to your heading;
                // the needle always points to true north. Grouped with recenter/music, off the stats.
                CircleIconButton(
                    Icons.Rounded.Navigation,
                    if (headingFollow) "Face north" else "Lock to heading",
                    Modifier,
                    iconRotation = -mapBearing,
                    tint = if (headingFollow) emberColor else MaterialTheme.colorScheme.onSurface,
                ) {
                    headingFollow = !headingFollow
                    compassSignal++
                }
                CircleIconButton(Icons.Rounded.MyLocation, "Recenter", Modifier) {
                    headingFollow = true
                    recenterSignal++
                }
            }
        }

        // Bottom controls.
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp)) {
            when {
                !locationGranted -> LocationRationale(::requestPermissions, Modifier.fillMaxWidth())
                idle -> StartButton(Modifier.align(Alignment.Center)) {
                    if (hasFineLocation()) countdown = 3 else requestPermissions()
                }
                else -> RecordingControls(
                    paused = state.phase == RunTracker.Phase.Paused,
                    onPause = vm::pause,
                    onResume = vm::resume,
                    onFinish = { confirmingFinish = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Countdown overlay. Respect "remove animations": skip the fade when reduce-motion is on.
        val countdownVisible = countdown != null && (countdown ?: 0) > 0
        if (reduceMotion) {
            if (countdownVisible) CountdownOverlay(countdown)
        } else {
            AnimatedVisibility(visible = countdownVisible) { CountdownOverlay(countdown) }
        }
    }

    if (confirmingFinish) {
        FinishSheet(
            state = state,
            units = units,
            onDiscard = {
                confirmingFinish = false
                vm.discard()
                onClose()
            },
            onSave = {
                confirmingFinish = false
                scope.launch {
                    val summary = vm.finish()
                    cues.finishCue(
                        Format.distance(summary.distanceMeters, units),
                        Pace.formatDuration(summary.movingSeconds),
                    )
                    finishSummary = summary
                }
            },
            onCancel = { confirmingFinish = false },
        )
    }

    finishSummary?.let { summary ->
        FinishSummarySheet(
            summary = summary,
            units = units,
            onDone = { finishSummary = null; onClose() },
        )
    }
}

@Composable
private fun CountdownOverlay(countdown: Int?) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${countdown ?: ""}",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 120.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Post-save summary: headline metrics + any personal records the run just set. */
@Composable
private fun FinishSummarySheet(
    summary: RunStats.RunSummary,
    units: UnitSystem,
    onDone: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Run saved", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Metric("Distance", Format.distance(summary.distanceMeters, units))
                Metric("Time", Pace.formatDuration(summary.movingSeconds))
                Metric("Pace", paceLabel(summary.avgPaceSecPerKm, units))
            }
            if (summary.newRecords.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    summary.newRecords.forEach { pr ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏅", fontSize = 16.sp)
                            Text(
                                "  New record · ${prLabel(pr)}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary).clickable(onClick = onDone)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Done", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun prLabel(pr: RunStats.PrKind): String = when (pr) {
    RunStats.PrKind.Fastest1k -> "Fastest 1K"
    RunStats.PrKind.Fastest5k -> "Fastest 5K"
    RunStats.PrKind.Fastest10k -> "Fastest 10K"
    RunStats.PrKind.FastestHalf -> "Fastest half marathon"
    RunStats.PrKind.LongestRun -> "Longest run"
    RunStats.PrKind.MostElevation -> "Most elevation"
    RunStats.PrKind.BestAvgPace -> "Best average pace"
}

@Composable
private fun MetricsPanel(state: LiveRunState, units: UnitSystem, offRouteMeters: Double?, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(
            Format.distance(state.distanceMeters, units),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Metric("Time", Pace.formatDuration(state.movingSeconds))
            Metric("Pace", paceLabel(state.avgPaceSecPerKm, units))
            Metric("Elev", "${state.elevationGainM.toInt()} m")
        }
        // Off-route is a gentle heads-up only — never a warning, never blocking.
        if (offRouteMeters != null && offRouteMeters > OFF_ROUTE_THRESHOLD_M) {
            Text(
                "${offRouteMeters.toInt()} m off route",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (state.autoPaused) {
            Text(
                "Auto-paused",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/** Only surface an off-route hint once you're clearly off the line — never for GPS wobble. */
private const val OFF_ROUTE_THRESHOLD_M = 35.0

private fun paceLabel(secPerKm: Double, units: UnitSystem): String {
    val perUnit = if (units == UnitSystem.Imperial) Pace.paceSecPerMile(secPerKm) else secPerKm
    val suffix = if (units == UnitSystem.Imperial) "/mi" else "/km"
    return "${Pace.formatPace(perUnit)} $suffix"
}

@Composable
private fun StartButton(modifier: Modifier, onClick: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(84.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, "Start run", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(44.dp))
        }
        Text(
            "Start",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun RecordingControls(
    paused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        // Finish (square stop) — left, secondary surface.
        ControlButton(
            icon = Icons.Rounded.Stop,
            label = "Finish",
            container = MaterialTheme.colorScheme.surface,
            content = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            onClick = onFinish,
        )
        // Pause/Resume — primary ember.
        ControlButton(
            icon = if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
            label = if (paused) "Resume" else "Pause",
            container = MaterialTheme.colorScheme.primary,
            content = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.weight(1f),
            onClick = if (paused) onResume else onPause,
        )
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier.clip(RoundedCornerShape(28.dp)).background(container).clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, tint = content, modifier = Modifier.size(22.dp))
        Text("  $label", color = content, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FinishSheet(
    state: LiveRunState,
    units: UnitSystem,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onCancel),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = false) {}
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Finish run?", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Metric("Distance", Format.distance(state.distanceMeters, units))
                Metric("Time", Pace.formatDuration(state.movingSeconds))
                Metric("Pace", paceLabel(state.avgPaceSecPerKm, units))
            }
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary).clickable(onClick = onSave)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Save run", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("Keep running", Modifier.weight(1f), onCancel)
                SecondaryButton("Discard", Modifier.weight(1f), onDiscard)
            }
        }
    }
}

@Composable
private fun SecondaryButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick).padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier,
    iconRotation: Float = 0f,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Box(
        modifier.size(44.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp).rotate(iconRotation),
        )
    }
}

@Composable
private fun LocationRationale(onEnable: () -> Unit, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(
                "  Location for your run",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            "Ember uses your location to draw your route and measure distance and pace. It only tracks " +
                "while a run is active — never in the background.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onEnable).padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Enable location", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
