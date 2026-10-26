package com.lukr99.workout.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.android.AndroidSettingsViewModel
import com.lukr99.workout.ui.android.rememberAndroidSettingsHost
import com.lukr99.workout.ui.components.LocalSpotify
import com.lukr99.workout.ui.components.UpdateCard
import com.lukr99.workout.ui.run.LiveRunScreen
import com.lukr99.workout.ui.run.LiveRunViewModel
import com.lukr99.workout.ui.run.RoutePlannerScreen
import com.lukr99.workout.ui.run.RoutePlannerViewModel
import com.lukr99.workout.ui.run.RunDetailScreen
import com.lukr99.workout.ui.run.RunningProgressSection
import com.lukr99.workout.ui.run.RunViewModel
import com.lukr99.workout.ui.run.RunsScreen
import com.lukr99.workout.ui.settings.SettingsHost
import kotlinx.coroutines.flow.first

/**
 * Everything [App] needs that only some platforms have: Run Mode, the in-app updater, Health
 * Connect, automatic backup and the Spotify controller. Android wires the real ones. The web
 * edition has none of them, so its version shows "not available" and returns nothing.
 *
 * Each platform supplies this file; the web edition's is under web/.
 */
class PlatformApp internal constructor(
    private val container: AppContainer,
    private val updatesVm: UpdatesViewModel,
    private val settingsVm: AndroidSettingsViewModel,
    private val routePlannerVm: RoutePlannerViewModel,
    private val liveRunVm: LiveRunViewModel,
    /** The runs view model, which the Progress tab also reads. */
    val runViewModel: RunViewModel,
    /** True while a run is being recorded. */
    val liveRunActive: Boolean,
    /** Home's "update available" card, or null when there is no offer. */
    val updateCard: (@Composable () -> Unit)?,
) {
    /** Composition locals the platform provides to the whole app (the music controller). */
    val locals: Array<ProvidedValue<*>> = arrayOf(LocalSpotify provides container.spotify)

    /** Gets the live-run screen ready, for a saved route when [routeId] is not null. */
    /** The Running pane on the Progress tab; the web edition has none. */
    val runningSection: (@Composable (UnitSystem) -> Unit)? = { units -> RunningProgressSection(vm = runViewModel, units = units) }

    fun prepareRun(routeId: String?) = liveRunVm.prepareRoute(routeId)

    /** Called after a workout is saved; sends it to Health Connect when the person turned that on. */
    suspend fun onWorkoutSaved(session: WorkoutSession) {
        if (container.devicePrefs.healthAutoSend.first()) container.healthConnect.exportWorkout(session)
    }

    /** The Runs tab. */
    @Composable
    fun RunsTab(
        units: UnitSystem,
        onStartRun: () -> Unit,
        onOpenRun: (String) -> Unit,
        onPlanRoute: () -> Unit,
        onStartRoute: (String) -> Unit,
    ) {
        RunsScreen(
            vm = runViewModel,
            units = units,
            onStartRun = onStartRun,
            onOpenRun = onOpenRun,
            onPlanRoute = onPlanRoute,
            onStartRoute = onStartRoute,
        )
    }

    /** The full-screen Run Mode flows ([Route.LiveRun], [Route.RunDetail], [Route.RoutePlanner]). */
    @Composable
    fun RunRoute(route: Route, units: UnitSystem, onClose: () -> Unit) {
        when (route) {
            Route.LiveRun -> LiveRunScreen(vm = liveRunVm, units = units, onClose = onClose)
            is Route.RunDetail -> RunDetailScreen(vm = runViewModel, runId = route.runId, units = units, onBack = onClose)
            Route.RoutePlanner -> RoutePlannerScreen(vm = routePlannerVm, units = units, onBack = onClose)
            else -> Unit
        }
    }

    /** The platform's part of the Settings screen. */
    @Composable
    fun rememberSettingsHost(): SettingsHost = rememberAndroidSettingsHost(settingsVm, updatesVm)
}

/** Builds the [PlatformApp] and starts the launch update check. */
@Composable
fun rememberPlatformApp(container: AppContainer): PlatformApp {
    val runVm: RunViewModel = viewModel(factory = RunViewModel.factory(container))
    val liveRunVm: LiveRunViewModel = viewModel(factory = LiveRunViewModel.factory(container))
    val routePlannerVm: RoutePlannerViewModel = viewModel(factory = RoutePlannerViewModel.factory(container))
    val settingsVm: AndroidSettingsViewModel = viewModel(
        factory = AndroidSettingsViewModel.factory(
            container.wgerSync,
            container.healthConnect,
            container.backup,
            container.devicePrefs,
            container.runRepository,
        ),
    )
    val updatesVm: UpdatesViewModel = viewModel(factory = UpdatesViewModel.factory(container.updates, container.devicePrefs))
    val updateState by updatesVm.state.collectAsState()
    LaunchedEffect(Unit) { updatesVm.checkIfDue() }
    val liveRunState by liveRunVm.state.collectAsState()

    val updateCard: (@Composable () -> Unit)? = updateState.offer?.let { offer ->
        {
            UpdateCard(
                version = offer.version,
                busy = updateState.busy,
                status = updateState.status,
                onUpdate = updatesVm::downloadAndInstall,
                onLater = updatesVm::dismissOffer,
            )
        }
    }
    return PlatformApp(container, updatesVm, settingsVm, routePlannerVm, liveRunVm, runVm, liveRunState.isActive, updateCard)
}
