package com.lukr99.workout.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/**
 * Holds the selected tab and the overlay back-stack as Compose state. Keep it with
 * `rememberSaveable(saver = Navigator.Saver)`: a plain `remember` loses the stack when the screen
 * rotates and the app jumps back to Home.
 */
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

    companion object {
        /**
         * Saves the tab and every route as plain strings, so it works in an Android Bundle and in
         * the web edition alike. A route that no longer decodes is dropped, never a crash.
         */
        val Saver: Saver<Navigator, Any> = listSaver(
            save = { nav -> listOf<Any>(nav.tab.value.name) + nav.stack.map(::encode) },
            restore = { saved ->
                Navigator().apply {
                    val name = saved.firstOrNull() as? String
                    switch(Tab.entries.firstOrNull { it.name == name } ?: Tab.HOME)
                    saved.drop(1).forEach { item -> (item as? List<*>)?.let(::decode)?.let(::push) }
                }
            },
        )

        private fun encode(route: Route): ArrayList<String?> = when (route) {
            Route.LiveWorkout -> arrayListOf("LiveWorkout")
            Route.LiveRun -> arrayListOf("LiveRun")
            is Route.RunDetail -> arrayListOf("RunDetail", route.runId)
            Route.RoutePlanner -> arrayListOf("RoutePlanner")
            Route.Settings -> arrayListOf("Settings")
            is Route.TemplateEditor -> arrayListOf("TemplateEditor", route.templateId)
            is Route.ExerciseEditor -> arrayListOf("ExerciseEditor", route.exerciseId, route.initialName)
            is Route.WorkoutDetail -> arrayListOf("WorkoutDetail", route.sessionId)
            is Route.ProgressDetail -> arrayListOf("ProgressDetail", route.exerciseId)
            Route.DataTransfer -> arrayListOf("DataTransfer")
            Route.PrivacyPolicy -> arrayListOf("PrivacyPolicy")
            Route.About -> arrayListOf("About")
        }

        private fun decode(parts: List<*>): Route? {
            val arg = parts.getOrNull(1) as? String
            return when (parts.firstOrNull()) {
                "LiveWorkout" -> Route.LiveWorkout
                "LiveRun" -> Route.LiveRun
                "RunDetail" -> arg?.let(Route::RunDetail)
                "RoutePlanner" -> Route.RoutePlanner
                "Settings" -> Route.Settings
                "TemplateEditor" -> Route.TemplateEditor(arg)
                "ExerciseEditor" -> Route.ExerciseEditor(arg, parts.getOrNull(2) as? String ?: "")
                "WorkoutDetail" -> arg?.let(Route::WorkoutDetail)
                "ProgressDetail" -> arg?.let(Route::ProgressDetail)
                "DataTransfer" -> Route.DataTransfer
                "PrivacyPolicy" -> Route.PrivacyPolicy
                "About" -> Route.About
                else -> null
            }
        }
    }
}
