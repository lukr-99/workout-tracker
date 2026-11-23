package com.lukr99.workout.ui.components

/**
 * The ways the app writes a calendar day, as `java.time` patterns. Czech writes a dot after the
 * day number, so it has its own pattern.
 */
enum class DateStyle(val pattern: String, val czechPattern: String) {
    /** "Mon 13 Oct". */
    Day("EEE d MMM", "EEE d. MMM"),

    /** "13 Oct 2026". */
    DayYear("d MMM yyyy", "d. MMM yyyy"),

    /** "13 Oct". */
    ShortDay("d MMM", "d. MMM"),

    /** "Monday 13 Oct". */
    LongDay("EEEE d MMM", "EEEE d. MMM"),

    /** "Monday 13 October", the Home header. */
    Header("EEEE d MMMM", "EEEE d. MMMM"),
    ;

    /** The pattern for [language]. */
    fun patternFor(language: com.lukr99.workout.ui.i18n.Language): String =
        if (language == com.lukr99.workout.ui.i18n.Language.Czech) czechPattern else pattern
}
