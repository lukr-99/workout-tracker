package com.lukr99.workout.data

/**
 * How the exercise list on a share image is laid out: whether rows are [expanded] with their sets,
 * how many rows are [shown], how many hide behind "+N more" ([overflow]), the height of each shown
 * row, the [cardHeight] that fits it all, and the [slack] a minimum-height card has to spare.
 */
data class ShareListPlan(
    val expanded: Boolean,
    val shown: Int,
    val overflow: Int,
    val rowHeights: List<Float>,
    val cardHeight: Int,
    val slack: Float,
)
