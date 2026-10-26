package com.lukr99.workout.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.lukr99.workout.web.platform.BrowserBack

/**
 * Runs [onBack] when the person uses the browser's back button or the iPhone's back swipe, while
 * [enabled]. Several can be active at once; the one registered last runs first, as on Android.
 *
 * Each platform supplies this file.
 */
@Composable
fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val latest by rememberUpdatedState(onBack)
    val registered = remember { arrayOfNulls<BrowserBack.Handler>(1) }
    DisposableEffect(Unit) {
        val handler = BrowserBack.shared.register(enabled) { latest() }
        registered[0] = handler
        onDispose {
            handler.remove()
            registered[0] = null
        }
    }
    SideEffect { registered[0]?.enabled = enabled }
}
