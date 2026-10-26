package com.lukr99.workout.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether the operating system asks for no animations (Android: the animator duration scale is 0).
 * Each platform supplies this file; the web edition's is under web/ and reads prefers-reduced-motion.
 */
@Composable
fun rememberSystemAnimationsOff(): Boolean {
    val context = LocalContext.current
    return remember {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        scale == 0f
    }
}
