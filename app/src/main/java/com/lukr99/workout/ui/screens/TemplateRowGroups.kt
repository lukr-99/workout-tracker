package com.lukr99.workout.ui.screens

/** Superset grouping for template rows: join a row to the one above, or take it out of its group. */
internal object TemplateRowGroups {

    /** Joins row [index] to the row above. It takes that row's group, or both start a new one. */
    fun joinPrevious(rows: List<TemplateRow>, index: Int): List<TemplateRow> {
        if (index <= 0 || index > rows.lastIndex) return rows
        val above = rows[index - 1]
        val group = above.supersetGroup ?: ((rows.mapNotNull { it.supersetGroup }.maxOrNull() ?: 0) + 1)
        return rows.mapIndexed { i, row ->
            when (i) {
                index - 1 -> row.copy(supersetGroup = group)
                index -> row.copy(supersetGroup = group)
                else -> row
            }
        }
    }

    /** Takes row [index] out of its superset. A group left with one row is dissolved. */
    fun leave(rows: List<TemplateRow>, index: Int): List<TemplateRow> {
        val group = rows.getOrNull(index)?.supersetGroup ?: return rows
        val left = rows.mapIndexed { i, row -> if (i == index) row.copy(supersetGroup = null) else row }
        return if (left.count { it.supersetGroup == group } < 2) left.map { if (it.supersetGroup == group) it.copy(supersetGroup = null) else it } else left
    }

    /** Whether row [index] is in the same superset as the row above it. */
    fun joinedToPrevious(rows: List<TemplateRow>, index: Int): Boolean =
        index > 0 && rows[index].supersetGroup != null && rows[index].supersetGroup == rows[index - 1].supersetGroup

    /** Dissolves superset [group]: every row in it goes back to being on its own. */
    fun ungroup(rows: List<TemplateRow>, group: Int): List<TemplateRow> =
        rows.map { if (it.supersetGroup == group) it.copy(supersetGroup = null) else it }

    /**
     * Supersets after a drag: a row stays in a group only next to another row of it, a group left
     * with one row dissolves, and a group split in two becomes two groups.
     */
    fun tidy(rows: List<TemplateRow>): List<TemplateRow> {
        val kept = rows.mapIndexed { i, row ->
            val group = row.supersetGroup ?: return@mapIndexed row
            val nextToSame = rows.getOrNull(i - 1)?.supersetGroup == group || rows.getOrNull(i + 1)?.supersetGroup == group
            if (nextToSame) row else row.copy(supersetGroup = null)
        }
        var next = (kept.mapNotNull { it.supersetGroup }.maxOrNull() ?: 0) + 1
        val seen = mutableSetOf<Int>()
        var renamed: Pair<Int, Int>? = null
        return kept.mapIndexed { i, row ->
            val group = row.supersetGroup ?: return@mapIndexed row
            val runStart = kept.getOrNull(i - 1)?.supersetGroup != group
            if (runStart) {
                renamed = if (group in seen) group to next++ else null
                seen += group
            }
            renamed?.takeIf { it.first == group }?.let { row.copy(supersetGroup = it.second) } ?: row
        }
    }
}

