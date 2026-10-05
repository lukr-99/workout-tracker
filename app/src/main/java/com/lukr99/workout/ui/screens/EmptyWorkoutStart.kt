package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.ui.theme.Accents
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * What an empty workout shows: a big "Add exercises" target, and your recent exercises as chips that
 * add with one tap, so a workout without a template still starts fast.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EmptyWorkoutStart(recent: List<Exercise>, onAddExercises: () -> Unit, onQuickAdd: (Exercise) -> Unit) {
    val colors = EmberTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val dash = colors.primary.copy(alpha = 0.55f)
        Column(
            Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(22.dp)).background(colors.primarySoft)
                .drawBehind {
                    val stroke = 2.dp.toPx()
                    drawRoundRect(
                        color = dash,
                        cornerRadius = CornerRadius(22.dp.toPx()),
                        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                    )
                }
                .clickable(role = Role.Button, onClick = onAddExercises),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(50.dp).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, null, tint = colors.onPrimary, modifier = Modifier.size(26.dp))
            }
            Text("Add exercises", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 6.dp))
            Text("Search the library, or type a new name", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
        }
        if (recent.isNotEmpty()) {
            Text(
                "RECENT, TAP TO ADD",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 12.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recent.forEach { exercise ->
                    val shape = RoundedCornerShape(50)
                    Row(
                        Modifier.heightIn(min = 40.dp).clip(shape).background(colors.surface).border(1.dp, colors.border, shape)
                            .clickable(role = Role.Button, onClickLabel = "Add ${exercise.name}") { onQuickAdd(exercise) }
                            .padding(start = 10.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Box(Modifier.size(8.dp).background(Accents.bodyPart(exercise.primaryBodyPart), CircleShape))
                        Text(exercise.name, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Icon(Icons.Rounded.Add, null, tint = colors.primaryText, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
