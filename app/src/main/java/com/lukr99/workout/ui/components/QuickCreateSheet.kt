package com.lukr99.workout.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.ui.theme.Accents
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * A new exercise in a few taps, from the picker's "Create" row: name, kind, main muscle, what else
 * it works and the equipment. Steps, a photo and a guide link can wait for the Library editor.
 * [onCreate] gets the new exercise; the caller saves it and adds it to the workout.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickCreateSheet(
    initialName: String,
    bodyParts: List<String>,
    onCreate: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = EmberTheme.colors
    var name by remember { mutableStateOf(initialName) }
    var category by remember { mutableStateOf(ExerciseCategory.Strength) }
    var main by remember { mutableStateOf("") }
    var also by remember { mutableStateOf(emptySet<String>()) }
    var equipment by remember { mutableStateOf("") }
    val canCreate = name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 20.dp)) {
            Text("New exercise", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text("Saved to your library and added to this workout.", style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)

            Label("Name")
            val shape = RoundedCornerShape(14.dp)
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary, fontSize = 17.sp),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().clip(shape).background(colors.surfaceRaised).border(1.dp, colors.border, shape)
                    .padding(horizontal = 14.dp, vertical = 14.dp).semantics { contentDescription = "Name" },
            )

            Label("Kind")
            SegmentedControl(ExerciseCategory.entries, category, { category = it }, label = { it.name })

            Label("Main muscle")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bodyParts.forEach { part ->
                    FilterChip(part, main == part, {
                        main = if (main == part) "" else part
                        also = also - part
                    }, dot = Accents.bodyPart(part))
                }
            }

            Label("Also works")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bodyParts.filter { it != main }.forEach { part ->
                    FilterChip(part, part in also, { also = if (part in also) also - part else also + part }, dot = Accents.bodyPart(part))
                }
            }

            Label("Equipment")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EQUIPMENT.forEach { item -> FilterChip(item, equipment == item, { equipment = if (equipment == item) "" else item }) }
            }

            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Info, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                Text(
                    "Steps, a photo and a guide link can wait. Add them later from the Library.",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
            }

            Box(
                Modifier.padding(top = 16.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp))
                    .background(if (canCreate) colors.primary else colors.surfaceRaised)
                    .clickable(enabled = canCreate, role = Role.Button) {
                        onCreate(
                            Exercise(
                                name = name.trim(),
                                category = category,
                                primaryBodyPart = main,
                                secondaryBodyParts = also.toList(),
                                equipment = equipment,
                                source = ExerciseSource.Custom,
                            ),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Create and add", fontWeight = FontWeight.Bold, color = if (canCreate) colors.onPrimary else colors.textSecondary)
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = EmberTheme.colors.textSecondary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

private val EQUIPMENT = listOf("Barbell", "Dumbbell", "Cable", "Machine", "Body weight", "Kettlebell", "Band", "Other")
