package com.lukr99.workout.data

/**
 * Pure row math for the list on a share image. When the rows do not fit, the last slot becomes a
 * "+N more" line, so the list never runs past its space.
 */
object ShareListLayout {

    /** How many rows of [rowHeight] fit between [top] and [bottom]. Never negative. */
    fun capacity(top: Float, bottom: Float, rowHeight: Float): Int =
        if (rowHeight <= 0f || bottom <= top) 0 else ((bottom - top) / rowHeight).toInt()

    /** How many of [total] items to draw in [capacity] rows, leaving one row for "+N more". */
    fun visibleCount(total: Int, capacity: Int): Int = when {
        total <= capacity -> total
        capacity <= 1 -> 0
        else -> capacity - 1
    }

    /** The "+N more" count, or 0 when everything fits. */
    fun overflow(total: Int, capacity: Int): Int = total - visibleCount(total, capacity)
}
