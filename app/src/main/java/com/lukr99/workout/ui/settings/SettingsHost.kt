package com.lukr99.workout.ui.settings

import androidx.compose.runtime.Composable

/**
 * What Settings asks of the platform: its extra rows, its dialogs and the version text. Android
 * implements it with Health Connect, backup and the updater; the web edition has no extra rows.
 */
interface SettingsHost {
    /** The version shown in the footer. */
    val appVersion: String

    /** Whether this platform has rows for [slot]. */
    fun has(slot: SettingsSlot): Boolean

    /** The platform's rows for [slot]. Only called when [has] is true. */
    @Composable
    fun Rows(slot: SettingsSlot, onOpenPrivacy: () -> Unit)

    /** Dialogs and one-time effects that belong to the whole screen, such as the update offer. */
    @Composable
    fun Overlays()
}
