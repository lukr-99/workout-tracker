package com.lukr99.workout.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.effectiveTags
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/**
 * One set while logging: `set · previous · weight · reps · check`. The set you are on (the first one
 * not done) has an orange outline. Tapping weight or reps opens [SetEntrySheet]; the badge opens set
 * options. [previous] is the same set from last time, shown greyed for reference.
 */
@Composable
fun SetRow(
    index: Int,
    total: Int,
    set: StrengthSet,
    units: UnitSystem,
    done: Boolean,
    current: Boolean,
    exerciseName: String,
    previous: StrengthSet?,
    before: StrengthSet?,
    onReps: (Int) -> Unit,
    onWeightKg: (Double) -> Unit,
    onToggleDone: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EmberTheme.colors
    val bg by animateColorAsState(if (done) colors.surface else colors.surfaceRaised, label = "setRowBg")
    var editing by remember { mutableStateOf<SetField?>(null) }
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .then(if (current && !done) Modifier.border(1.5.dp, colors.primary, shape) else Modifier)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SetBadge(index = index, set = set, onClick = onOptions)
        Text(
            previous?.let { "${Format.weight(it.weightKg, units)} \u00d7 ${it.reps}" } ?: "\u2013",
            style = Numbers.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            color = colors.textTertiary,
            maxLines = 1,
            modifier = Modifier.weight(1f).semantics { contentDescription = previous?.let { "Last time ${Format.weight(it.weightKg, units)} ${Format.unitLabel(units)} for ${it.reps}" } ?: "No last time" },
        )
        ValueCell(
            display = SetEntryState.trim(Format.toDisplay(set.weightKg, units)),
            done = done,
            label = "Weight, set ${index + 1}",
            modifier = Modifier.width(76.dp),
        ) { editing = SetField.Weight }
        ValueCell(
            display = set.reps.toString(),
            done = done,
            label = "Reps, set ${index + 1}",
            modifier = Modifier.width(60.dp),
        ) { editing = SetField.Reps }
        DoneCheck(done, index, onToggleDone)
    }
    SetTagChips(set, Modifier.fillMaxWidth().padding(start = 48.dp, top = 3.dp))
    SetNoteLine(set)

    editing?.let { field ->
        val last = previous?.let { " \u00b7 last time ${Format.weight(it.weightKg, units)} \u00d7 ${it.reps}" }.orEmpty()
        SetEntrySheet(
            title = exerciseName,
            subtitle = "Set ${index + 1} of $total$last",
            initial = SetEntryState.of(Format.toDisplay(set.weightKg, units), set.reps, field),
            unitLabel = Format.unitLabel(units),
            weightStep = if (units == UnitSystem.Imperial) 5.0 else 2.5,
            copyLabel = before?.let { "Set $index" },
            copyFrom = before?.let { Format.toDisplay(it.weightKg, units) to it.reps },
            onChange = { weight, reps ->
                onWeightKg(Format.toKg(weight, units))
                onReps(reps)
            },
            onDone = {
                editing = null
                if (!done) onToggleDone()
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ValueCell(display: String, done: Boolean, label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    Box(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (done) androidx.compose.ui.graphics.Color.Transparent else colors.background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$label: $display" },
        contentAlignment = Alignment.Center,
    ) {
        Text(display, style = Numbers.copy(fontSize = 21.sp), color = colors.textPrimary)
    }
}

@Composable
private fun DoneCheck(done: Boolean, index: Int, onToggleDone: () -> Unit) {
    val colors = EmberTheme.colors
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val checkScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (done) 1f else 0.86f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
        ),
        label = "checkSpring",
    )
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(42.dp)
            .clip(shape)
            .background(if (done) colors.successSoft else androidx.compose.ui.graphics.Color.Transparent)
            .then(if (done) Modifier else Modifier.border(1.5.dp, colors.border, shape))
            .toggleable(value = done, role = Role.Checkbox) {
                if (!done) haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onToggleDone()
            }
            .semantics { contentDescription = "Set ${index + 1} done" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Check,
            contentDescription = null,
            tint = if (done) colors.success else colors.textTertiary,
            modifier = Modifier.size(22.dp).graphicsLayer { scaleX = checkScale; scaleY = checkScale },
        )
    }
}

/**
 * Small, horizontally scrollable chips so combined tags remain visible outside the edit sheet.
 *
 * Deliberately a plain scrolling [Row] and not a `LazyRow`: a set row is laid out inside cards that
 * may be measured with intrinsics, and lazy lists are `SubcomposeLayout`s, which cannot answer an
 * intrinsic measurement and throw instead. A handful of chips needs no recycling anyway.
 */
@Composable
fun SetTagChips(set: StrengthSet, modifier: Modifier = Modifier) {
    val visibleTags = buildList {
        if (set.isPr) add("PR" to EmberTheme.colors.success)
        set.effectiveTags.forEach { tag ->
            add(tag.label to when (tag) {
                SetTag.Warmup -> EmberTheme.colors.warning
                SetTag.Failed -> MaterialTheme.colorScheme.error
                SetTag.ToFailure -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.secondary
            })
        }
    }
    if (visibleTags.isEmpty()) return
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visibleTags.forEach { (label, tint) ->
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.13f))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = tint)
            }
        }
    }
}

/** A set's own note under its row, aligned with the tag chips. Nothing when the set has none. */
@Composable
fun SetNoteLine(set: StrengthSet, modifier: Modifier = Modifier) {
    if (set.notes.isBlank()) return
    NoteLine(
        Icons.AutoMirrored.Rounded.Notes,
        label = null,
        text = set.notes,
        description = "Set note",
        maxLines = 2,
        modifier = modifier.padding(start = 48.dp),
    )
}

/** SET, PREVIOUS, KG and REPS labels aligned to the [SetRow] columns. Render once above a set list. */
@Composable
fun SetColumnHeader(units: UnitSystem, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ColumnLabel("SET", Modifier.width(34.dp))
        ColumnLabel("PREVIOUS", Modifier.weight(1f), start = true)
        ColumnLabel(Format.unitLabel(units).uppercase(), Modifier.width(76.dp))
        ColumnLabel("REPS", Modifier.width(60.dp))
        Spacer(Modifier.width(42.dp))
    }
}

@Composable
private fun ColumnLabel(text: String, modifier: Modifier, start: Boolean = false) {
    Box(modifier, contentAlignment = if (start) Alignment.CenterStart else Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = EmberTheme.colors.textSecondary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SetBadge(index: Int, set: StrengthSet, onClick: () -> Unit) {
    val (label, tint) = when {
        set.isPr -> "PR" to EmberTheme.colors.success
        SetTag.Failed in set.effectiveTags -> "!" to MaterialTheme.colorScheme.error
        set.isWarmup || SetTag.Warmup in set.effectiveTags -> "W" to EmberTheme.colors.warning
        SetTag.ToFailure in set.effectiveTags -> "TF" to MaterialTheme.colorScheme.primary
        SetTag.Drop in set.effectiveTags -> "D" to MaterialTheme.colorScheme.secondary
        else -> "${index + 1}" to EmberTheme.colors.textSecondary
    }
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f))
            .clickable(role = Role.Button, onClickLabel = "Set options", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Numbers.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = tint)
    }
}
