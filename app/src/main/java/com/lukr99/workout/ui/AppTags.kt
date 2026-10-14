package com.lukr99.workout.ui

/**
 * Stable test tags for the app shell. The root turns on `testTagsAsResourceId`, so these names work
 * in Compose tests, `uiautomator dump`, Android CLI `layout` and Maestro `id:` selectors alike.
 */
object AppTags {
    const val ROOT = "app_root"
    const val NAV_START = "nav_start"
    const val START_LIFT = "start_lift"
    const val START_RUN = "start_run"
    const val DAY_SHEET = "day_sheet"
    const val EXERCISE_PICKER = "exercise_picker"
    const val EXERCISE_PICKER_CLOSE = "exercise_picker_close"

    /** A day tile in Home's week strip, by its date, like `home_day_2026-10-13`. */
    fun homeDay(date: java.time.LocalDate): String = "home_day_$date"

    /** A workout or run row in the day chooser sheet. */
    fun dayActivity(id: String): String = "day_activity_$id"

    /** A bottom-bar tab, like `nav_home` or `nav_settings`. */
    fun nav(tab: Tab): String = "nav_${tab.name.lowercase()}"
}
