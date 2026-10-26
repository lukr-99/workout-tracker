package com.lukr99.workout.ui.settings

/**
 * The places in Settings where a platform can add rows of its own. A platform that has none of
 * them (the web edition) answers false from [SettingsHost.has] and the screen leaves them out.
 */
enum class SettingsSlot {
    /** The wger exercise catalog row at the end of the Workouts section. */
    Catalog,

    /** The whole Health Connect section. */
    HealthConnect,

    /** The automatic backup rows at the top of Your data. */
    Backup,

    /** The whole Updates section. */
    Updates,
}
