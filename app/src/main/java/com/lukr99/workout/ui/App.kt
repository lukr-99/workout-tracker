package com.lukr99.workout.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.ui.components.LocalExerciseImageResolver
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.ResumeChooserSheet
import com.lukr99.workout.ui.components.StartChooserSheet
import com.lukr99.workout.ui.components.ToastHost
import com.lukr99.workout.ui.components.exposeTestTags
import com.lukr99.workout.ui.components.LocationSetupCard
import com.lukr99.workout.ui.components.rememberReduceMotion
import com.lukr99.workout.ui.components.rememberToastState
import com.lukr99.workout.ui.screens.DataTransferScreen
import com.lukr99.workout.ui.screens.ExerciseEditorScreen
import com.lukr99.workout.ui.screens.HomeScreen
import com.lukr99.workout.ui.screens.LibraryScreen
import com.lukr99.workout.ui.screens.LiveWorkoutScreen
import com.lukr99.workout.ui.screens.PrivacyPolicyScreen
import com.lukr99.workout.ui.screens.ProgressDetailScreen
import com.lukr99.workout.ui.screens.ProgressHubScreen
import com.lukr99.workout.ui.screens.TemplateEditorScreen
import com.lukr99.workout.ui.screens.TemplatePreviewSheet
import com.lukr99.workout.ui.screens.WorkoutDetailScreen
import com.lukr99.workout.ui.settings.SettingsScreen

/**
 * App root. The 5-item shell — `Home · Runs · (＋ Start) · Progress · Settings` — with the center
 * ember Start action opening a Lift/Run chooser (or resuming a live lift), not a peer tab. History
 * lives under the Progress tab now. Peer tabs cross-fade; full-screen flows layer over them through a
 * manual back-stack ([Navigator]). See 02-design-system.md and docs/run-mode.
 */
@Composable
fun App(container: AppContainer) {
    val nav = remember { Navigator() }
    val toast = rememberToastState()

    val homeVm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container.repository, container.runRepository, container.whatsNew))
    val historyVm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container.repository))
    val progressVm: ProgressViewModel = viewModel(factory = ProgressViewModel.factory(container.repository, container.workoutData, container.insights))
    val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container.settings))
    val libraryVm: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(container.repository, container.workoutData, container.exercisePhotos))
    val liveVm: LiveWorkoutViewModel = viewModel(
        factory = LiveWorkoutViewModel.factory(
            container.repository,
            container.settings,
            container.insights,
            container.locationSuggester,
            container.devicePrefs,
        ),
    )
    val locationsVm: LocationsViewModel = viewModel(factory = LocationsViewModel.factory(container.repository, container.devicePrefs, container.locationSuggester))
    val locationSetup by locationsVm.setup.collectAsState()
    val locations by locationsVm.locations.collectAsState()
    val platform = rememberPlatformApp(container)
    val dataVm: DataTransferViewModel = viewModel(
        factory = DataTransferViewModel.factory(container.dataTransfer, container.dataEraser),
    )

    val settings by settingsVm.settings.collectAsState()
    val activeSession by homeVm.activeSession.collectAsState()
    val runActive = platform.liveRunActive
    // An in-progress lift only counts as "resume-able" once it has content; an empty 0-exercise
    // session (e.g. one started then abandoned) shouldn't keep the centre action stuck on Resume.
    val hasResumableLift = activeSession?.entries?.isNotEmpty() == true
    val resumeMode = runActive || hasResumableLift
    val overlay = nav.top
    val reduceMotion = rememberReduceMotion()
    var chooserOpen by remember { mutableStateOf(false) }
    var resumeChooserOpen by remember { mutableStateOf(false) }
    var previewTemplateId by remember { mutableStateOf<String?>(null) }
    val templates by homeVm.templates.collectAsState()

    fun startWorkout(templateId: String? = null) {
        liveVm.startOrResume(templateId)
        nav.push(Route.LiveWorkout)
    }

    fun startRun() {
        platform.prepareRun(null)
        nav.push(Route.LiveRun)
    }

    fun startRunFromRoute(routeId: String) {
        platform.prepareRun(routeId)
        nav.push(Route.LiveRun)
    }

    // The center action: when a run **and** a lift are both live it can't route to both, so it asks
    // which to reopen; otherwise the live one resumes; otherwise the ＋ opens the Lift/Run chooser.
    fun onCenterAction() {
        when {
            runActive && hasResumableLift -> resumeChooserOpen = true
            runActive -> nav.push(Route.LiveRun)
            hasResumableLift -> startWorkout()
            // Without Run Mode (the web edition) there is nothing to choose: Start is a workout.
            platform.hasRuns -> chooserOpen = true
            else -> startWorkout()
        }
    }

    CompositionLocalProvider(
        LocalToast provides toast,
        LocalExerciseImageResolver provides container.exerciseImages,
        *platform.locals,
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                // Test tags become resource ids for uiautomator, Android CLI and Maestro.
                .exposeTestTags()
                .testTag(AppTags.ROOT),
        ) {
            // Peer-tab layer (always mounted so tab state persists behind overlays).
            Column(
                Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 18.dp)
                    .padding(top = 12.dp),
            ) {
                Crossfade(targetState = nav.tab.value, animationSpec = tween(if (reduceMotion) 0 else 220), label = "tab") { tab ->
                    when (tab) {
                        Tab.HOME -> HomeScreen(
                            vm = homeVm,
                            units = settings.units,
                            onResume = { startWorkout() },
                            onStartTemplate = { startWorkout(it) },
                            onOpenTemplate = { previewTemplateId = it },
                            onOpenTemplates = { nav.switch(Tab.LIBRARY) },
                            onOpenSession = { nav.push(Route.WorkoutDetail(it)) },
                            onOpenRun = { nav.push(Route.RunDetail(it)) },
                            onOpenSettings = { nav.push(Route.Settings) },
                            updateCard = platform.updateCard,
                            locationSetupCard = locationSetup?.let { setup ->
                                {
                                    val toast = LocalToast.current
                                    LocationSetupCard(
                                        unknownWorkouts = setup.unknownWorkouts,
                                        onSave = { name, assignPast ->
                                            locationsVm.finishSetup(name, assignPast) { moved ->
                                                toast(if (moved > 0) "$name added, $moved workouts moved there" else "$name added")
                                            }
                                        },
                                        onDismiss = locationsVm::dismissSetup,
                                        modifier = Modifier.testTag(AppTags.LOCATION_SETUP),
                                    )
                                }
                            },
                        )
                        Tab.LIBRARY -> LibraryScreen(
                            vm = libraryVm,
                            units = settings.units,
                            onEditTemplate = { nav.push(Route.TemplateEditor(it)) },
                            onNewTemplate = { nav.push(Route.TemplateEditor(null)) },
                            onEditExercise = { nav.push(Route.ExerciseEditor(it)) },
                            onNewExercise = { nav.push(Route.ExerciseEditor(null, initialName = it)) },
                            onStartTemplate = { startWorkout(it) },
                            onOpenTemplate = { previewTemplateId = it },
                        )
                        Tab.RUNS -> platform.RunsTab(
                            units = settings.units,
                            onStartRun = { startRun() },
                            onOpenRun = { nav.push(Route.RunDetail(it)) },
                            onPlanRoute = { nav.push(Route.RoutePlanner) },
                            onStartRoute = { startRunFromRoute(it) },
                        )
                        Tab.PROGRESS -> ProgressHubScreen(
                            progressVm = progressVm,
                            historyVm = historyVm,
                            locationsVm = locationsVm,
                            units = settings.units,
                            onOpenExercise = { nav.push(Route.ProgressDetail(it)) },
                            onOpenSession = { nav.push(Route.WorkoutDetail(it)) },
                            runningSection = platform.runningSection?.let { section -> { section(settings.units) } },
                        )
                    }
                }
            }

            // Overlay layer (full-screen flows).
            if (overlay != null) {
                PlatformBackHandler(enabled = true) { nav.pop() }
                Box(
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.systemBars),
                ) {
                    when (overlay) {
                        Route.LiveWorkout -> LiveWorkoutScreen(
                            vm = liveVm,
                            locationsVm = locationsVm,
                            units = settings.units,
                            onClose = { liveVm.flush(); nav.pop() },
                            onEditExercise = { nav.push(Route.ExerciseEditor(it)) },
                            onOpenHistory = { liveVm.flush(); nav.push(Route.ProgressDetail(it)) },
                            onWorkoutSaved = { saved ->
                                platform.onWorkoutSaved(saved)
                            },
                        )
                        Route.LiveRun, is Route.RunDetail, Route.RoutePlanner ->
                            platform.RunRoute(overlay, settings.units, onClose = { nav.pop() })
                        Route.Settings -> Box(Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(top = 12.dp)) {
                            SettingsScreen(
                                vm = settingsVm,
                                host = platform.rememberSettingsHost(),
                                locationsVm = locationsVm,
                                onOpenData = { nav.push(Route.DataTransfer) },
                                onOpenPrivacy = { nav.push(Route.PrivacyPolicy) },
                                onBack = { nav.pop() },
                            )
                        }
                        is Route.TemplateEditor -> TemplateEditorScreen(
                            vm = libraryVm,
                            templateId = overlay.templateId,
                            onDone = { nav.pop() },
                        )
                        is Route.ExerciseEditor -> ExerciseEditorScreen(
                            vm = libraryVm,
                            exerciseId = overlay.exerciseId,
                            initialName = overlay.initialName,
                            onDone = { nav.pop() },
                        )
                        is Route.WorkoutDetail -> WorkoutDetailScreen(
                            vm = historyVm,
                            locationsVm = locationsVm,
                            sessionId = overlay.sessionId,
                            units = settings.units,
                            onBack = { nav.pop() },
                            onCreateExercise = {
                                nav.push(Route.ExerciseEditor(null, initialName = it))
                            },
                        )
                        is Route.ProgressDetail -> ProgressDetailScreen(
                            vm = progressVm,
                            exerciseId = overlay.exerciseId,
                            units = settings.units,
                            onBack = { nav.pop() },
                            locations = locations,
                        )
                        Route.DataTransfer -> Box(Modifier.fillMaxSize()) {
                            DataTransferScreen(vm = dataVm)
                            BackChip(onBack = { nav.pop() }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp))
                        }
                        Route.PrivacyPolicy -> PrivacyPolicyScreen(onBack = { nav.pop() })
                    }
                }
            }

            // Bottom nav only over the peer tabs (full-screen flows own their chrome).
            if (overlay == null) {
                FloatingNav(
                    current = nav.tab.value,
                    resumeMode = resumeMode,
                    showRuns = platform.hasRuns,
                    onSelect = { nav.switch(it) },
                    onStart = { onCenterAction() },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
                )
            }

            if (chooserOpen) {
                StartChooserSheet(
                    onLift = { chooserOpen = false; startWorkout() },
                    onRun = { chooserOpen = false; startRun() },
                    onDismiss = { chooserOpen = false },
                )
            }

            templates.firstOrNull { it.id == previewTemplateId }?.let { template ->
                val data by produceState(TemplatePreviewData(), template) { value = homeVm.preview(template) }
                TemplatePreviewSheet(
                    template = template,
                    data = data,
                    units = settings.units,
                    defaultRestSeconds = settings.defaultRestSeconds,
                    onEdit = { previewTemplateId = null; nav.push(Route.TemplateEditor(template.id)) },
                    onStart = { previewTemplateId = null; startWorkout(template.id) },
                    onDismiss = { previewTemplateId = null },
                )
            }

            if (resumeChooserOpen) {
                ResumeChooserSheet(
                    onLift = { resumeChooserOpen = false; startWorkout() },
                    onRun = { resumeChooserOpen = false; nav.push(Route.LiveRun) },
                    onDismiss = { resumeChooserOpen = false },
                )
            }

            ToastHost(toast, Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.systemBars))
        }
    }
}

@Composable
private fun BackChip(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        "Done",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onBack)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
