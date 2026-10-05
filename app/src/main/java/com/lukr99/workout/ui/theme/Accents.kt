package com.lukr99.workout.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Body-part hues for the dots on exercises and the recovery map. They mark which muscle group an
 * exercise trains; the name always sits next to the dot, so the colour is never the only signal.
 * Metric colours (e1RM violet, cardio teal) live in [EmberColors] because text uses them.
 */
object Accents {
    val Chest = Color(0xFFF472B6)
    val Back = Color(0xFF60A5FA)
    val Legs = Color(0xFF34D399)
    val Shoulders = Color(0xFFFBBF24)
    val Arms = Color(0xFFB79CFF)
    val Core = Color(0xFF3FD5C0)
    val Other = Color(0xFF9A9087)

    /** The hue for a body part, grouping the specific muscles under their main group. */
    fun bodyPart(name: String): Color = when (name.trim().lowercase()) {
        "chest", "pecs" -> Chest
        "back", "lats", "traps", "upper back", "lower back" -> Back
        "legs", "quads", "quadriceps", "hamstrings", "glutes", "calves", "adductors", "abductors" -> Legs
        "shoulders", "delts" -> Shoulders
        "arms", "biceps", "triceps", "forearms" -> Arms
        "core", "abs", "obliques" -> Core
        else -> Other
    }
}
