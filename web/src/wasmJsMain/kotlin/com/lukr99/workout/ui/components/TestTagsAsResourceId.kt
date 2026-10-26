package com.lukr99.workout.ui.components

import androidx.compose.ui.Modifier

/**
 * Makes the test tags below this node visible to UI automation as resource ids. That is an Android
 * idea; the web edition's version does nothing. Each platform supplies this file.
 */
fun Modifier.exposeTestTags(): Modifier = this
