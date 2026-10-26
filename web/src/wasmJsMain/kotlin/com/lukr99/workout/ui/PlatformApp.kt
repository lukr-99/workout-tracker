package com.lukr99.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.settings.SettingsHost
import com.lukr99.workout.ui.settings.WebSettingsHost

/**
 * Everything [App] needs that only some platforms have: Run Mode, the in-app updater, Health
 * Connect, automatic backup and the Spotify controller. The web edition has none of them, so this
 * version offers nothing and its Runs tab says so.
 *
 * Each platform supplies this file.
 */
class PlatformApp internal constructor() {
    /** The Running pane on the Progress tab; the web edition has none. */
    val runningSection: (@Composable (UnitSystem) -> Unit)? = null

    /** True while a run is being recorded; never, here. */
    val liveRunActive: Boolean = false

    /** Home's "update available" card; the page updates through its service worker instead. */
    val updateCard: (@Composable () -> Unit)? = null

    /** Composition locals the platform provides to the whole app; none here. */
    val locals: Array<ProvidedValue<*>> = arrayOf()

    /** Gets the live-run screen ready; there is none here. */
    @Suppress("UNUSED_PARAMETER")
    fun prepareRun(routeId: String?) = Unit

    /** Called after a workout is saved; there is nowhere to send it here. */
    @Suppress("UNUSED_PARAMETER")
    suspend fun onWorkoutSaved(session: WorkoutSession) = Unit

    /** The Runs tab: a note that Run Mode is not in the web edition. */
    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun RunsTab(
        units: UnitSystem,
        onStartRun: () -> Unit,
        onOpenRun: (String) -> Unit,
        onPlanRoute: () -> Unit,
        onStartRoute: (String) -> Unit,
    ) {
        NoRunsNote(onBack = null)
    }

    /** The full-screen Run Mode flows; here only the same note, with a way back. */
    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun RunRoute(route: Route, units: UnitSystem, onClose: () -> Unit) {
        NoRunsNote(onBack = onClose)
    }

    /** The platform's part of the Settings screen: nothing, so Settings shows only the shared rows. */
    @Composable
    fun rememberSettingsHost(): SettingsHost = remember { WebSettingsHost() }
}

/** Builds the [PlatformApp]. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun rememberPlatformApp(container: AppContainer): PlatformApp = remember { PlatformApp() }

@Composable
private fun NoRunsNote(onBack: (() -> Unit)?) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Runs are not in the web edition yet", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            "Use the Android app to record runs. Runs saved in a backup are kept when you import it here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onBack != null) TextButton(onClick = onBack) { Text("Back") }
    }
}
