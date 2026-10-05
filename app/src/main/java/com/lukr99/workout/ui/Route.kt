package com.lukr99.workout.ui

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
