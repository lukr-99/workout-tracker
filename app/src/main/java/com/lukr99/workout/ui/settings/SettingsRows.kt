package com.lukr99.workout.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.theme.EmberTheme

/*
 * The Settings row types from CodePrint's settings guide. Rows are at least 56 dp, split by a thin
 * line; the hint says what the setting does, and a result or an error replaces the hint.
 */

/** A row with a label, an optional hint and a control on the right. */
@Composable
fun SettingsRow(
    label: String,
    hint: String?,
    first: Boolean = false,
    hintIsError: Boolean = false,
    onClick: (() -> Unit)? = null,
    control: @Composable () -> Unit = {},
) {
    val colors = EmberTheme.colors
    Column {
        if (!first) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                if (!hint.isNullOrBlank()) {
                    Text(hint, style = MaterialTheme.typography.bodyMedium, color = if (hintIsError) colors.danger else colors.textSecondary)
                }
            }
            control()
        }
    }
}

@Composable
fun ToggleRow(label: String, hint: String?, checked: Boolean, first: Boolean = false, busy: Boolean = false, onChange: (Boolean) -> Unit) {
    val colors = EmberTheme.colors
    SettingsRow(label, hint, first) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = colors.primary)
        } else {
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                modifier = Modifier.semantics { contentDescription = label },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = colors.primary,
                    checkedThumbColor = colors.onPrimary,
                    uncheckedTrackColor = colors.surfaceRaised,
                    uncheckedThumbColor = colors.textTertiary,
                    uncheckedBorderColor = colors.border,
                ),
            )
        }
    }
}

/** Opens a page in the app (›) or a site outside it (↗). The whole row is the target. */
@Composable
fun LinkRow(label: String, hint: String?, outside: Boolean = false, first: Boolean = false, onClick: () -> Unit) {
    SettingsRow(label, hint, first, onClick = onClick) {
        Icon(
            if (outside) Icons.AutoMirrored.Rounded.OpenInNew else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = EmberTheme.colors.textSecondary,
            modifier = Modifier.size(if (outside) 18.dp else 22.dp),
        )
    }
}

/** An action with a result. While [busy] it shows a spinner and [busyLabel], and cannot be pressed. */
@Composable
fun ButtonRow(
    label: String,
    hint: String?,
    action: String,
    first: Boolean = false,
    busy: Boolean = false,
    busyLabel: String = "Working…",
    hintIsError: Boolean = false,
    onClick: () -> Unit,
) {
    SettingsRow(label, hint, first, hintIsError) { PillButton(if (busy) busyLabel else action, busy = busy, onClick = onClick) }
}

@Composable
fun PillButton(text: String, busy: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.height(40.dp).clip(shape).background(if (danger) colors.surface else colors.surfaceRaised)
            .border(if (danger) 1.5.dp else 1.dp, if (danger) colors.danger else colors.border, shape)
            .clickable(enabled = !busy, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.primary)
        Text(text, fontWeight = FontWeight.SemiBold, color = if (danger) colors.danger else colors.textPrimary)
    }
}

/** The outlined block for destructive actions, at the end of the section it affects. */
@Composable
fun DangerZone(content: @Composable () -> Unit) {
    val colors = EmberTheme.colors
    Column(
        Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, colors.danger, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text("DANGER ZONE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.danger)
        content()
    }
}
