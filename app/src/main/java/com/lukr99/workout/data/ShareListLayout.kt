package com.lukr99.workout.data

/**
 * Pure layout math for the exercise list on a share image. Rows show their sets when everything
 * fits that way, else they fall back to one compact line each. When even compact rows do not fit,
 * the last slot becomes a "+N more" line. The card grows with the list, from [minHeight] up to
 * [maxHeight], so a short workout makes a shorter card instead of one with an empty bottom half.
 * What a minimum-height card still has to spare is the plan's slack, for centring the content.
 */
object ShareListLayout {

    /**
     * Plans the list. [hasSets] says per row whether it has sets to show when expanded. Rows start
     * at [rowsTop] and the card keeps [bottomSpace] free below the last row.
     */
    fun plan(
        hasSets: List<Boolean>,
        rowsTop: Float,
        bottomSpace: Float,
        compactHeight: Float,
        expandedHeight: Float,
        minHeight: Int,
        maxHeight: Int,
    ): ShareListPlan {
        val space = maxHeight - bottomSpace - rowsTop
        val expandedHeights = hasSets.map { if (it) expandedHeight else compactHeight }
        val expanded = hasSets.any { it } && expandedHeights.sum() <= space
        val heights = if (expanded) expandedHeights else hasSets.map { compactHeight }
        val shown = visibleCount(heights, space, compactHeight)
        val overflow = hasSets.size - shown
        val listBottom = rowsTop + heights.take(shown).sum() + if (overflow > 0) compactHeight else 0f
        val needed = listBottom + bottomSpace
        val cardHeight = Math.ceil(needed.toDouble()).toInt().coerceIn(minHeight, maxHeight)
        val slack = (cardHeight - needed).coerceAtLeast(0f)
        return ShareListPlan(expanded, shown, overflow, heights.take(shown), cardHeight, slack)
    }

    /** How many rows of [heights] fit in [space], keeping one [moreHeight] row free when not all fit. */
    fun visibleCount(heights: List<Float>, space: Float, moreHeight: Float): Int {
        if (heights.sum() <= space) return heights.size
        var used = 0f
        var count = 0
        for (height in heights) {
            if (used + height + moreHeight > space) break
            used += height
            count++
        }
        return count
    }
}
