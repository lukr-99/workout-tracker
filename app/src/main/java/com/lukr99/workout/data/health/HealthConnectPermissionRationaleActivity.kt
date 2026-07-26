package com.lukr99.workout.data.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.lukr99.workout.ui.screens.PrivacyPolicyScreen
import com.lukr99.workout.ui.theme.WorkoutTheme

/** Required Health Connect endpoint, rendered with the same policy shown from app Settings. */
class HealthConnectPermissionRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkoutTheme {
                Box(
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.systemBars),
                ) {
                    PrivacyPolicyScreen(onBack = ::finish, permissionRationale = true)
                }
            }
        }
    }
}
