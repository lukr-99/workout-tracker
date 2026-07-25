package com.lukr99.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.ui.screens.CatalogScreen
import com.lukr99.workout.ui.screens.HistoryScreen
import com.lukr99.workout.ui.screens.HomeScreen
import com.lukr99.workout.ui.screens.SettingsScreen
import com.lukr99.workout.ui.screens.StatsScreen
import com.lukr99.workout.ui.screens.TemplatesScreen
import com.lukr99.workout.ui.screens.WorkoutScreen

/** The app's top-level tabs (screen inventory, 02-design-system.md). */
enum class Screen(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Dashboard),
    WORKOUT("Workout", Icons.Rounded.FitnessCenter),
    TEMPLATES("Templates", Icons.Rounded.ListAlt),
    CATALOG("Catalog", Icons.Rounded.ViewList),
    HISTORY("History", Icons.Rounded.History),
    STATS("Stats", Icons.Rounded.ShowChart),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

@Composable
fun App(vm: WorkoutViewModel) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 104.dp),
        ) {
            when (screen) {
                Screen.HOME -> HomeScreen(vm)
                Screen.WORKOUT -> WorkoutScreen(vm)
                Screen.TEMPLATES -> TemplatesScreen(vm)
                Screen.CATALOG -> CatalogScreen(vm)
                Screen.HISTORY -> HistoryScreen(vm)
                Screen.STATS -> StatsScreen(vm)
                Screen.SETTINGS -> SettingsScreen(vm)
            }
        }
        FloatingNav(
            screen, { screen = it },
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(start = 14.dp, end = 14.dp, bottom = 20.dp),
        )
    }
}

/**
 * Full-width rounded floating bottom nav (ported from ring-set's App.kt): equal-width columns so
 * the bar never resizes between tabs; the selected item is tinted `primary @ 16%` behind its icon
 * with a small always-on label.
 */
@Composable
private fun FloatingNav(current: Screen, onSelect: (Screen) -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 3.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (s in Screen.entries) {
                val on = s == current
                val tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    Modifier.weight(1f).clickable { onSelect(s) }.padding(vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.background(
                            if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                            CircleShape,
                        ).padding(horizontal = 12.dp, vertical = 5.dp),
                    ) { Icon(s.icon, s.label, tint = tint, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        s.label, color = tint, fontSize = 9.5.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1, softWrap = false,
                    )
                }
            }
        }
    }
}
