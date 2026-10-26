package com.lukr99.workout.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lukr99.workout.web.platform.prefersReducedMotion

/**
 * Whether the system asks for no animations. On the web that is the `prefers-reduced-motion`
 * setting. Each platform supplies this file.
 */
@Composable
fun rememberSystemAnimationsOff(): Boolean = remember { prefersReducedMotion() }
