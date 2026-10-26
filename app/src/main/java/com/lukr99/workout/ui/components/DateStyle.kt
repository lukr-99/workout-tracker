package com.lukr99.workout.ui.components

/** The ways the app writes a calendar day, as `java.time` patterns. */
enum class DateStyle(val pattern: String) {
    /** "Mon 13 Oct". */
    Day("EEE d MMM"),

    /** "13 Oct 2026". */
    DayYear("d MMM yyyy"),

    /** "13 Oct". */
    ShortDay("d MMM"),

    /** "Monday 13 Oct". */
    LongDay("EEEE d MMM"),

    /** "Monday 13 October", the Home header. */
    Header("EEEE d MMMM"),
}
