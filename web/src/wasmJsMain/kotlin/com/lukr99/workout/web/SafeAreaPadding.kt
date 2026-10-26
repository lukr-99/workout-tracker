package com.lukr99.workout.web

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.lukr99.workout.web.platform.onSafeAreaChange
import com.lukr99.workout.web.platform.safeAreaInset

/**
 * The iPhone's safe area (notch, home indicator, rounded corners) as padding. The canvas covers the
 * whole screen, and Compose on the web reports no system-bar insets, so the app is padded by what
 * boot.js measures. One CSS pixel is one dp on the web canvas.
 */
@Composable
internal fun rememberSafeAreaPadding(): PaddingValues {
    var padding by remember { mutableStateOf(measure()) }
    DisposableEffect(Unit) {
        onSafeAreaChange { padding = measure() }
        onDispose { }
    }
    return padding
}

private fun measure(): PaddingValues = PaddingValues(
    start = safeAreaInset("left").dp,
    top = safeAreaInset("top").dp,
    end = safeAreaInset("right").dp,
    bottom = safeAreaInset("bottom").dp,
)
