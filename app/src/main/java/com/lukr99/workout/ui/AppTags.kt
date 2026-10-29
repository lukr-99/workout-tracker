package com.lukr99.workout.ui

import com.lukr99.workout.ui.components.MuscleMapMode

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
    const val LOCATION_SHEET = "location_sheet"
    const val LOCATION_SETUP = "location_setup"
    const val MUSCLE_MAP = "muscle_map"
    const val MUSCLE_MAP_INFO = "muscle_map_info"
    const val MUSCLE_MAP_INFO_SHEET = "muscle_map_info_sheet"

    /** The Recovery or Weekly sets switch on the muscle map, like `muscle_map_mode_weeklysets`. */
    fun muscleMapMode(mode: MuscleMapMode): String = "muscle_map_mode_${mode.name.lowercase()}"

    /** A muscle row under the map, like `muscle_row_chest`. */
    fun muscleRow(bodyPart: String): String = "muscle_row_${bodyPart.lowercase().replace(' ', '_')}"

    /** A row in the location picker, by location id, like `location_home`. */
    fun location(id: String): String = "location_$id"

    /** A day tile in Home's week strip, by its date, like `home_day_2026-10-13`. */
    fun homeDay(date: kotlinx.datetime.LocalDate): String = "home_day_$date"

    /** A workout or run row in the day chooser sheet. */
    fun dayActivity(id: String): String = "day_activity_$id"

    /** A bottom-bar tab, like `nav_home` or `nav_settings`. */
    fun nav(tab: Tab): String = "nav_${tab.name.lowercase()}"
}
