package com.lukr99.workout.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The four peer tabs of the 5-item shell (the center Start action is not a tab). Run Mode (v2.1)
 * reshuffled the slots: `Runs` takes History's old slot and History moved into `Progress`.
 */
enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    RUNS("Runs", Icons.AutoMirrored.Rounded.DirectionsRun),
    PROGRESS("Progress", Icons.Rounded.BarChart),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

/** Full-screen flows layered over the tabs (a simple manual back-stack, ring-set style). */
sealed interface Route {
    data object LiveWorkout : Route
    /** Live-run flow (R0: a stubbed dark map that follows your location; recording lands in R1). */
    data object LiveRun : Route
    data class RunDetail(val runId: String) : Route
    data object RoutePlanner : Route
    data object Library : Route
    data class TemplateEditor(val templateId: String?) : Route
    data class ExerciseEditor(val exerciseId: String?, val initialName: String = "") : Route
    data class WorkoutDetail(val sessionId: String) : Route
    data class ProgressDetail(val exerciseId: String) : Route
    data object DataTransfer : Route
    data object PrivacyPolicy : Route
}

/** Holds the selected tab and the overlay back-stack as Compose state. */
class Navigator {
    var tab = mutableStateOf(Tab.HOME)
        private set
    val stack = mutableStateListOf<Route>()

    val top: Route? get() = stack.lastOrNull()

    fun switch(target: Tab) {
        tab.value = target
    }

    fun push(route: Route) {
        stack.add(route)
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    fun popTo(route: Route) {
        val index = stack.indexOf(route)
        if (index >= 0) while (stack.size > index) stack.removeAt(stack.lastIndex)
    }
}
