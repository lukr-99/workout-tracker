package com.lukr99.workout.ui.android

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.lukr99.workout.data.health.HealthConnectAvailability
import com.lukr99.workout.ui.UpdatesViewModel
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.SegmentedControl
import com.lukr99.workout.ui.settings.ButtonRow
import com.lukr99.workout.ui.settings.LinkRow
import com.lukr99.workout.ui.settings.SettingsHost
import com.lukr99.workout.ui.settings.SettingsRow
import com.lukr99.workout.ui.settings.SettingsSlot
import com.lukr99.workout.ui.settings.ToggleRow
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * Android's [SettingsHost]: the wger catalog, Health Connect, automatic backup and the updater.
 * The launchers are made here, at the top of the Settings screen, so a result still finds its
 * handler after the row that started it has scrolled out of the list.
 */
@Composable
fun rememberAndroidSettingsHost(vm: AndroidSettingsViewModel, updates: UpdatesViewModel): SettingsHost {
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted -> vm.onHealthPermissionsResult(granted) }
    val backupFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(vm::enableBackup) }
    val updateState by updates.state.collectAsState()
    return AndroidSettingsHost(vm, updates, healthPermissionLauncher, backupFolderLauncher, updateState.currentVersion)
}

private class AndroidSettingsHost(
    private val vm: AndroidSettingsViewModel,
    private val updates: UpdatesViewModel,
    private val healthPermissionLauncher: ActivityResultLauncher<Set<String>>,
    private val backupFolderLauncher: ActivityResultLauncher<android.net.Uri?>,
    override val appVersion: String,
) : SettingsHost {

    override fun has(slot: SettingsSlot): Boolean = true

    @Composable
    override fun Rows(slot: SettingsSlot, onOpenPrivacy: () -> Unit) {
        when (slot) {
            SettingsSlot.Catalog -> CatalogRow()
            SettingsSlot.HealthConnect -> HealthConnectRows(onOpenPrivacy)
            SettingsSlot.Backup -> BackupRows()
            SettingsSlot.Updates -> UpdatesRows()
        }
    }

    @Composable
    override fun Overlays() {
        val health by vm.healthConnectUi.collectAsState()
        val updateState by updates.state.collectAsState()
        val toast = LocalToast.current
        LaunchedEffect(health.summary) {
            health.summary?.let { summary ->
                toast("Health Connect: imported ${summary.imported}, exported ${summary.exported}, skipped ${summary.skipped}")
                vm.consumeHealthSummary()
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

    @Composable
    private fun CatalogRow() {
        val syncState by vm.catalogSync.collectAsState()
        val running = syncState is AndroidSettingsViewModel.CatalogSyncState.Running
        ButtonRow(
            label = "Exercise catalog",
            hint = when (val state = syncState) {
                is AndroidSettingsViewModel.CatalogSyncState.Done ->
                    "Added ${state.summary.added}, updated ${state.summary.updated}, skipped ${state.summary.skipped}."
                is AndroidSettingsViewModel.CatalogSyncState.Failed -> state.message
                else -> "Download the open wger exercise database. Your own exercises are never changed."
            },
            hintIsError = syncState is AndroidSettingsViewModel.CatalogSyncState.Failed,
            action = "Sync now",
            busy = running,
            busyLabel = "Syncing…",
            onClick = vm::syncCatalog,
        )
    }

    @Composable
    private fun HealthConnectRows(onOpenPrivacy: () -> Unit) {
        val health by vm.healthConnectUi.collectAsState()
        val healthAutoSend by vm.healthAutoSend.collectAsState()
        val context = LocalContext.current
        val status = health.error ?: healthAvailabilityDetail(health.availability, health.connected)
        when (health.availability) {
            HealthConnectAvailability.Available -> if (!health.connected) {
                ButtonRow("Not connected", status, "Connect", first = true, busy = health.refreshing) {
                    healthPermissionLauncher.launch(vm.healthConnectPermissions)
                }
            } else {
                SettingsRow("Connected", status, first = true, hintIsError = health.error != null)
                ToggleRow(
                    "Send finished workouts and runs",
                    "Each one goes to Health Connect when you finish it.",
                    checked = healthAutoSend,
                    onChange = vm::setHealthAutoSend,
                )
                ButtonRow("Send everything now", "Workouts and runs Health Connect does not have yet.", "Export", busy = health.operation != null, onClick = vm::exportToHealthConnect)
                ButtonRow("Bring workouts in", "Imports workouts from other apps as sessions.", "Import", busy = health.operation != null, onClick = vm::importFromHealthConnect)
            }
            HealthConnectAvailability.ProviderUpdateRequired ->
                ButtonRow("Update needed", status, "Install", first = true) { openHealthConnectListing(context) }
            else -> SettingsRow(healthAvailabilityLabel(health.availability, health.connected), status, first = true)
        }
        LinkRow("Privacy policy", "What Ember reads and why.", onClick = onOpenPrivacy)
    }

    @Composable
    private fun BackupRows() {
        val backup by vm.backupState.collectAsState()
        val backupOptions by vm.backupOptions.collectAsState()
        val backupBusy by vm.backupBusy.collectAsState()
        val backupError by vm.backupError.collectAsState()
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
    }

    @Composable
    private fun UpdatesRows() {
        val updateState by updates.state.collectAsState()
        val autoCheck by updates.autoCheck.collectAsState()
        val uriHandler = LocalUriHandler.current
        ButtonRow(
            label = "Version ${updateState.currentVersion}",
            hint = updateState.status.ifBlank { "Checks GitHub for a newer, signed release." },
            action = "Check now",
            first = true,
            busy = updateState.busy,
            busyLabel = "Checking…",
            onClick = updates::check,
        )
        ToggleRow(
            "Check automatically",
            "Looks once a day when you open Ember, and says so on Home when an update is ready.",
            checked = autoCheck,
            onChange = updates::setAutoCheck,
        )
        LinkRow("All releases on GitHub", "Release notes and the manual download.", outside = true) {
            runCatching { uriHandler.openUri(UpdatesViewModel.RELEASES_URL) }
        }
    }
}
