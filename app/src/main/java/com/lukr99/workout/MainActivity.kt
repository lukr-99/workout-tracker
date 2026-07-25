package com.lukr99.workout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.lukr99.workout.ui.App
import com.lukr99.workout.ui.WorkoutViewModel
import com.lukr99.workout.ui.theme.WorkoutTheme

/**
 * Single-activity Compose host. Edge-to-edge with a dark (light-icon) status bar; the UI theme is
 * [WorkoutTheme] (dark-first). Share / open-document intents land here in later phases.
 */
class MainActivity : ComponentActivity() {

    private val vm: WorkoutViewModel by viewModels {
        WorkoutViewModel.factory((application as WorkoutApp).container.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Light-on-dark system bar icons (we render over a near-black background).
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        setContent {
            WorkoutTheme {
                App(vm)
            }
        }
    }
}
