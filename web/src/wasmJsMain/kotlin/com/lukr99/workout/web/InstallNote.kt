package com.lukr99.workout.web

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.web.platform.isIosDevice
import com.lukr99.workout.web.platform.isStandaloneApp
import com.lukr99.workout.web.platform.readLocalFlag
import com.lukr99.workout.web.platform.writeLocalFlag

private const val DISMISSED_KEY = "ember.installNote.dismissed"

/**
 * A gentle note on an iPhone or iPad when Ember runs in a Safari tab instead of from the home screen:
 * how to add it, and why (Safari may delete the data of a site that was not opened for 7 days, unless
 * it is on the home screen). "Got it" hides it on this device for good.
 */
@Composable
internal fun InstallNote(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(isIosDevice() && !isStandaloneApp() && readLocalFlag(DISMISSED_KEY) == null) }
    if (!visible) return
    val colors = EmberTheme.colors
    Column(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceRaised).border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 4.dp),
    ) {
        Text("Add Ember to your Home Screen", color = colors.textPrimary, style = MaterialTheme.typography.titleSmall)
        Text(
            "Tap Share, then Add to Home Screen, and open Ember from there. In a Safari tab, Safari may " +
                "delete your workouts if you do not visit for 7 days, and the Home Screen app keeps its own " +
                "data, so export a backup first if you already logged workouts here.",
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, end = 8.dp),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                writeLocalFlag(DISMISSED_KEY, "1")
                visible = false
            }) { Text("Got it", color = colors.primaryText) }
        }
    }
}
