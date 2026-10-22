package com.lukr99.workout.ui.components

import androidx.compose.ui.graphics.Color

/** One named, coloured line, like an exercise at one gym. Points are oldest first. */
data class ChartLine(
    val name: String,
    val color: Color,
    val points: List<TimedValue>,
    /** Dashed lines tell series apart without colour. */
    val dashed: Boolean = false,
)
