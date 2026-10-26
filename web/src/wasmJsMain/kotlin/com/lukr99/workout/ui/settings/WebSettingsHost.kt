package com.lukr99.workout.ui.settings

import androidx.compose.runtime.Composable
import com.lukr99.workout.web.WEB_VERSION_NAME

/**
 * The web edition's part of Settings. It has no rows of its own: no catalog sync, Health Connect,
 * automatic backup or updater, so [has] is false for every slot and the screen leaves them out.
 */
internal class WebSettingsHost : SettingsHost {
    override val appVersion: String = WEB_VERSION_NAME

    override fun has(slot: SettingsSlot): Boolean = false

    @Composable
    override fun Rows(slot: SettingsSlot, onOpenPrivacy: () -> Unit) = Unit

    @Composable
    override fun Overlays() = Unit
}
