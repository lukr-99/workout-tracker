package com.lukr99.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * Full-width rounded floating nav: four equal-width tabs split around a central raised ember Start
 * action (container-transform into the live session). Selected tabs tint `primary @ 16%` behind the
 * icon. When a session is live the center action shows a ▶ resume glyph instead of ＋.
 */
@Composable
internal fun FloatingNav(
    current: Tab,
    resumeMode: Boolean,
    onSelect: (Tab) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 3.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Tab.HOME, current, Modifier.weight(1f)) { onSelect(Tab.HOME) }
            NavItem(Tab.LIBRARY, current, Modifier.weight(1f)) { onSelect(Tab.LIBRARY) }
            StartAction(resumeMode, onStart, Modifier.weight(1f))
            NavItem(Tab.RUNS, current, Modifier.weight(1f)) { onSelect(Tab.RUNS) }
            NavItem(Tab.PROGRESS, current, Modifier.weight(1f)) { onSelect(Tab.PROGRESS) }
        }
    }
}

@Composable
private fun NavItem(tab: Tab, current: Tab, modifier: Modifier, onClick: () -> Unit) {
    val on = tab == current
    val tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier.testTag(AppTags.nav(tab)).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.background(
                if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                CircleShape,
            ).padding(horizontal = 12.dp, vertical = 5.dp),
        ) { Icon(tab.icon, tab.label, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.height(2.dp))
        Text(
            tab.label, color = tint, fontSize = 12.sp,
            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1, softWrap = false,
        )
    }
}

@Composable
private fun StartAction(resumeMode: Boolean, onStart: () -> Unit, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.testTag(AppTags.NAV_START).size(52.dp).clip(CircleShape)
                .background(EmberTheme.colors.primary)
                .clickable(onClick = onStart),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (resumeMode) Icons.Rounded.PlayArrow else Icons.Rounded.Add,
                contentDescription = if (resumeMode) "Resume workout" else "Start workout",
                tint = EmberTheme.colors.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            if (resumeMode) "Resume" else "Start",
            color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
