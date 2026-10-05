package com.lukr99.workout.ui.screens

import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry

/** The next set to do: the first set not done, in the first strength exercise that is not finished. */
internal fun nextSet(entries: List<WorkoutEntry>, doneIds: Set<String>): Pair<WorkoutEntry, StrengthSet>? =
    entries.asSequence()
        .filter { it.isStrength && it.completedAtUtc == null }
        .firstNotNullOfOrNull { entry -> entry.strengthSets.firstOrNull { it.id !in doneIds }?.let { entry to it } }

/** "set 4 of 4" while an exercise is under way, or the next exercise's name before it starts. */
internal fun nextSetLabel(entries: List<WorkoutEntry>, doneIds: Set<String>): String? {
    val (entry, set) = nextSet(entries, doneIds) ?: return null
    val started = entry.strengthSets.any { it.id in doneIds }
    return if (started) "set ${entry.strengthSets.indexOf(set) + 1} of ${entry.strengthSets.size}" else entry.exerciseSnapshotName
}
