package com.lukr99.workout.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.ToastHost
import com.lukr99.workout.ui.components.rememberToastState
import com.lukr99.workout.ui.screens.DataTransferScreen
import com.lukr99.workout.ui.screens.ExerciseEditorScreen
import com.lukr99.workout.ui.screens.HistoryScreen
import com.lukr99.workout.ui.screens.HomeScreen
import com.lukr99.workout.ui.screens.LibraryScreen
import com.lukr99.workout.ui.screens.LiveWorkoutScreen
import com.lukr99.workout.ui.screens.ProgressDetailScreen
import com.lukr99.workout.ui.screens.ProgressScreen
import com.lukr99.workout.ui.screens.PrivacyPolicyScreen
import com.lukr99.workout.ui.screens.SettingsScreen
import com.lukr99.workout.ui.screens.TemplateEditorScreen
import com.lukr99.workout.ui.screens.WorkoutDetailScreen

/**
 * App root. The decided 5-item shell — `Home · History · (＋ Start) · Progress · Settings` — with the
 * center ember Start action starting/resuming the live workout (not a peer tab). Peer tabs cross-fade;
 * full-screen flows layer over them through a manual back-stack ([Navigator]). See 02-design-system.md.
 */
@Composable
fun App(container: AppContainer) {
    val nav = remember { Navigator() }
    val toast = rememberToastState()

    val homeVm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val historyVm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
    val progressVm: ProgressViewModel = viewModel(factory = ProgressViewModel.factory(container))
    val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val libraryVm: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(container))
    val liveVm: LiveWorkoutViewModel = viewModel(factory = LiveWorkoutViewModel.factory(container))
    val dataVm: DataTransferViewModel = viewModel(
        factory = DataTransferViewModel.factory(container.dataTransfer, container.documents),
    )

    val settings by settingsVm.settings.collectAsState()
    val activeSession by homeVm.activeSession.collectAsState()
    val overlay = nav.top

    fun startWorkout(templateId: String? = null) {
        liveVm.startOrResume(templateId)
        nav.push(Route.LiveWorkout)
    }

    CompositionLocalProvider(LocalToast provides toast) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Peer-tab layer (always mounted so tab state persists behind overlays).
            Column(
                Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 18.dp)
                    .padding(top = 12.dp),
            ) {
                Crossfade(targetState = nav.tab.value, animationSpec = tween(220), label = "tab") { tab ->
                    when (tab) {
                        Tab.HOME -> HomeScreen(
                            vm = homeVm,
                            units = settings.units,
                            onStart = { startWorkout() },
                            onStartTemplate = { startWorkout(it) },
                            onOpenLibrary = { nav.push(Route.Library) },
                            onOpenSession = { nav.push(Route.WorkoutDetail(it)) },
                        )
                        Tab.HISTORY -> HistoryScreen(
                            vm = historyVm,
                            units = settings.units,
                            onOpen = { nav.push(Route.WorkoutDetail(it)) },
                        )
                        Tab.PROGRESS -> ProgressScreen(
                            vm = progressVm,
                            units = settings.units,
                            onOpenExercise = { nav.push(Route.ProgressDetail(it)) },
                        )
                        Tab.SETTINGS -> SettingsScreen(
                            vm = settingsVm,
                            onOpenData = { nav.push(Route.DataTransfer) },
                            onOpenPrivacy = { nav.push(Route.PrivacyPolicy) },
                        )
                    }
                }
            }

            // Overlay layer (full-screen flows).
            if (overlay != null) {
                BackHandler(enabled = true) { nav.pop() }
                Box(
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.systemBars),
                ) {
                    when (overlay) {
                        Route.LiveWorkout -> LiveWorkoutScreen(
                            vm = liveVm,
                            units = settings.units,
                            onClose = { nav.pop() },
                        )
                        Route.Library -> LibraryScreen(
                            vm = libraryVm,
                            units = settings.units,
                            onBack = { nav.pop() },
                            onEditTemplate = { nav.push(Route.TemplateEditor(it)) },
                            onNewTemplate = { nav.push(Route.TemplateEditor(null)) },
                            onEditExercise = { nav.push(Route.ExerciseEditor(it)) },
                            onNewExercise = { nav.push(Route.ExerciseEditor(null)) },
                            onStartTemplate = { startWorkout(it) },
                        )
                        is Route.TemplateEditor -> TemplateEditorScreen(
                            vm = libraryVm,
                            templateId = overlay.templateId,
                            onDone = { nav.pop() },
                        )
                        is Route.ExerciseEditor -> ExerciseEditorScreen(
                            vm = libraryVm,
                            exerciseId = overlay.exerciseId,
                            onDone = { nav.pop() },
                        )
                        is Route.WorkoutDetail -> WorkoutDetailScreen(
                            vm = historyVm,
                            sessionId = overlay.sessionId,
                            units = settings.units,
                            onBack = { nav.pop() },
                        )
                        is Route.ProgressDetail -> ProgressDetailScreen(
                            vm = progressVm,
                            exerciseId = overlay.exerciseId,
                            units = settings.units,
                            onBack = { nav.pop() },
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
                    resumeMode = activeSession != null,
                    onSelect = { nav.switch(it) },
                    onStart = { startWorkout() },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
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

/**
 * Full-width rounded floating nav: four equal-width tabs split around a central raised ember Start
 * action (container-transform into the live session). Selected tabs tint `primary @ 16%` behind the
 * icon. When a session is live the center action shows a ▶ resume glyph instead of ＋.
 */
@Composable
private fun FloatingNav(
    current: Tab,
    resumeMode: Boolean,
    onSelect: (Tab) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 3.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Tab.HOME, current, Modifier.weight(1f)) { onSelect(Tab.HOME) }
            NavItem(Tab.HISTORY, current, Modifier.weight(1f)) { onSelect(Tab.HISTORY) }
            StartAction(resumeMode, onStart, Modifier.weight(1f))
            NavItem(Tab.PROGRESS, current, Modifier.weight(1f)) { onSelect(Tab.PROGRESS) }
            NavItem(Tab.SETTINGS, current, Modifier.weight(1f)) { onSelect(Tab.SETTINGS) }
        }
    }
}

@Composable
private fun NavItem(tab: Tab, current: Tab, modifier: Modifier, onClick: () -> Unit) {
    val on = tab == current
    val tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.background(
                if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                CircleShape,
            ).padding(horizontal = 12.dp, vertical = 5.dp),
        ) { Icon(tab.icon, tab.label, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.height(2.dp))
        Text(
            tab.label, color = tint, fontSize = 9.5.sp,
            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1, softWrap = false,
        )
    }
}

@Composable
private fun StartAction(resumeMode: Boolean, onStart: () -> Unit, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(52.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onStart),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (resumeMode) Icons.Rounded.PlayArrow else Icons.Rounded.Add,
                contentDescription = if (resumeMode) "Resume workout" else "Start workout",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            if (resumeMode) "Resume" else "Start",
            color = MaterialTheme.colorScheme.primary, fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
