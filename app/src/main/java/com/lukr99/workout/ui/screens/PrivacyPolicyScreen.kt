package com.lukr99.workout.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.components.ScreenHeader
import com.lukr99.workout.ui.components.SectionCard
import com.lukr99.workout.ui.theme.TextMid

/** Shared in-app policy and Android Health Connect permission-rationale destination. */
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    permissionRationale: Boolean = false,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ScreenHeader(
                if (permissionRationale) "Health permissions" else "Privacy policy",
                if (permissionRationale) "Why Workout Tracker requests access" else "Your data stays under your control",
                modifier = Modifier.weight(1f),
            )
        }

        PolicySection("Health data we use") {
            Text(
                "With your permission, Workout Tracker reads exercise sessions and weight records " +
                    "from Health Connect. It can write completed workout sessions and the bodyweight " +
                    "recorded with those sessions.",
            )
        }

        PolicySection("Why access is needed") {
            Text(
                "Read access powers the Import action so workouts from other apps can appear in your " +
                    "history. Write access powers the Export action so workouts completed here can be " +
                    "shared through Health Connect. Sync only runs when you choose Import or Export.",
            )
        }

        PolicySection("Storage and sharing") {
            Text(
                "Workout data is stored in the app on this device. Health data is exchanged directly " +
                    "with Health Connect; Workout Tracker does not sell health data. Automatic backups " +
                    "are off by default and only write to a folder you select.",
            )
        }

        PolicySection("Your choices") {
            Text(
                "Health Connect access is optional. You can review or revoke permissions in Health " +
                    "Connect at any time, keep using Workout Tracker without connecting it, and disable " +
                    "automatic backup from Settings.",
            )
        }

        Text(
            "Last updated: July 26, 2026",
            style = MaterialTheme.typography.labelSmall,
            color = TextMid,
        )
    }
}

@Composable
private fun PolicySection(title: String, content: @Composable () -> Unit) {
    SectionCard {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides TextMid,
                androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                content = content,
            )
        }
    }
}
