package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/** Sets a rep range for one template exercise ("6 to 8"), or clears it. Equal ends mean an exact count. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RepRangeSheet(
    exerciseName: String,
    initialMin: Int?,
    initialMax: Int?,
    onSave: (min: Int?, max: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = EmberTheme.colors
    var min by remember { mutableIntStateOf(initialMin ?: initialMax ?: 8) }
    var max by remember { mutableIntStateOf(initialMax ?: initialMin ?: 12) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text("Reps for $exerciseName", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Text("Aim for the range; the workout starts each set at the top.", style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Counter("From", min, Modifier.weight(1f)) { min = it.coerceIn(1, 100); if (max < min) max = min }
                Counter("To", max, Modifier.weight(1f)) { max = it.coerceIn(1, 100); if (min > max) min = max }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button("No range", filled = false, Modifier.weight(1f)) { onSave(null, null); onDismiss() }
                Button("Save", filled = true, Modifier.weight(1f)) { onSave(min, max); onDismiss() }
            }
        }
    }
}

@Composable
private fun Counter(label: String, value: Int, modifier: Modifier, onValue: (Int) -> Unit) {
    val colors = EmberTheme.colors
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(colors.surfaceRaised).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Key(Icons.Rounded.Remove, "$label fewer") { onValue(value - 1) }
            Text(value.toString(), style = Numbers.copy(fontSize = 34.sp), color = colors.textPrimary)
            Key(Icons.Rounded.Add, "$label more") { onValue(value + 1) }
        }
    }
}

@Composable
private fun Key(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).border(1.dp, colors.border, RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = colors.textPrimary) }
}

@Composable
private fun Button(text: String, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(27.dp)
    Box(
        modifier.height(54.dp).clip(shape).background(if (filled) colors.primary else colors.surfaceRaised)
            .then(if (filled) Modifier else Modifier.border(1.dp, colors.border, shape))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontWeight = FontWeight.Bold, color = if (filled) colors.onPrimary else colors.textPrimary) }
}
