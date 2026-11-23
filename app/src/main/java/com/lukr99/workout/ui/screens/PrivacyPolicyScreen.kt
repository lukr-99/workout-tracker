package com.lukr99.workout.ui.screens

import com.lukr99.workout.ui.i18n.PrivacyPolicyText
import com.lukr99.workout.ui.i18n.CommonText
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
import com.lukr99.workout.ui.theme.EmberTheme

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
                    CommonText.back.text(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ScreenHeader(
                if (permissionRationale) PrivacyPolicyText.rationaleTitle.text() else PrivacyPolicyText.title.text(),
                if (permissionRationale) PrivacyPolicyText.rationaleSubtitle.text() else PrivacyPolicyText.subtitle.text(),
                modifier = Modifier.weight(1f),
            )
        }

        PolicySection(PrivacyPolicyText.healthDataTitle.text()) {
            Text(PrivacyPolicyText.healthDataBody.text())
        }

        PolicySection(PrivacyPolicyText.accessTitle.text()) {
            Text(PrivacyPolicyText.accessBody.text())
        }

        PolicySection(PrivacyPolicyText.storageTitle.text()) {
            Text(PrivacyPolicyText.storageBody.text())
        }

        PolicySection(PrivacyPolicyText.claudeTitle.text()) {
            Text(PrivacyPolicyText.claudeBody.text())
        }

        PolicySection(PrivacyPolicyText.choicesTitle.text()) {
            Text(PrivacyPolicyText.choicesBody.text())
        }

        Text(
            PrivacyPolicyText.lastUpdated.text(),
            style = MaterialTheme.typography.labelSmall,
            color = EmberTheme.colors.textSecondary,
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
                androidx.compose.material3.LocalContentColor provides EmberTheme.colors.textSecondary,
                androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                content = content,
            )
        }
    }
}
