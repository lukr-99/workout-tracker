package com.lukr99.workout.ui.android

import com.lukr99.workout.ui.i18n.LocalLanguage
import com.lukr99.workout.ui.i18n.DeviceSettingsText
import androidx.activity.compose.rememberLauncherForActivityResult
import com.lukr99.workout.ui.about.AboutEdition
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
 * Android's [SettingsHost]: the wger catalog, Health Connect, automatic backup, the updater and the
 * Claude connector.
 * The launchers are made here, at the top of the Settings screen, so a result still finds its
 * handler after the row that started it has scrolled out of the list.
 */
@Composable
fun rememberAndroidSettingsHost(vm: AndroidSettingsViewModel, updates: UpdatesViewModel, claude: ClaudeSettingsViewModel): SettingsHost {
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted -> vm.onHealthPermissionsResult(granted) }
    val backupFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(vm::enableBackup) }
    // Claude's requests show as notifications; the push token itself needs no permission.
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { claude.registerPush() }
    val updateState by updates.state.collectAsState()
    return AndroidSettingsHost(vm, updates, claude, healthPermissionLauncher, backupFolderLauncher, notificationLauncher, updateState.currentVersion)
}

private class AndroidSettingsHost(
    private val vm: AndroidSettingsViewModel,
    private val updates: UpdatesViewModel,
    private val claude: ClaudeSettingsViewModel,
    private val healthPermissionLauncher: ActivityResultLauncher<Set<String>>,
    private val backupFolderLauncher: ActivityResultLauncher<android.net.Uri?>,
    private val notificationLauncher: ActivityResultLauncher<String>,
    override val appVersion: String,
) : SettingsHost {
    override val edition: AboutEdition = AboutEdition.Android

    override fun has(slot: SettingsSlot): Boolean = true

    @Composable
    override fun Rows(slot: SettingsSlot, onOpenPrivacy: () -> Unit) {
        when (slot) {
            SettingsSlot.Catalog -> CatalogRow()
            SettingsSlot.HealthConnect -> HealthConnectRows(onOpenPrivacy)
            SettingsSlot.Backup -> BackupRows()
            SettingsSlot.Updates -> UpdatesRows()
            SettingsSlot.Claude -> ClaudeSettingsRows(claude)
        }
    }

    @Composable
    override fun Overlays() {
        val health by vm.healthConnectUi.collectAsState()
        val updateState by updates.state.collectAsState()
        val toast = LocalToast.current
        val lang = LocalLanguage.current
        ClaudeSettingsDialogs(claude) { serverUrl, setupSecret ->
            claude.connect(serverUrl, setupSecret, android.os.Build.MODEL)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        LaunchedEffect(health.summary) {
            health.summary?.let { summary ->
                toast(
                    DeviceSettingsText.healthSummary.get(
                        lang,
                        "imported" to summary.imported,
                        "exported" to summary.exported,
                        "skipped" to summary.skipped,
                    ),
                )
                vm.consumeHealthSummary()
            }
        }
        updateState.offer?.let { offer ->
            AlertDialog(
                onDismissRequest = updates::dismissOffer,
                confirmButton = { TextButton(enabled = !updateState.busy, onClick = updates::downloadAndInstall) { Text(DeviceSettingsText.updateDownload.text()) } },
                dismissButton = { TextButton(enabled = !updateState.busy, onClick = updates::dismissOffer) { Text(DeviceSettingsText.updateLater.text()) } },
                title = { Text(DeviceSettingsText.updateOfferTitle.text("version" to offer.version)) },
                text = { Text(offer.notes.ifBlank { DeviceSettingsText.updateNewVersion.get(lang) }.take(600), color = EmberTheme.colors.textSecondary) },
            )
        }
    }

    @Composable
    private fun CatalogRow() {
        val syncState by vm.catalogSync.collectAsState()
        val running = syncState is AndroidSettingsViewModel.CatalogSyncState.Running
        ButtonRow(
            label = DeviceSettingsText.catalogTitle.text(),
            hint = when (val state = syncState) {
                is AndroidSettingsViewModel.CatalogSyncState.Done ->
                    DeviceSettingsText.catalogDone.text("added" to state.summary.added, "updated" to state.summary.updated, "skipped" to state.summary.skipped)
                is AndroidSettingsViewModel.CatalogSyncState.Failed -> state.message
                else -> DeviceSettingsText.catalogHint.text()
            },
            hintIsError = syncState is AndroidSettingsViewModel.CatalogSyncState.Failed,
            action = DeviceSettingsText.catalogSync.text(),
            busy = running,
            busyLabel = DeviceSettingsText.catalogSyncing.text(),
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
                ButtonRow(DeviceSettingsText.healthNotConnected.text(), status, DeviceSettingsText.healthConnect.text(), first = true, busy = health.refreshing) {
                    healthPermissionLauncher.launch(vm.healthConnectPermissions)
                }
            } else {
                SettingsRow(DeviceSettingsText.healthConnected.text(), status, first = true, hintIsError = health.error != null)
                ToggleRow(
                    DeviceSettingsText.healthSendTitle.text(),
                    DeviceSettingsText.healthSendHint.text(),
                    checked = healthAutoSend,
                    onChange = vm::setHealthAutoSend,
                )
                ButtonRow(DeviceSettingsText.healthSendAllTitle.text(), DeviceSettingsText.healthSendAllHint.text(), DeviceSettingsText.healthExport.text(), busy = health.operation != null, onClick = vm::exportToHealthConnect)
                ButtonRow(DeviceSettingsText.healthBringInTitle.text(), DeviceSettingsText.healthBringInHint.text(), DeviceSettingsText.healthImport.text(), busy = health.operation != null, onClick = vm::importFromHealthConnect)
            }
            HealthConnectAvailability.ProviderUpdateRequired ->
                ButtonRow(DeviceSettingsText.healthUpdateNeeded.text(), status, DeviceSettingsText.healthInstall.text(), first = true) { openHealthConnectListing(context) }
            else -> SettingsRow(healthAvailabilityLabel(health.availability, health.connected), status, first = true)
        }
        LinkRow(DeviceSettingsText.privacyTitle.text(), DeviceSettingsText.privacyHint.text(), onClick = onOpenPrivacy)
    }

    @Composable
    private fun BackupRows() {
        val backup by vm.backupState.collectAsState()
        val backupOptions by vm.backupOptions.collectAsState()
        val backupBusy by vm.backupBusy.collectAsState()
        val backupError by vm.backupError.collectAsState()
        val lang = LocalLanguage.current
        ToggleRow(
            label = DeviceSettingsText.backupTitle.text(),
            hint = if (backup.enabled) {
                "${backupFolderLabel(backup.treeUri)}. ${backupStatus(backup.lastRunUtcMillis, backup.lastResult, backup.lastMessage, backupError)}"
            } else {
                DeviceSettingsText.backupHint.text()
            },
            checked = backup.enabled,
            first = true,
            busy = backupBusy,
        ) { enabled -> if (enabled) backupFolderLauncher.launch(null) else vm.disableBackup() }
        SettingsRow(DeviceSettingsText.backupHowOften.text(), null)
        val interval = if (backup.enabled) backup.intervalHours else backupOptions.intervalHours
        SegmentedControl(listOf(24L, 168L), interval, { vm.setBackupInterval(it) }, label = { if (it == 24L) DeviceSettingsText.backupDaily.get(lang) else DeviceSettingsText.backupWeekly.get(lang) }, modifier = Modifier.padding(bottom = 8.dp))
        SettingsRow(DeviceSettingsText.backupKeepTitle.text(), DeviceSettingsText.backupKeepHint.text())
        val kept = if (backup.enabled) backup.retentionCount else backupOptions.retentionCount
        SegmentedControl(listOf(3, 7, 14), kept, { vm.setBackupRetention(it) }, label = { it.toString() }, modifier = Modifier.padding(bottom = 8.dp))
    }

    @Composable
    private fun UpdatesRows() {
        val updateState by updates.state.collectAsState()
        val autoCheck by updates.autoCheck.collectAsState()
        val uriHandler = LocalUriHandler.current
        ButtonRow(
            label = DeviceSettingsText.updateVersion.text("version" to updateState.currentVersion),
            hint = updateState.status.ifBlank { DeviceSettingsText.updateHint.text() },
            action = DeviceSettingsText.updateCheckNow.text(),
            first = true,
            busy = updateState.busy,
            busyLabel = DeviceSettingsText.updateChecking.text(),
            onClick = updates::check,
        )
        ToggleRow(
            DeviceSettingsText.updateAutoTitle.text(),
            DeviceSettingsText.updateAutoHint.text(),
            checked = autoCheck,
            onChange = updates::setAutoCheck,
        )
        LinkRow(DeviceSettingsText.updateReleasesTitle.text(), DeviceSettingsText.updateReleasesHint.text(), outside = true) {
            runCatching { uriHandler.openUri(UpdatesViewModel.RELEASES_URL) }
        }
    }
}
