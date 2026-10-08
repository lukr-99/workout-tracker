package com.lukr99.workout.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.lukr99.workout.data.health.HealthConnectAvailability
import com.lukr99.workout.settings.ThemeMode
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.SettingsViewModel
import com.lukr99.workout.ui.UpdatesViewModel
import com.lukr99.workout.ui.components.FilterChip
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.RoundIconButton
import com.lukr99.workout.ui.components.SegmentedControl
import com.lukr99.workout.ui.components.rememberReduceMotion
import com.lukr99.workout.ui.theme.EmberTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Settings as one scrolling page, built to CodePrint's settings guide: one card per section, a chip
 * row that jumps to a section and follows the scroll, the jump and scroll highlights, and no Save
 * button because every change saves at once.
 */
@OptIn(FlowPreview::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    updates: UpdatesViewModel,
    onOpenData: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onBack: () -> Unit,
) {
    val settings by vm.settings.collectAsState()
    val updateState by updates.state.collectAsState()
    val health by vm.healthConnectUi.collectAsState()
    val backup by vm.backupState.collectAsState()
    val backupOptions by vm.backupOptions.collectAsState()
    val backupBusy by vm.backupBusy.collectAsState()
    val backupError by vm.backupError.collectAsState()
    val syncState by vm.catalogSync.collectAsState()
    val toast = LocalToast.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted -> vm.onHealthPermissionsResult(granted) }
    val backupFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(vm::enableBackup) }

    LaunchedEffect(health.summary) {
        health.summary?.let { summary ->
            toast("Health Connect: imported ${summary.imported}, exported ${summary.exported}, skipped ${summary.skipped}")
            vm.consumeHealthSummary()
        }
    }

    val sections = SettingsSection.entries
    val list = rememberLazyListState()
    val chips = rememberLazyListState()
    val lineY = with(LocalDensity.current) { 80.dp.roundToPx() }
    val reduceMotion = rememberReduceMotion()
    val jump = remember { sections.map { Animatable(0f) } }
    val scrollHint = remember { sections.map { Animatable(0f) } }
    var jumping by remember { mutableStateOf<Int?>(null) }

    val current by remember {
        derivedStateOf {
            val info = list.layoutInfo
            val tops = sections.indices.map { index ->
                info.visibleItemsInfo.firstOrNull { it.index == index }?.let { it.offset - info.viewportStartOffset }
            }
            jumping ?: SettingsScroll.currentSection(tops, list.firstVisibleItemIndex, lineY, atBottom = !list.canScrollForward)
        }
    }
    LaunchedEffect(current) { chips.animateScrollToItem(current) }
    // Scroll hint: once scrolling has been still for 150 ms, light the edge of the section you landed in.
    LaunchedEffect(Unit) {
        snapshotFlow { current }.distinctUntilChanged().debounce(150).collect { index ->
            if (reduceMotion || jumping != null || !list.isScrollInProgress && list.firstVisibleItemIndex == 0 && index == 0) return@collect
            val hint = scrollHint[index]
            hint.animateTo(1f, tween(120, easing = LinearOutSlowInEasing))
            hint.animateTo(0f, tween(580, easing = FastOutSlowInEasing))
        }
    }

    fun jumpTo(index: Int) = scope.launch {
        jumping = index
        try {
            if (reduceMotion) {
                list.scrollToItem(index)
                jump[index].snapTo(1f)
                delay(900)
                jump[index].snapTo(0f)
            } else {
                list.animateScrollToItem(index)
                jump[index].animateTo(1f, tween(150, easing = LinearOutSlowInEasing))
                delay(350)
                jump[index].animateTo(0f, tween(700, easing = FastOutSlowInEasing))
            }
        } finally {
            jumping = null
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Text("Settings", style = MaterialTheme.typography.displayLarge, color = EmberTheme.colors.textPrimary)
        }
        if (SettingsScroll.showNavigation(sections.size)) {
            LazyRow(
                state = chips,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp).semantics { contentDescription = "Settings sections" },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(sections) { index, section ->
                    FilterChip(section.title, index == current, { jumpTo(index) })
                }
            }
        }
        LazyColumn(state = list, modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 40.dp)) {
            itemsIndexed(sections, key = { _, s -> s.name }) { index, section ->
                SettingsCard(section, jump[index].value, scrollHint[index].value, Modifier.padding(top = if (index == 0) 4.dp else 12.dp)) {
                    when (section) {
                        SettingsSection.Appearance -> {
                            SettingsRow("Theme", "Follows the phone unless you pick one.", first = true)
                            SegmentedControl(
                                options = listOf(ThemeMode.System, ThemeMode.Light, ThemeMode.Dark),
                                selected = settings.themeMode,
                                onSelect = vm::setTheme,
                                label = { it.name },
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            ToggleRow(
                                "Reduce motion",
                                "Jumps and highlights happen at once, with no sliding or fading.",
                                checked = settings.reduceMotion,
                                onChange = vm::setReduceMotion,
                            )
                        }
                        SettingsSection.Workouts -> {
                            SettingsRow("Weight unit", "Logged weights are stored in kilograms either way.", first = true)
                            SegmentedControl(
                                options = listOf(UnitSystem.Metric, UnitSystem.Imperial),
                                selected = settings.units,
                                onSelect = vm::setUnits,
                                label = { if (it == UnitSystem.Metric) "Kilograms" else "Pounds" },
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            SettingsRow("Default rest", "Used when an exercise has no rest of its own.")
                            Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(60, 90, 120, 150, 180).forEach { seconds ->
                                    FilterChip(Format.clock(seconds), settings.defaultRestSeconds == seconds, { vm.setDefaultRest(seconds) })
                                }
                            }
                            val running = syncState is SettingsViewModel.CatalogSyncState.Running
                            ButtonRow(
                                label = "Exercise catalog",
                                hint = when (val state = syncState) {
                                    is SettingsViewModel.CatalogSyncState.Done ->
                                        "Added ${state.summary.added}, updated ${state.summary.updated}, skipped ${state.summary.skipped}."
                                    is SettingsViewModel.CatalogSyncState.Failed -> state.message
                                    else -> "Download the open wger exercise database. Your own exercises are never changed."
                                },
                                hintIsError = syncState is SettingsViewModel.CatalogSyncState.Failed,
                                action = "Sync now",
                                busy = running,
                                busyLabel = "Syncing…",
                                onClick = vm::syncCatalog,
                            )
                        }
                        SettingsSection.HealthConnect -> {
                            val status = health.error ?: healthAvailabilityDetail(health.availability, health.connected)
                            when (health.availability) {
                                HealthConnectAvailability.Available -> if (!health.connected) {
                                    ButtonRow("Not connected", status, "Connect", first = true, busy = health.refreshing) {
                                        healthPermissionLauncher.launch(vm.healthConnectPermissions)
                                    }
                                } else {
                                    SettingsRow("Connected", status, first = true, hintIsError = health.error != null)
                                    ButtonRow("Send workouts and runs", "Exports what Health Connect does not have yet.", "Export", busy = health.operation != null, onClick = vm::exportToHealthConnect)
                                    ButtonRow("Bring workouts in", "Imports workouts from other apps as sessions.", "Import", busy = health.operation != null, onClick = vm::importFromHealthConnect)
                                }
                                HealthConnectAvailability.ProviderUpdateRequired ->
                                    ButtonRow("Update needed", status, "Install", first = true) { openHealthConnectListing(context) }
                                else -> SettingsRow(healthAvailabilityLabel(health.availability, health.connected), status, first = true)
                            }
                            LinkRow("Privacy policy", "What Ember reads and why.", onClick = onOpenPrivacy)
                        }
                        SettingsSection.YourData -> {
                            ToggleRow(
                                label = "Automatic backup",
                                hint = if (backup.enabled) {
                                    "${backupFolderLabel(backup.treeUri)}. ${backupStatus(backup.lastRunUtcMillis, backup.lastResult, backup.lastMessage, backupError)}"
                                } else {
                                    "Saves a full backup to a folder you pick, on this phone or in the cloud."
                                },
                                checked = backup.enabled,
                                first = true,
                                busy = backupBusy,
                            ) { enabled -> if (enabled) backupFolderLauncher.launch(null) else vm.disableBackup() }
                            SettingsRow("How often", null)
                            val interval = if (backup.enabled) backup.intervalHours else backupOptions.intervalHours
                            SegmentedControl(listOf(24L, 168L), interval, { vm.setBackupInterval(it) }, label = { if (it == 24L) "Daily" else "Weekly" }, modifier = Modifier.padding(bottom = 8.dp))
                            SettingsRow("Backups to keep", "Older automatic backups are deleted. Other files are never touched.")
                            val kept = if (backup.enabled) backup.retentionCount else backupOptions.retentionCount
                            SegmentedControl(listOf(3, 7, 14), kept, { vm.setBackupRetention(it) }, label = { it.toString() }, modifier = Modifier.padding(bottom = 8.dp))
                            LinkRow("Import, export and delete", "Ember backups, Lyfta CSV, plain CSV, and deleting all data.", onClick = onOpenData)
                        }
                        SettingsSection.Updates -> {
                            ButtonRow(
                                label = "Version ${updateState.currentVersion}",
                                hint = updateState.status.ifBlank { "Checks GitHub for a newer, signed release." },
                                action = "Check now",
                                first = true,
                                busy = updateState.busy,
                                busyLabel = "Checking…",
                                onClick = updates::check,
                            )
                            LinkRow("All releases on GitHub", "Release notes and the manual download.", outside = true) {
                                runCatching { uriHandler.openUri(UpdatesViewModel.RELEASES_URL) }
                            }
                        }
                    }
                }
            }
            item(key = "footer") {
                Text(
                    "Ember ${updateState.currentVersion}",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmberTheme.colors.textTertiary,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                )
            }
        }
    }

    updateState.offer?.let { offer ->
        AlertDialog(
            onDismissRequest = updates::dismissOffer,
            confirmButton = { TextButton(enabled = !updateState.busy, onClick = updates::downloadAndInstall) { Text("Download and install") } },
            dismissButton = { TextButton(enabled = !updateState.busy, onClick = updates::dismissOffer) { Text("Later") } },
            title = { Text("Ember ${offer.version}") },
            text = { Text(offer.notes.ifBlank { "A new version is available." }.take(600), color = EmberTheme.colors.textSecondary) },
        )
    }
}
