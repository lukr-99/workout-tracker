package com.lukr99.workout.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.lukr99.workout.data.backup.BackupResult
import com.lukr99.workout.data.health.HealthConnectAvailability
import com.lukr99.workout.settings.ThemeMode
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.SettingsViewModel
import com.lukr99.workout.ui.components.FilterChip
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.ScreenHeader
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.TextMid
import java.text.DateFormat
import java.util.Date

/** Preferences plus Health Connect and automatic backup release integrations. */
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onOpenData: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val settings by vm.settings.collectAsState()
    val health by vm.healthConnectUi.collectAsState()
    val backup by vm.backupState.collectAsState()
    val backupOptions by vm.backupOptions.collectAsState()
    val backupBusy by vm.backupBusy.collectAsState()
    val backupError by vm.backupError.collectAsState()
    val toast = LocalToast.current
    val context = LocalContext.current

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted -> vm.onHealthPermissionsResult(granted) }
    val backupFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(vm::enableBackup) }

    LaunchedEffect(health.summary) {
        health.summary?.let { summary ->
            toast(
                "Health Connect · imported ${summary.imported} · exported ${summary.exported} · " +
                    "skipped ${summary.skipped} · unsupported ${summary.unsupported}",
            )
            vm.consumeHealthSummary()
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Settings", "Preferences & data")

        SettingSection("Appearance") {
            Text("Theme", color = TextMid, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(mode.name, settings.themeMode == mode, { vm.setTheme(mode) })
                }
            }
        }

        SettingSection("Units") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    "Kilograms",
                    settings.units == UnitSystem.Metric,
                    { vm.setUnits(UnitSystem.Metric) },
                )
                FilterChip(
                    "Pounds",
                    settings.units == UnitSystem.Imperial,
                    { vm.setUnits(UnitSystem.Imperial) },
                )
            }
        }

        SettingSection("Rest timer") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Default rest",
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    Format.clock(settings.defaultRestSeconds),
                    style = Numbers,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120, 150, 180).forEach { seconds ->
                    FilterChip(
                        Format.clock(seconds),
                        settings.defaultRestSeconds == seconds,
                        { vm.setDefaultRest(seconds) },
                    )
                }
            }
        }

        SettingSection("Health Connect") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        healthAvailabilityLabel(health.availability, health.connected),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        health.error ?: healthAvailabilityDetail(health.availability, health.connected),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (health.error != null) MaterialTheme.colorScheme.error else TextMid,
                    )
                }
                if (health.refreshing || health.operation != null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            when (health.availability) {
                HealthConnectAvailability.Available -> {
                    if (!health.connected) {
                        SettingsAction("Connect") {
                            healthPermissionLauncher.launch(vm.healthConnectPermissions)
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                "Import",
                                false,
                                vm::importFromHealthConnect,
                                modifier = Modifier.weight(1f),
                            )
                            FilterChip(
                                "Export",
                                false,
                                vm::exportToHealthConnect,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                HealthConnectAvailability.ProviderUpdateRequired -> {
                    SettingsAction("Install / update") {
                        openHealthConnectListing(context)
                    }
                }
                else -> Unit
            }

            SettingsLink(
                title = "Privacy policy",
                subtitle = "How workout and health data are used",
                onClick = onOpenPrivacy,
            )
        }

        SettingSection("Backup") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Automatic backup", color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        if (backup.enabled) backupFolderLabel(backup.treeUri) else "Choose a device or cloud folder",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMid,
                    )
                }
                if (backupBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Switch(
                        checked = backup.enabled,
                        onCheckedChange = { enabled ->
                            if (enabled) backupFolderLauncher.launch(null) else vm.disableBackup()
                        },
                    )
                }
            }

            Text("Frequency", color = TextMid, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val selected = if (backup.enabled) backup.intervalHours else backupOptions.intervalHours
                FilterChip("Daily", selected == 24L, { vm.setBackupInterval(24) })
                FilterChip("Weekly", selected == 168L, { vm.setBackupInterval(168) })
            }

            Text("Backups kept", color = TextMid, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val selected = if (backup.enabled) backup.retentionCount else backupOptions.retentionCount
                listOf(3, 7, 14).forEach { count ->
                    FilterChip(count.toString(), selected == count, { vm.setBackupRetention(count) })
                }
            }

            Text(
                backupStatus(backup.lastRunUtcMillis, backup.lastResult, backup.lastMessage, backupError),
                style = MaterialTheme.typography.labelSmall,
                color = if (backup.lastResult == BackupResult.Failed || backupError != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    TextMid
                },
            )
        }

        val syncState by vm.catalogSync.collectAsState()
        SettingSection("Exercise catalog") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Sync from wger", color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        when (val state = syncState) {
                            is SettingsViewModel.CatalogSyncState.Running -> "Syncing…"
                            is SettingsViewModel.CatalogSyncState.Done ->
                                "Added ${state.summary.added} · updated ${state.summary.updated} · " +
                                    "images ${state.summary.imagesBackfilled} · skipped ${state.summary.skipped}"
                            is SettingsViewModel.CatalogSyncState.Failed -> state.message
                            else -> "Download the open exercise database"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (syncState is SettingsViewModel.CatalogSyncState.Failed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            TextMid
                        },
                    )
                }
                if (syncState is SettingsViewModel.CatalogSyncState.Running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    SettingsAction("Sync", vm::syncCatalog)
                }
            }
        }

        SettingsLink(
            title = "Data",
            subtitle = "Manual import & export",
            onClick = onOpenData,
        )

        Text(
            "Ember · 2.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = TextMid,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        content()
    }
}

@Composable
private fun SettingsAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsLink(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 2.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextMid)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = TextMid)
    }
}

private fun healthAvailabilityLabel(
    availability: HealthConnectAvailability?,
    connected: Boolean,
): String = when (availability) {
    HealthConnectAvailability.Available -> if (connected) "Connected" else "Available"
    HealthConnectAvailability.ProviderUpdateRequired -> "Provider update required"
    HealthConnectAvailability.Unavailable -> "Unavailable on this device"
    null -> "Checking availability…"
}

private fun healthAvailabilityDetail(
    availability: HealthConnectAvailability?,
    connected: Boolean,
): String = when (availability) {
    HealthConnectAvailability.Available ->
        if (connected) "Exercise and weight permissions granted" else "Connect to import or export workouts"
    HealthConnectAvailability.ProviderUpdateRequired -> "Install or update Health Connect to continue"
    HealthConnectAvailability.Unavailable -> "Health Connect is not supported by this device"
    null -> "Reading provider status"
}

private fun openHealthConnectListing(context: android.content.Context) {
    val packageName = "com.google.android.apps.healthdata"
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
    val browser = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
    )
    runCatching { context.startActivity(market) }
        .recoverCatching { context.startActivity(browser) }
}

private fun backupFolderLabel(treeUri: String?): String {
    if (treeUri.isNullOrBlank()) return "Backup folder selected"
    return Uri.decode(treeUri.substringAfterLast('/')).ifBlank { "Backup folder selected" }
}

private fun backupStatus(
    lastRunUtcMillis: Long?,
    result: BackupResult,
    message: String?,
    uiError: String?,
): String {
    uiError?.let { return it }
    val whenRun = lastRunUtcMillis?.let {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
    } ?: return "No automatic backup has run yet"
    val resultLabel = when (result) {
        BackupResult.Success -> "Succeeded"
        BackupResult.Failed -> "Failed"
        BackupResult.Disabled -> "Disabled"
        BackupResult.NeverRun -> "Not run"
    }
    return listOfNotNull("$resultLabel · $whenRun", message?.takeIf(String::isNotBlank))
        .joinToString(" · ")
}
