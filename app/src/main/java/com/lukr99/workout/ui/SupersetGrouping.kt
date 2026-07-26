package com.lukr99.workout.ui

import com.lukr99.workout.domain.WorkoutEntry

/**
 * Toggles the boundary between an entry and its predecessor. Joined boundaries merge adjacent
 * runs; removing a boundary splits a run and clears one-entry fragments.
 */
internal fun toggleSupersetBoundary(
    entries: List<WorkoutEntry>,
    entryId: String,
): List<WorkoutEntry> {
    val normalized = normalizeSupersetGroups(entries)
    val index = normalized.indexOfFirst { it.id == entryId }
    if (index <= 0) return normalized

    val previous = normalized[index - 1]
    val current = normalized[index]
    val previousGroup = previous.supersetGroup
    val currentGroup = current.supersetGroup
    val nextGroup = (normalized.mapNotNull(WorkoutEntry::supersetGroup).maxOrNull() ?: 0) + 1

    val toggled = if (previousGroup != null && previousGroup == currentGroup) {
        val start = (index - 1 downTo 0)
            .takeWhile { normalized[it].supersetGroup == currentGroup }
            .last()
        val end = (index until normalized.size)
            .takeWhile { normalized[it].supersetGroup == currentGroup }
            .last()
        val leftSize = index - start
        val rightSize = end - index + 1
        normalized.mapIndexed { position, entry ->
            when {
                position in start until index ->
                    entry.copy(supersetGroup = previousGroup.takeIf { leftSize >= 2 })
                position in index..end ->
                    entry.copy(supersetGroup = nextGroup.takeIf { rightSize >= 2 })
                else -> entry
            }
        }
    } else {
        val targetGroup = previousGroup ?: currentGroup ?: nextGroup
        normalized.mapIndexed { position, entry ->
            when {
                previousGroup != null && currentGroup != null && previousGroup != currentGroup &&
                    entry.supersetGroup == currentGroup -> entry.copy(supersetGroup = previousGroup)
                position == index - 1 || position == index -> entry.copy(supersetGroup = targetGroup)
                else -> entry
            }
        }
    }
    return normalizeSupersetGroups(toggled)
}

/** Keeps group ids contiguous and removes singleton fragments after a move or removal. */
internal fun normalizeSupersetGroups(entries: List<WorkoutEntry>): List<WorkoutEntry> {
    if (entries.isEmpty()) return entries
    val output = entries.toMutableList()
    val usedGroups = mutableSetOf<Int>()
    var nextGroup = (entries.mapNotNull(WorkoutEntry::supersetGroup).maxOrNull() ?: 0) + 1
    var index = 0
    while (index < output.size) {
        val group = output[index].supersetGroup
        if (group == null) {
            index++
            continue
        }
        var end = index + 1
        while (end < output.size && output[end].supersetGroup == group) end++
        val runSize = end - index
        val normalizedGroup = when {
            runSize < 2 -> null
            group !in usedGroups -> group
            else -> nextGroup++
        }
        for (position in index until end) {
            output[position] = output[position].copy(supersetGroup = normalizedGroup)
        }
        normalizedGroup?.let(usedGroups::add)
        index = end
    }
    return output
}
