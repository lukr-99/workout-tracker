package com.lukr99.workout.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Ember's own Reduce motion setting, provided at the theme root. */
val LocalReduceMotion = compositionLocalOf { false }

/**
 * Reads Ember's Reduce motion setting and the OS "remove animations" accessibility preference so Run Mode surfaces can honour it. When
 * the system animator duration scale is 0 (Settings → Accessibility → Remove animations, or
 * Developer options → Animator duration scale off), motion should be dropped in favour of an instant
 * state change. One source of truth for every run screen's animation gate.
 *
 * @return true when animations should be suppressed.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val app = LocalReduceMotion.current
    val system = remember {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        scale == 0f
    }
    return app || system
}
