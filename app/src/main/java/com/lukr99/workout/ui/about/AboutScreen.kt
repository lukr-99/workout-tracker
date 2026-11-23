package com.lukr99.workout.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.i18n.AboutText
import com.lukr99.workout.ui.i18n.CommonText
import com.lukr99.workout.ui.i18n.LocalLanguage
import com.lukr99.workout.ui.AppTags
import com.lukr99.workout.ui.components.RoundIconButton
import com.lukr99.workout.ui.settings.LinkRow
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/**
 * About Ember, opened from Settings: the hero, who made it and how to reach them, the app's facts,
 * what it keeps and where, and the credits. Shared by the Android app and the web edition; [edition]
 * picks the lines that differ.
 */
@Composable
fun AboutScreen(
    version: String,
    edition: AboutEdition,
    onOpenPrivacy: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EmberTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .testTag(AppTags.ABOUT)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, CommonText.back.text(), onBack)
            Text(AboutText.title.text(), style = MaterialTheme.typography.displayLarge, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
        }
        AboutHero(version, edition)
        AboutAuthorCard()
        Facts(version, edition)
        DataCard(edition, onOpenPrivacy)
        AboutCreditsCard(edition)
        Text(
            AboutText.copyright.text("name" to AuthorLinks.NAME),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

/** Three small tiles: the version, the edition and the account Ember does not need. */
@Composable
private fun Facts(version: String, edition: AboutEdition) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FactTile(AboutText.factVersion.text(), version.removePrefix("v").ifBlank { "?" }, Modifier.weight(1f))
        FactTile(AboutText.factRunsOn.text(), AboutContent.editionName(edition, LocalLanguage.current), Modifier.weight(1f))
        FactTile(AboutText.factAccount.text(), AboutText.factNone.text(), Modifier.weight(1f))
    }
}

@Composable
private fun FactTile(label: String, value: String, modifier: Modifier) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(colors.surface).border(1.dp, colors.border, shape).padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
        Text(value, style = Numbers.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.8f), color = colors.textPrimary, maxLines = 2)
    }
}

/** What Ember keeps and where, as short points, with the full privacy policy one tap away. */
@Composable
private fun DataCard(edition: AboutEdition, onOpenPrivacy: () -> Unit) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(colors.surface).border(1.dp, colors.border, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(AboutText.dataTitle.text(), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
        AboutContent.privacy(edition, LocalLanguage.current).forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.padding(top = 8.dp).size(7.dp).clip(CircleShape).background(colors.primary))
                Text(line, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.weight(1f))
            }
        }
        LinkRow(
            AboutText.privacyPolicy.text(),
            if (edition == AboutEdition.Android) AboutText.privacyPolicyAndroid.text() else AboutText.privacyPolicyWeb.text(),
            onClick = onOpenPrivacy,
        )
    }
}
