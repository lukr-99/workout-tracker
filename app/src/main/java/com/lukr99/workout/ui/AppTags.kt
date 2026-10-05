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

    /** A bottom-bar tab, like `nav_home` or `nav_settings`. */
    fun nav(tab: Tab): String = "nav_${tab.name.lowercase()}"
}
