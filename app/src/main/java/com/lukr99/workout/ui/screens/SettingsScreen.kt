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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lukr99.workout.settings.ThemeMode
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.SettingsViewModel
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.FilterChip
import com.lukr99.workout.ui.components.ScreenHeader
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.TextMid

/** Preferences — theme, units, default rest — plus the route to the Phase 3 Data screen. */
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onOpenData: () -> Unit,
) {
    val settings by vm.settings.collectAsState()

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
                FilterChip("Kilograms", settings.units == UnitSystem.Metric, { vm.setUnits(UnitSystem.Metric) })
                FilterChip("Pounds", settings.units == UnitSystem.Imperial, { vm.setUnits(UnitSystem.Imperial) })
            }
        }

        SettingSection("Rest timer") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Default rest", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                Text(
                    Format.clock(settings.defaultRestSeconds),
                    style = Numbers, color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120, 150, 180).forEach { secs ->
                    FilterChip(Format.clock(secs), settings.defaultRestSeconds == secs, { vm.setDefaultRest(secs) })
                }
            }
        }

        val syncState by vm.catalogSync.collectAsState()
        SettingSection("Exercise catalog") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Sync from wger", color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        when (val s = syncState) {
                            is SettingsViewModel.CatalogSyncState.Running -> "Syncing…"
                            is SettingsViewModel.CatalogSyncState.Done ->
                                "Added ${s.summary.added} · updated ${s.summary.updated} · skipped ${s.summary.skipped}"
                            is SettingsViewModel.CatalogSyncState.Failed -> s.message
                            else -> "Download the open exercise database"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (syncState is SettingsViewModel.CatalogSyncState.Failed) MaterialTheme.colorScheme.error else TextMid,
                    )
                }
                if (syncState is SettingsViewModel.CatalogSyncState.Running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        "Sync",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .clickable { vm.syncCatalog() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }

        // Data route to the Phase 3 screen (import/export/backup) — not reimplemented here.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface).clickable(onClick = onOpenData).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Data", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("Import, export & backup", style = MaterialTheme.typography.labelSmall, color = TextMid)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = TextMid)
        }

        Text(
            "Workout Tracker · rework build",
            style = MaterialTheme.typography.labelSmall, color = TextMid,
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
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        content()
    }
}
