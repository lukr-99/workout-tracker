package com.lukr99.workout.ui
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.settings.SettingsHost
class PlatformApp {
    val runningSection: (@Composable (UnitSystem) -> Unit)? = null
    val liveRunActive: Boolean = false
    val updateCard: (@Composable () -> Unit)? = null
    val locals: Array<ProvidedValue<*>> = arrayOf()
    fun prepareRun(routeId: String?) {}
    suspend fun onWorkoutSaved(session: WorkoutSession) {}
    @Composable fun RunsTab(units: UnitSystem, onStartRun: () -> Unit, onOpenRun: (String) -> Unit, onPlanRoute: () -> Unit, onStartRoute: (String) -> Unit) {}
    @Composable fun RunRoute(route: Route, units: UnitSystem, onClose: () -> Unit) {}
    @Composable fun rememberSettingsHost(): SettingsHost = TODO()
}
@Composable fun rememberPlatformApp(container: AppContainer): PlatformApp = TODO()
